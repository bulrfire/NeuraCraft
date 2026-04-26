package com.bulefire.neuracraft.core.inside.model.yinying;

import com.bulefire.neuracraft.compatibility.command.FullCommand;
import com.bulefire.neuracraft.compatibility.entity.APlayer;
import com.bulefire.neuracraft.compatibility.entity.Content;
import com.bulefire.neuracraft.compatibility.entity.SendMessage;
import com.bulefire.neuracraft.compatibility.util.CUtil;
import com.bulefire.neuracraft.compatibility.util.FileUtil;
import com.bulefire.neuracraft.core.agent.AbsAgent;
import com.bulefire.neuracraft.core.agent.Agent;
import com.bulefire.neuracraft.core.agent.AgentController;
import com.bulefire.neuracraft.core.agent.annotation.RegisterAgent;
import com.bulefire.neuracraft.core.util.UnSupportFormattedMessage;
import com.google.gson.Gson;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;

public class YinYing extends AbsAgent {
    private static final Logger log = getLogger(YinYing.class);

    private final String chatId;
    private final String appId;
    final Variables variables;
    private final String systemPrompt;

    private boolean single = false;

    @Data
    static class Variables {
        private String nickName;
        private String furryCharacter;
        private String promptPatch;
        private String singlePromptPatch;

        public Variables(String nickName, String furryCharacter, String promptPatch, String singlePromptPatch) {
            this.nickName = nickName;
            this.furryCharacter = furryCharacter;
            this.promptPatch = promptPatch;
            this.singlePromptPatch = singlePromptPatch;
        }
    }

    YinYing(String name, UUID uuid, Set<APlayer> players, @NotNull Set<APlayer> admins, String modelName, String disPlayName, int timePerMin,
            String chatId, String appId, Variables variables, String systemPrompt) {
        super(name, uuid, players, admins, modelName, disPlayName, "yinying", timePerMin);
        this.chatId = chatId;
        this.appId = appId;
        this.variables = variables;
        this.systemPrompt = systemPrompt;
    }

    YinYing(YinYingSerializationData data) {
        super(data);
        chatId = data.chatId;
        appId = data.appId;
        variables = data.variables;
        systemPrompt = data.systemPrompt;
    }

    @Contract(" -> new")
    static @NotNull YinYing newInstance() {
        var uuid = UUID.randomUUID();
        int ri = new Random().nextInt();
        return new YinYing(
                "YinYing" + ri,
                uuid,
                new HashSet<>(),
                new HashSet<>(),
                YinYingConfig.getModelName(),
                YinYingConfig.getDisplayName(),
                YinYingConfig.getTimePerMin(),
                YinYingConfig.getAppId() + '-' + uuid.toString().replace("-", "") + '-' + ri,
                YinYingConfig.getAppId(),
                YinYingConfig.getVariables(),
                YinYingConfig.getSystemPrompt()
        );
    }

