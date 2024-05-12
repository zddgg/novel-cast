package com.example.novelcastserver.bean;

import lombok.Data;

import java.util.List;

@Data
public class ChapterConfig {
    private List<String> linesModifiers;
    private Integer processStep;
}
