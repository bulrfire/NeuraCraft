package com.bulefire.neuracraft.core.inside.model.deepseek;

import com.bulefire.neuracraft.compatibility.entity.APlayer;
import com.bulefire.neuracraft.compatibility.entity.Content;
import com.bulefire.neuracraft.compatibility.util.CUtil;
import com.bulefire.neuracraft.compatibility.util.FileUtil;
import com.bulefire.neuracraft.core.agent.AbsAgent;
import com.bulefire.neuracraft.core.agent.AgentController;
import com.bulefire.neuracraft.core.agent.annotation.RegisterAgent;
import com.google.gson.Gson;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;

public class DeepSeek extends AbsAgent {
    private static final Logger log = getLogger(DeepSeek.class);

    private final ChatHistory chatHistory;

    public DeepSeek(String name, UUID uuid, Set<APlayer> players, Set<APlayer> admins, String modelName, String disPlayName, int timePerMin) {
        super(name, uuid, players, admins, modelName, disPlayName, "deepseek", timePerMin);
        chatHistory = new ChatHistory();
        chatHistory.addBlock(
                new ChatHistory.ChatBlock(
                        "system",
                        List.of(
                                new Content("text", DeepSeekConfig.getPrompt()),
                                new Content("text", AgentController.fullRecommendedPrompt)
                        )
                )
        );
    }

    private DeepSeek(DeepSeekSerializationData data) {
        super(data);
        chatHistory = data.chatHistory;
    }

    public DeepSeek() {
        super("DeepSeek1", UUID.randomUUID(), new HashSet<>(), new HashSet<>(), DeepSeekConfig.getModelName(), DeepSeekConfig.getDisplayName(), "deepseek", DeepSeekConfig.getTimePerMin());
        chatHistory = new ChatHistory();
    }

    @Contract(" -> new")
    private static @NotNull DeepSeek newInstance() {
        return new DeepSeek(
                "DeepSeek" + (new Random()).nextInt(),
                UUID.randomUUID(),
                new HashSet<>(),
                new HashSet<>(),
                DeepSeekConfig.getModelName(),
                DeepSeekConfig.getDisplayName(),
                DeepSeekConfig.getTimePerMin()
        );
    }

    @RegisterAgent
    public static void init() {
        log.info("DeepSeek static init");
        DeepSeekConfig.init();
        AgentController.getInstance().registerAgentClassInitFunction(
                () -> {
                    var agentManager = AgentController.getInstance().getAgentManager();
                    agentManager.registerAgentMapping("DeepSeek", DeepSeek::newInstance);

                    agentManager.registerAgentPathConsumer(
                            path -> {
                                if (path.toString().endsWith(".deepseek")) {
                                    return "DeepSeek";
                                }
                                return null;
                            }
                    );
                }
        );
    }

    @Override
    protected @NotNull String message(@NotNull List<Content> msg) {
        try {
            CUtil.Response response = CUtil.AiPOST(
                    DeepSeekConfig.getUrl(),
                    buildBody(msg),
                    DeepSeekConfig.getToken()
            );
            if (response.status() != 200) {
                return "API error with %s %s".formatted(response.status(), response.responseMessage());
            }
            return decoder(response.response());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildBody(@NotNull List<Content> message) {
        log.info("start build body");
        Gson g = new Gson();
        chatHistory.addBlock(new ChatHistory.ChatBlock("user",message));
        return g.toJson(new SendBody(this.getModelName(), chatHistory.histories));
    }

    record SendBody(String model, List<ChatHistory.ChatBlock> messages) {
    }

    public String decoder(@NotNull String repose) {
        Gson g = new Gson();
        OPAResult result = g.fromJson(repose, OPAResult.class);
        OPAResult.ChoicesBean.Message m = result.getChoices().get(0).getMessage();
        chatHistory.addBlock(new ChatHistory.ChatBlock(m.getRole(), List.of(new Content("text", m.getContent()))));
        return m.getContent();
    }

    @Override
    @SneakyThrows
    public void saveToFile(@NotNull Path path) {
        log.debug("deepseek save to file: {}", path);
        FileUtil.saveJsonToFile(this, path);
    }

    @Override
    public void loadFromFile(@NotNull Path path) {
        // 我简直是甜菜
        loadFileToManager(path, DeepSeekSerializationData.class, DeepSeek.class);
    }

    @Override
    public void reloadConfig() {
        DeepSeekConfig.init();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class DeepSeekSerializationData extends AbsAgent.AgentSerializationData {
        public ChatHistory chatHistory;

        public DeepSeekSerializationData(@NotNull DeepSeek agent) {
            super(agent);
            this.chatHistory = agent.chatHistory;
        }

        public DeepSeekSerializationData() {
            super();
            this.chatHistory = new ChatHistory();
        }
    }
}