    @RegisterAgent
    public static void init() {
        log.info("YinYing init");
        YinYingConfig.init();
        AgentController.getInstance().registerAgentClassInitFunction(
                () -> {
                    var agentManager = AgentController.getInstance().getAgentManager();
                    agentManager.registerAgentMapping("YinYing", YinYing::newInstance);
                    agentManager.registerAgentPathConsumer(
                            path -> {
                                if (path.toString().endsWith(".yinying")) {
                                    return "YinYing";
                                }
                                return null;
                            }
                    );
                }
        );
        // 注册命令!
        // region yinying subcommand
        var commands = AgentController.getInstance().getGAME_COMMAND();
        //commands.registerCommand(
        commands.getPluginBaseCommand()
                .then(Commands.literal("yinying")
                              .then(Commands.literal("nickname")
                                            .then(Commands.argument("nickname", StringArgumentType.string())
                                                          .executes(new FullCommand.AbsCommand() {
                                                              @Override
                                                              public int run(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
                                                                  String nickname = StringArgumentType.getString(commandContext, "nickname");
                                                                  var agent = getAgent(commandContext);
                                                                  if (agent instanceof YinYing yinYing) {
                                                                      if (yinYing.getPlayers().size() != 1 || yinYing.getAdmins().size() != 1)
                                                                          feedback(commandContext.getSource(), Component.literal("you are not alone in this agent!, can't set nickname!"));
                                                                      yinYing.variables.setNickName(nickname);
                                                                      yinYing.single = true;
                                                                      feedback(
                                                                              commandContext.getSource(), Component.literal(
                                                                                      "successful set nickname to " +
                                                                                              nickname +
                                                                                              "send a message to save")
                                                                      );
                                                                  } else {
                                                                      feedback(commandContext.getSource(), Component.literal("you are not in a yinying agent!"));
                                                                  }
                                                                  return 1;
                                                              }
                                                          })
                                            )
                              )
                              .then(Commands.literal("furryCharacter")
                                            .then(Commands.argument("furryCharacter", StringArgumentType.string())
                                                          .executes(new FullCommand.AbsCommand() {
                                                              @Override
                                                              public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
                                                                  String furryCharacter = StringArgumentType.getString(context, "furryCharacter");
                                                                  var agent = getAgent(context);
                                                                  if (agent instanceof YinYing yinYing) {
                                                                      if (yinYing.getPlayers().size() != 1 || yinYing.getAdmins().size() != 1)
                                                                          feedback(context.getSource(), Component.literal("you are not alone in this agent!, can't set furryCharacter!"));
                                                                      yinYing.variables.setFurryCharacter(furryCharacter);
                                                                      yinYing.single = true;
                                                                      feedback(
                                                                              context.getSource(), Component.literal(
                                                                                      "successful set furryCharacter to \n" +
                                                                                              furryCharacter +
                                                                                              "\n send a message to save"
                                                                              )
                                                                      );
                                                                  } else {
                                                                      feedback(context.getSource(), Component.literal("you are not in a yinying agent!"));
                                                                  }
                                                                  return 1;
                                                              }
                                                          })
                                            )
                              )
                );
        //);
        // endregion
    }

    private static Agent getAgent(@NotNull CommandContext<CommandSourceStack> commandContext) {
        String playerName = Objects.requireNonNull(commandContext.getSource().getPlayer()).getName().getString();
        UUID playerUUID = commandContext.getSource().getPlayer().getUUID();
        APlayer player = new APlayer(playerName, playerUUID);
        var agentManger = AgentController.getInstance().getAgentManager();
        var playerManager = AgentController.getInstance().getPlayerManager();
        var agentUUID = playerManager.getPlayerAgentUUID(player);
        return agentManger.getAgent(agentUUID);
    }

    @Override
    protected @NotNull String message(@NotNull List<Content> msg) throws UnSupportFormattedMessage {
        if (AgentController.JOIN_MSG_FORMATE.apply(msg.get(0).textOrThrow()))
            throw new UnSupportFormattedMessage("this agent is single", UnSupportFormattedMessage.Type.JOIN);
        if (AgentController.LEAVE_MSG_FORMATE.apply(msg.get(0).textOrThrow()))
            throw new UnSupportFormattedMessage("this agent is single", UnSupportFormattedMessage.Type.LEAVE);
        var body = new SendBody(
                appId,
                chatId,
                new SendBody.Variables(
                        variables.getNickName(),
                        variables.getFurryCharacter(),
                        this.single ? variables.getSinglePromptPatch() : variables.getPromptPatch()
                ),
                getModelName(),
                systemPrompt,
                msg.get(0).textOrThrow()
        );
        Gson g = new Gson();
        String response;
        try {
            CUtil.Response rawresponse = CUtil.AiPOST(
                    YinYingConfig.getUrl(),
                    g.toJson(body),
                    YinYingConfig.getToken()
            );
            if (rawresponse.status() != 200) {
                return "API error with %s %s".formatted(rawresponse.status(), rawresponse.responseMessage());
            }
            response = rawresponse.response();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ResultBody result = g.fromJson(response, ResultBody.class);
        return result.getChoices().get(0).getMessage().getContent();
    }

    @Override
    public void addPlayer(@NotNull APlayer player) {
        if (! single)
            super.addPlayer(player);
        else {
            CUtil.broadcastMessageToCharBar(
                    new SendMessage(
                            Component.literal("you are in a single yinying agent, can't invite any one else"),
                            CUtil.getEnv(CUtil.getServer.get()),
                            player
                    )
            );
        }
    }

    @Override
    public void addAdmin(@NotNull APlayer player) {
        if (! single)
            super.addAdmin(player);
        else {
            CUtil.broadcastMessageToCharBar(
                    new SendMessage(
                            Component.literal("you are in a single yinying agent, can't invite any admin else"),
                            CUtil.getEnv(CUtil.getServer.get()),
                            player
                    )
            );
        }
    }

    @Override
    @SneakyThrows
    public void saveToFile(@NotNull Path path) {
        FileUtil.saveJsonToFile(this, path);
    }

    @Override
    public void loadFromFile(@NotNull Path path) {
        loadFileToManager(path, YinYingSerializationData.class, YinYing.class);
    }

    @Override
    public void reloadConfig() {
        YinYingConfig.init();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    static class YinYingSerializationData extends AbsAgent.AgentSerializationData {
        private String chatId;
        private String appId;
        private Variables variables;
        private String systemPrompt;

        public YinYingSerializationData(@NotNull YinYing agent) {
            super(agent);
            this.chatId = agent.chatId;
            this.appId = agent.appId;
            this.variables = agent.variables;
            this.systemPrompt = agent.systemPrompt;
        }

        public YinYingSerializationData() {
            super();
            this.chatId = "";
            this.appId = "";
            this.variables = new Variables("", "", "", "");
            this.systemPrompt = "";
        }
    }
}
