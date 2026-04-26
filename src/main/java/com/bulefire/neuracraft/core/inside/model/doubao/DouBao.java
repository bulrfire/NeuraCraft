package com.bulefire.neuracraft.core.inside.model.doubao;

import com.bulefire.neuracraft.compatibility.entity.APlayer;
import com.bulefire.neuracraft.compatibility.entity.Content;
import com.bulefire.neuracraft.compatibility.util.FileUtil;
import com.bulefire.neuracraft.core.agent.AbsAgent;
import com.bulefire.neuracraft.core.agent.AgentController;
import com.bulefire.neuracraft.core.agent.annotation.RegisterAgent;
import com.bulefire.neuracraft.core.inside.model.deepseek.DeepSeek;
import com.bulefire.neuracraft.core.util.UnSupportFormattedMessage;
import com.volcengine.ark.runtime.model.responses.request.CreateResponsesRequest;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;

public class DouBao extends AbsAgent {
    private static final Logger log = getLogger(DouBao.class);
    // 这个 id 怎么还不能手动指定 (
    // 害得我不能用 final 了
    private String conversationId;
    
    private final transient ArkService service;
    
    public DouBao(String name, UUID uuid, Set<APlayer> players, Set<APlayer> admins, String modelName, String disPlayName, int timePerMin) {
        super(name, uuid, players, admins, modelName, disPlayName, "doubao", timePerMin);
        conversationId = "";
        service = ArkService
                .builder()
                .apiKey(DouBaoConfig.getToken())
                .baseUrl(DouBaoConfig.getUrl())
                .build();
    }
    
    private DouBao(DouBaoSerializationData data) {
        super(data);
        conversationId = data.conversationId;
        service = ArkService
                .builder()
                .apiKey(DouBaoConfig.getToken())
                .baseUrl(DouBaoConfig.getUrl())
                .build();
    }
    
    @RegisterAgent
    public static void init() {
//        log.info("Doubao static init");
//        DouBaoConfig.init();
//        AgentController.getInstance().registerAgentClassInitFunction(
//                () -> {
//                    var agentManager = AgentController.getInstance().getAgentManager();
//                    agentManager.registerAgentMapping("DouBao", DouBao::newInstance);
//
//                    agentManager.registerAgentPathConsumer(
//                            path -> {
//                                if (path.toString().endsWith(".doubao")) {
//                                    return "DouBao";
//                                }
//                                return null;
//                            }
//                    );
//                }
//        );
    }
    
    @Contract(" -> new")
    private static @NotNull DeepSeek newInstance() {
        return new DeepSeek(
                "DouBao" + (new Random()).nextInt(),
                UUID.randomUUID(),
                new HashSet<>(),
                new HashSet<>(),
                DouBaoConfig.getModelName(),
                DouBaoConfig.getDisplayName(),
                DouBaoConfig.getTimePerMin()
        );
    }
    
    @Override
    protected @NotNull String message(@NotNull List<Content> messages) throws UnSupportFormattedMessage {
        return "";
    }
    
    private CreateResponsesRequest buildRequest(@NotNull List<Content> messages) {
        return null;
    }
    
    @Override
    @SneakyThrows
    public void saveToFile(@NotNull Path path) {
        FileUtil.saveJsonToFile(this, path);
    }
    
    @Override
    public void loadFromFile(@NotNull Path path) {
        loadFileToManager(path, DouBaoSerializationData.class, DouBao.class);
    }
    
    @Override
    public void reloadConfig() {
        DouBaoConfig.init();
    }
    
    @Data
    @EqualsAndHashCode(callSuper=true)
    private static class DouBaoSerializationData extends AgentSerializationData {
        private String conversationId;
        
        public DouBaoSerializationData(@NotNull DouBao agent) {
            super(agent);
            this.conversationId = agent.conversationId;
        }
        
        public DouBaoSerializationData() {
            super();
            this.conversationId = "";
        }
    }
}
