package com.example.novelcastserver.bean;

import lombok.Data;

import java.util.List;

@Data
public class GsvTimbre {
    private Integer id;
    private String name;
    private String group;
    private String gender;
    private String ageGroup;
    private List<Mood> moods;
    private List<String> tags;

    @Data
    public static class Mood {
        private Integer id;
        private String name;
        private List<Audio> audios;
    }

    @Data
    public static class Audio {
        private Integer id;
        private String name;
        private String text;
        private String lang;
    }
}
