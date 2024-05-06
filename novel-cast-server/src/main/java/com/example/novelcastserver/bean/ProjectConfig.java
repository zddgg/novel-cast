package com.example.novelcastserver.bean;

import lombok.Data;

import java.util.List;

@Data
public class ProjectConfig {

    private String project;
    private ProjectGlobalConfig globalConfig;
    private List<ProjectRoleConfig> roleConfigs;
    private AudioConfig audioConfig;

    @Data
    public static class ProjectRoleConfig {
        private String role;
        private List<Model> models;
        private String strategyType;
    }

    @Data
    public static class ProjectModelConfig {
        private Model model;
        private String strategyType;
    }

    @Data
    public static class ProjectModelsConfig {
        private List<Model> models;
        private String strategyType;
    }

    @Data
    public static class ProjectGlobalConfig {
        private ProjectModelConfig defaultModel;
        private ProjectModelsConfig titleModel;
        private ProjectModelsConfig asideModel;
        private ProjectModelsConfig viewersModel;
    }
}
