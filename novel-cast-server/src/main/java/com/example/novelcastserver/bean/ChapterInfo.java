package com.example.novelcastserver.bean;

import lombok.Data;

import java.util.List;

@Data
public class ChapterInfo {
    private Integer index;
    private String title;
    private Boolean prologue;
    private List<LineInfo> lineInfos;

    @Data
    public static class LineInfo {
        private Integer index;
        private List<SentenceInfo> sentenceInfos;
    }

    @Data
    public static class SentenceInfo {
        private Integer index;
        private String content;
        private Boolean linesFlag;
        private Boolean linesDelFlag;

        public SentenceInfo() {
        }

        public SentenceInfo(Integer index, String content) {
            this.index = index;
            this.content = content;
        }

        public SentenceInfo(Integer index, String content, Boolean linesFlag) {
            this.index = index;
            this.content = content;
            this.linesFlag = linesFlag;
        }
    }
}