package com.example.novelcastserver.bean;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LinesParseVO extends ChapterVO {
    private List<Lines> linesList;
    private List<String> linesModifiers;
}
