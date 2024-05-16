package com.example.novelcastserver.bean;

import lombok.Data;

import java.util.List;

@Data
public class ProjectConfig {

    private String project;
    private ProjectGlobalConfig globalConfig;
    private List<ProjectRoleConfig> roleConfigs;
    private AudioConfig audioConfig;
    private ProjectTextConfig textConfig;
    private Integer chapterNum;

    @Data
    public static class ProjectRoleConfig extends ChapterVO {
        private String role;
        private GsvModel gsvModel;
        private Model model;
        private String strategyType;
        private String mood;
    }

    @Data
    public static class ProjectModelConfig {
        private GsvModel gsvModel;
        private Model model;
        private String strategyType;
        private String mood;
    }

    @Data
    public static class ProjectGlobalConfig {
        private ProjectModelConfig defaultModel;
        private ProjectModelConfig titleModel;
        private ProjectModelConfig asideModel;
        private ProjectModelConfig viewersModel;
    }

    @Data
    public static class ProjectTextConfig {
        private List<String> linesModifiers;
        private String chapterTitlePattern;
        private String textLanguage;
    }
}
