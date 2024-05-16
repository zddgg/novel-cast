package com.example.novelcastserver.bean;

import lombok.Data;

import java.util.List;

@Data
public class GsvModel {
    private Integer id;
    private String name;
    private String group;
    private String gptWeights;
    private String sovitsWeights;
    private List<String> tags;
}
