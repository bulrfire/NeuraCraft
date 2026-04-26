package com.bulefire.neuracraft.core.inside.model.doubao;

import com.bulefire.neuracraft.compatibility.util.FileUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.nio.file.Path;


public class DouBaoConfig {
    private static final Path configPath = FileUtil.agent_config_url.resolve("deepseek.json");

    @Getter
    private static String modelName;
    @Getter
    private static String displayName;
    @Getter
    private static int timePerMin;
    @Getter
    private static String url;
    @Getter
    private static String token;
    @Getter
    private static String prompt;

    @SneakyThrows
    public static void init() {
        if (! configPath.toFile().exists()) {
            configPath.toFile().getParentFile().mkdirs();
            configPath.toFile().createNewFile();
            FileUtil.saveJsonToFile(new Config(), configPath);
        }
        load();
    }

    @SneakyThrows
    public static void load() {
        Config config = FileUtil.loadJsonFromFile(configPath, Config.class);
        modelName = config.getModelName();
        displayName = config.getDisplayName();
        timePerMin = config.getTimePerMin();
        url = config.getUrl();
        token = config.getToken();
        prompt = config.getPrompt();
    }

    @Getter
    @Setter
    public static class Config {
        private String modelName;
        private String displayName;
        private int timePerMin;
        private String url;
        private String token;
        private String prompt;

        public Config() {
            modelName = "deepseek-reasoner";
            displayName = "DeepSeek";
            timePerMin = 60;
            url = "https://api.deepseek.com/chat/completions";
            token = "";
            prompt = "你是一个游戏内的助手，你在和很用户对话，每个用户使用 [username(uuid)] 区分，用户的加入和退出游戏都会通知你，你需要做出相应的欢迎和播报。用尽量简短的语言回复，不要使用emoji，可以使用颜文字，使用文本格式输出而不是markdown。你需要保持热情面对用户.不要过度使用命令,仅在确实需要的时候使用.\n";
        }
    }
}
