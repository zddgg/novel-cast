package com.example.novelcastserver.bean;

import lombok.Data;

@Data
public class SpeechConfig {
    private String linesIndex;
    private String role;
    private String lines;
    private String group;
    private String name;
    private String mood;
    private String promptAudioPath;
    private String promptText;
    private String audioUrl;
    private Long duration;
}