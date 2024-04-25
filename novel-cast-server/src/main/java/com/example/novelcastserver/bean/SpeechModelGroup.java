package com.example.novelcastserver.bean;

import lombok.Data;

import java.util.List;

@Data
public class SpeechModelGroup {
    private Integer index;
    private String group;
    private List<SpeechModel> speechModels;
}