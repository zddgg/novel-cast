package com.example.novelcastserver.bean;

import lombok.Data;

import java.util.List;

@Data
public class AiResult {

    private List<Role> roles;

    private List<LinesMapping> linesMappings;
}
