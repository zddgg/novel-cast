package com.example.novelcastserver.bean;

import lombok.Data;

import java.util.List;

@Data
public class SpeechConfig {
    private Boolean processFlag;
    private List<RoleSpeechConfig> roleSpeechConfigs;
    private Integer audioMergeInterval;
    private String creatingIndex;
}
