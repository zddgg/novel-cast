package com.example.novelcastserver.bean;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Project {

    private Integer id;
    private String projectId;
    private String projectName;
    private String processState;
    private Integer chapterNum;
    private String status;
    private LocalDateTime createTime;
}
