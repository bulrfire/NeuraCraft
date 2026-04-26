package com.bulefire.neuracraft.core.agent;

import com.bulefire.neuracraft.NeuraCraft;
import com.bulefire.neuracraft.compatibility.command.CommandRegister;
import com.bulefire.neuracraft.compatibility.entity.Content;
import com.bulefire.neuracraft.compatibility.entity.SendMessage;
import com.bulefire.neuracraft.compatibility.function.process.*;
import com.bulefire.neuracraft.compatibility.util.CUtil;
import com.bulefire.neuracraft.compatibility.util.FileUtil;
import com.bulefire.neuracraft.compatibility.util.scanner.AnnotationsMethodScanner;
import com.bulefire.neuracraft.core.agent.annotation.RegisterAgent;
import com.bulefire.neuracraft.core.agent.commnd.NCCommand;
import com.bulefire.neuracraft.core.agent.entity.AgentMessage;
import com.bulefire.neuracraft.core.agent.entity.AgentResponse;
import com.bulefire.neuracraft.core.command.GameCommand;
import com.bulefire.neuracraft.core.config.NCMainConfig;
import com.bulefire.neuracraft.core.mcp.MCPController;
import com.bulefire.neuracraft.core.util.AgentOutOfTime;
import com.bulefire.neuracraft.core.util.UnSupportFormattedMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * AgentController
 * 用于管理和调度所有的 {@link Agent} 实例, 与玩家进行交互, 监听事件并处理.
 *
 * @author bulefire_fox
 * @version 2.0
 * @see Agent
 * @see AgentManager
 * @see PlayerManager
 * @see GameCommand
 * @see ChatEventProcesser.ChatMessage
 * @see AgentController#registerAgentClassInitFunction(Runnable)
 * @since 2.0
 */
@Log4j2
public class AgentController {
    private static final Pattern JOIN_MSG_P = Pattern.compile("^[a-zA-Z0-9_]+\\\\s+join\\\\s+the\\\\s+game\\\\.\\\\.$");
    private static final Pattern LEAVE_MSG_P = Pattern.compile("^[a-zA-Z0-9_]+\\\\s+left\\\\s+the\\\\s+game\\\\.\\\\.$");
    
    public static final Function<String, Boolean> JOIN_MSG_FORMATE = (s) -> JOIN_MSG_P.matcher(s).matches();
    public static final Function<String, Boolean> LEAVE_MSG_FORMATE = (s) -> LEAVE_MSG_P.matcher(s).matches();
    
    public static final Consumer<Component> gameLogger = (c) -> CUtil.broadcastMessageToCharBar(new SendMessage(c, CUtil.getCurrentEnv(), CUtil.broadcast));
    // region prompt
    public static String AgentIdentityPrompt = """
            你的身份与核心原则
            1.  你可以主动使用我提供的工具来获取实时数据、执行操作或处理信息。
            2.  当用户问题直接涉及或隐含需要以下工具所提供的能力时，你必须主动调用工具。调用前无需额外征求我的许可。
            3.  你需要对工具调用的时机、参数的正确性和最终回答的完整性负责。
            """;
    public static String AliveMCPPromptHead = """
            你可调用的工具
            以下是你当前可用的全部工具列表。若列表为空则说明目前没有可用的工具,不要随意调用。请仔细理解每个工具的描述和参数要求：
            """;
    public static String AliveMCPPrompt;
    public static String MCPFormatPrompt = """
            工具描述的标准格式如下：
            工具名称：
            - 唯一调用ID：[服务器名].[工具名]
            - 功能描述：[此工具具体能做什么]
            - 必需参数：[参数1名称] (数据类型，说明)，[参数2名称] (数据类型，说明)...
            - 可选参数：(如有) [参数3名称] (数据类型，默认值，说明)
            """;
    public static String AgentMCPOutputPrompt = """
            你必须遵守的通信协议
            这是你与我（客户端系统）交互的唯一且强制的规则。
            1.  决策与调用：
                分析用户请求后，若决定使用工具，你必须且只能输出一个严格符合下方格式的JSON对象。输出中不得包含任何其他文字、解释、标记或代码框。
            2.  强制输出格式：
                {"tool_call": {"id": "此处填写与工具列表中完全一致的唯一调用ID", "parameters": {"参数1名称": "参数1值", "参数2名称": "参数2值"}}}
            """;
    public static String AgentMCPWorkflowPrompt = """
            我们的标准化工作流程
            1.  回合一（你判断并输出）：你收到用户问题。若需工具，则严格按上述第三条规则输出JSON；若无需工具，则直接回复。
            2.  回合二（我执行）：我（客户端系统）将拦截你输出的JSON，实际操作对应工具，并将原始执行结果以固定格式反馈给你。
            3.  回合三（你整合答复）：你将收到我的一条系统消息，格式固定为：“[工具调用结果]：<具体的返回数据或错误信息>”。你的任务是：
                a. 解读结果：用清晰、友好的语言向我解释工具返回的结果。
                b. 整合回答：将结果融入你的最终答复，直接、完整地回应用户最初的问题。
                c. 处理错误：如果结果包含错误信息，请根据该信息向我说明可能的原因，或建议调整调用方式。
            """;
    public static String reiteratedKeyPrompt = """
            关键指令重申
                - 不要在你输出的JSON对象前后添加任何文字说明。
                - 不要修改或编造工具定义的参数名称和结构。
                - 务必确保“id”字段的值与工具列表中提供的“唯一调用ID”完全一致。
                - 如果用户请求模糊，请依据工具描述推断最合理的参数值进行调用。
            """;
    public static String fullRecommendedPrompt;
    // endregion
    @Getter
    private final AgentManager agentManager;
    @Getter
    private final PlayerManager playerManager;
    @Getter
    private final GameCommand GAME_COMMAND;
    @Getter
    private final MCPController mcpController;
    
