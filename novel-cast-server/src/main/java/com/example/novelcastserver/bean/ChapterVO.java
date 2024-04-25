package com.example.novelcastserver.bean;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChapterVO extends PageReq {

    private String project;

    private String chapterName;

    private List<Lines> linesList;
}
