package com.example.novelcastserver.bean;

import lombok.Data;

import java.util.List;

@Data
public class RoleVO {

    private String project;

    private String chapterName;

    private List<Role> roles;
}
