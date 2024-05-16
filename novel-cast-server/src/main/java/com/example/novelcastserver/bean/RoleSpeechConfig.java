package com.example.novelcastserver.bean;

import lombok.Data;

@Data
public class RoleSpeechConfig {
    private String linesIndex;
    private String role;
    private String gender;
    private String ageGroup;
    private String lines;
    private String gsvModelGroup;
    private String gsvModelName;
    private String group;
    private String name;
    private String mood;
    private String promptAudioPath;
    private String promptText;
    private String audioUrl;
    private Long duration;
    private Float speedControl;
    private String textLanguage;
    private Boolean combineIgnore;
    private String gptWeightsPath;
    private String sovitsWeightsPath;
}