    private final List<Runnable> agentClassInitFunctions;
    
    @Setter
    private String prefix;
    
    private final ExecutorService executor;
    
    public static final AgentController INSTANCE = new AgentController();
    private AgentController() {
        agentManager = new AgentManager();
        playerManager = new PlayerManager();
        GAME_COMMAND = GameCommand.getINSTANCE();
        mcpController = MCPController.getInstance();
        agentClassInitFunctions = Collections.synchronizedList(new ArrayList<>());
        prefix = NCMainConfig.getPrefix();
        executor = Executors.newFixedThreadPool(Math.max(1, NCMainConfig.getThreadsNumber()));
    }
    public static AgentController getInstance() {
        return INSTANCE;
    }
    
    /**
     * 注册一个 Agent 类初始化逻辑
     *
     * @param fun 初始化逻辑, 包装为一个 {@link Runnable}
     * @see Runnable
     * @see AgentController#agentClassInitFunctions
     */
    public synchronized void registerAgentClassInitFunction(Runnable fun) {
        log.debug("register agent class init function {}", fun);
        agentClassInitFunctions.add(fun);
    }
    
    public static void init() {
        // 加载配置文件
        
        log.debug("register to chatEvent");
        // 监听聊天事件
        ChatEventProcesser.registerFun(getInstance()::onMessage);
        // 监听玩家加入事件
        PlayerJoinEventProcesser.registerFun(
                (msg) -> {
                    // 将玩家加入管，注意到manager不会覆盖原有值,因此与从配置文件添加的玩家不冲突
                    getInstance().playerManager.addPlayer(msg.player(), PlayerManager.emptyUUID);
                    String message = NCMainConfig.getPrefix() + msg.player().toFormatedString() + "join the game..";
                    // 稍加处理即可
                    getInstance().onMessage(
                            new ChatEventProcesser.ChatMessage(
                                    message,
                                    msg.player(),
                                    msg.env()
                            )
                    );
                }
        );
        // 监听玩家退出事件
        PlayerExitEventProcesser.registerFun(
                (msg) -> {
                    // 稍加处理即可
                    String message = NCMainConfig.getPrefix() + msg.player().toFormatedString() + "exit the game..";
                    getInstance().onMessage(
                            new ChatEventProcesser.ChatMessage(
                                    message,
                                    msg.player(),
                                    msg.env()
                            )
                    );
                    getInstance().playerManager.removePlayer(msg.player());
                }
        );
        // 监听服务器关闭事件
        // 保存所有Agent
        ServerStoppingEventProcesser.registerFun(
                getInstance()::saveAllAgentToFile
        );
        // 监听世界加载事件
        // 先加载agent
        // 加载所有Agent
        LevelLoadEventProcess.registerFun(
                getInstance()::loadAllAgentFromFile
        );
        // 加载玩家到 player manager
        LevelLoadEventProcess.registerFun(
                () -> {
                    // 加载玩家到 player manager
                    getInstance().playerManager.loadPlayerFromAgentManager(getInstance().agentManager);
                }
        );
        // 监听世界卸载事件
        // 保存所有Agent
        // 卸载所有Agent
        LevelUnloadEventProcess.registerFun(
                getInstance()::saveAllAgentToFile
        );
        
        // 注册我们自己的指令
        //CommandRegister.registerCommand(NCCommand.getCommands());
        NCCommand.buildCommands();
        
        // 扫描插件的注册
        // 放在InitCore里了
        
        // 扫描MCP的注册
        // 放在InitCore里了
        
        // 构建MCP的提示词供Agent使用
        StringBuilder sb = new StringBuilder();
        for (var mcps : getInstance().mcpController.getMcpManager().getTools()) {
            for (var mcp : mcps) {
                sb.append(mcp.getPrompt()).append("\n");
            }
        }
        AliveMCPPrompt = sb.toString();
        
        fullRecommendedPrompt =
                AgentIdentityPrompt +
                        AliveMCPPromptHead +
                        AliveMCPPrompt +
                        MCPFormatPrompt +
                        AgentMCPOutputPrompt +
                        AgentMCPWorkflowPrompt +
                        reiteratedKeyPrompt;
        //log.info("fullRecommendedPrompt {}", fullRecommendedPrompt);
        
        // 扫描我们自己的Agent类
        var methods = AnnotationsMethodScanner.scanPackageToMethod("com.bulefire.neuracraft.core", Set.of(RegisterAgent.class));
        log.info("MMM found methods {}", methods);
        
        for (Method method : methods) {
            try {
                method.invoke(null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("Failed to invoke method , report to the mod develop, not NC!", e);
                throw new RuntimeException(e);
            }
        }
        
        // 执行所有Agent类的初始化逻辑
        List<Runnable> copy = new ArrayList<>(getInstance().agentClassInitFunctions);
        log.debug("all functions {}", copy);
        for (Runnable fun : copy) {
            log.debug("Running agent class init function {}", fun);
            fun.run();
        }
        // 加载所有的指令
        //CommandRegister.registerCommands(agentGameCommand.getAllCommands());
        CommandRegister.registerCommands(getInstance().GAME_COMMAND.getAllCommands());
    }
    
    public static void afterInit() {
        // 加载所有Agent,必须在Agent类加载完后加载,否则无法注入Function
        getInstance().loadAllAgentFromFile();
        // 加载玩家
        getInstance().playerManager.loadPlayerFromAgentManager(getInstance().agentManager);
    }
    
    public void onMessage(@NotNull ChatEventProcesser.ChatMessage chatMessage) {
        executor.submit(() -> AsyncMessage(chatMessage));
    }
    
    // 消息入口
    private void AsyncMessage(@NotNull ChatEventProcesser.ChatMessage chatMessage) {
        if (! chatMessage.msg().startsWith(prefix))
            return;
        String msg = chatMessage.msg().substring(prefix.length());
        if (Objects.requireNonNull(chatMessage.env()) == ChatEventProcesser.ChatMessage.Env.CLIENT) {
            if (CUtil.hasMod.apply(NeuraCraft.MOD_ID)) {
                // 服务端有模组,交给服务端处理
                return;
            }
        }
        // 单人和服务器唯一的区别是发送消息至聊天栏的方式,但这是兼容层的事情,在这里我们把他当作同一个东西
        // 当服务器没有模组时当作单人处理
        // 保留 SINGLE 的唯一目的是在调用兼容层方法时作为环境传入,以便兼容层做出合理的处理
        
        // uuid 最为 agent 的唯一标识符
        UUID uuid = playerManager.getPlayerAgentUUID(chatMessage.player());
        if (uuid == null) {
            // 玩家不在聊天室则 uuid 为 null
            // 提示即可
            CUtil.sendMessageToPlayer(
                    new SendMessage(
                            Component.translatable("neuracraft.agent.command.find.notInChatRoom"),
                            chatMessage.env(),
                            chatMessage.player()
                    )
            );
            return;
        }
        // 获取聊天室
        Agent agent = agentManager.getAgent(uuid);
        
        // 上锁!
        agent.lockMessageLock();
        // 防止多重调用的幻觉
        if (agent.isMCPCalling()) {
            CUtil.broadcastMessageToGroupPlayer(
                    new SendMessage(
                            Component.translatable("neuracraft.agent.chat.error.mcpcalling"),
                            chatMessage.env(),
                            chatMessage.player()
                    ),
                    agent.getPlayers().stream().toList()
            );
            return;
        }
        
        AgentResponse remessage;
        // 这里异常是最好的办法(?),尽管这样会让代码变得臃肿一些
        // 判断 null 的话就太不优雅了
        // 虽然现在也不是很优雅 :D
        try {
            // 邪恶的作用域
            remessage = agent.sendMessage(new AgentMessage(List.of(new Content("text", msg)), chatMessage.player()));
        } catch (AgentOutOfTime e) {
            agent.releaseMessageLock();
            // 提示即可
            CUtil.broadcastMessageToGroupPlayer(
                    new SendMessage(
                            Component.translatable("neuracraft.agent.chat.error.tooFast"),
                            chatMessage.env(),
                            chatMessage.player()
                    ),
                    agent.getPlayers().stream().toList()
            );
            return;
        } catch (UnSupportFormattedMessage e) {
            agent.releaseMessageLock();
            CUtil.broadcastMessageToGroupPlayer(
                    new SendMessage(
                            Component.translatable("neuracraft.agent.chat.warn.unsupport.message.type", agent.getName(), e.getMessageType().name()),
                            chatMessage.env(),
                            chatMessage.player()
                    ),
                    agent.getPlayers().stream().toList()
            );
            return;
        }
        
        // 判断并进行MCP调用
        if (remessage.state() == AgentResponse.State.START_MCP_CALL || remessage.state() == AgentResponse.State.MCP_CALLING) {
            // 将控制权流转至MCP调用方法
            remessage = mcpCall(
                    agent, remessage,
                    (input) -> CUtil.broadcastMessageToGroupPlayer(
                            new SendMessage(
                                    Component.translatable(
                                            "neuracraft.mcp.message.format.system",
                                            Component.literal(agent.getDisPlayName()).withStyle(
                                                    style -> style.withColor(TextColor.parseColor("#7CFC00"))
                                            ),
                                            input
                                    ),
                                    chatMessage.env(),
                                    chatMessage.player()
                            ),
                            agent.getPlayers().stream().toList()
                    )
            );
        }
        
        // 在成功之后保存聊天室,防止崩溃导致的数据丢失
        agent.saveToFile(
                // path/to/world/agent/<modelName>/<uuid>.<suffix>
                getAgentPath(agent)
        );
        
        try {
            agent.releaseAllMessageLock();
        } catch (IllegalMonitorStateException  e) {
            log.info("lock already released for agent {}",agent.getUUID());
        }
        
        // 返回格式化消息
        // 这里使用 Component 是因为 Component 为minecraft内置接口,与具体loader无关
        CUtil.broadcastMessageToGroupPlayer(
                new SendMessage(
                        Component.translatable("neuracraft.agent.chat.message.format.player", agent.getDisPlayName(), remessage.msg()),
                        chatMessage.env(),
                        chatMessage.player()
                ),
                agent.getPlayers().stream().toList()
        );
    }
    
    // 这应该是一个一异步方法....
    // 调用这个方法的人应该确保自己获取了对应agent的锁....
    private @NotNull AgentResponse mcpCall(@NotNull Agent agent, @NotNull AgentResponse startResponse, Consumer<Component> print) {
        log.debug("AgentController MCP call start");
        AgentResponse agentResponse;
        do {
            List<Content> response = mcpController.processAgentInput(startResponse.msg(), print);
            log.debug("response from MCPController is: {}", response);
            agentResponse = agent.sendMessage(new AgentMessage(response, startResponse.player()));
            log.debug("response from agent is: {}", agentResponse);
        } while (agentResponse.state() == AgentResponse.State.MCP_CALLING || agentResponse.state() == AgentResponse.State.START_MCP_CALL);
        log.debug("AgentController MCP call end and return {}", agentResponse);
        return agentResponse;
    }
    
    public static @NotNull Path getAgentPath(@NotNull Agent agent) {
        Path path = FileUtil.getAgentBaseUrl().resolve(agent.getModelName()).resolve(agent.getUUID() + "." + agent.getSuffix());
        log.debug("getAgentPath for {} is {}", agent, path);
        return path;
    }
    
    private void loadAllAgentFromFile() {
        List<Path> paths;
        try {
            // 获取所有文件
            paths = FileUtil.readAllFilePath(FileUtil.getAgentBaseUrl())
                            .stream()
                            .filter(path -> ! path.startsWith(FileUtil.agent_config_url))
                            .toList();
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Path path : paths) {
            if (path.toFile().isFile()) {
                log.debug("load agent from {}", path);
                // 让Agent自己判断文件是否是自己的
                String agentName = agentManager.getAgentByConfigFilePath(path);
                if (agentName == null) {
                    // 找不到模型
                    log.warn("can not find agentName model for {}", path);
                    continue;
                }
                log.debug("load agent name {}", agentName);
                // 创建聊天室并加载
                agentManager.createAgent(agentName).loadFromFile(path);
            }
        }
    }
    
    private void saveAllAgentToFile() {
        for (Agent agent : agentManager.getAllAgents()) {
            agent.saveToFile(
                    // path/to/world/agent/<modelName>/<uuid>.<suffix>
                    getAgentPath(agent)
            );
        }
    }
}
