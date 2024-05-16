package com.example.novelcastserver.bean;

import lombok.Data;

import java.util.List;

@Data
public class LoadProjectRoleModel {
    private String project;
    private List<String> roles;
}
