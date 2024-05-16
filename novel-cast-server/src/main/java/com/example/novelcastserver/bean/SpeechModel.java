package com.example.novelcastserver.bean;

import lombok.Data;

import java.util.List;

@Data
public class SpeechModel {
    private String name;
    private List<Mood> moods;
    private String gender;
    private String ageGroup;
    private String url;
}