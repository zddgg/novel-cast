package com.example.novelcastserver.bean;

import lombok.Data;

import java.util.List;

@Data
public class RoleSpeechVO {
    private Boolean processFlag;
    private List<SpeechConfig> speechConfigs;
}
