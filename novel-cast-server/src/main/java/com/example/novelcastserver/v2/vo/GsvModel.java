package com.example.novelcastserver.v2.vo;

import lombok.Data;

import java.util.List;

@Data
public class GsvModel {
    private Integer id;
    private String name;
    private String gptWeights;
    private String sovitsWeights;
    private List<String> tags;
}
