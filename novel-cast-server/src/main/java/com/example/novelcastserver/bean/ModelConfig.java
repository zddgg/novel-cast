package com.example.novelcastserver.bean;

import lombok.Data;

import java.util.List;

@Data
public class ModelConfig {

    private List<RoleModelConfig> commonRoleConfigs;
    private List<RoleModelConfig> roleConfigs;
    private List<LinesConfig> linesConfigs;
    private Boolean aiProcess;
    private Boolean aiIgnore;
    private Boolean hasSpeechConfig;

    @Data
    public static class RoleModelConfig {
        private Role role;
        private List<Model> models;
        private String strategyType;
        private List<String> moods;
    }

    @Data
    public static class LinesConfig {
        private LinesMapping linesMapping;
        private Model model;
        private String mood;
    }
}

