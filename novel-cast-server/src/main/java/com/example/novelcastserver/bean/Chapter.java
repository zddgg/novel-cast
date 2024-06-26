package com.example.novelcastserver.bean;

import lombok.Data;

import java.util.List;

@Data
public class Chapter {
    private String chapterName;
    private String content;
    private String markedText;
    private List<Lines> linesList;
    private List<LinesMapping> linesMappings;
    private Integer step;
    private String outAudioUrl;
    private List<RoleSpeechConfig> roleSpeechConfigs;
    private Integer audioMergeInterval;
}
