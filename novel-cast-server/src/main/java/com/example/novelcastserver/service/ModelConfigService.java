package com.example.novelcastserver.service;

import com.example.novelcastserver.bean.LinesMapping;
import com.example.novelcastserver.bean.ModelConfig;
import com.example.novelcastserver.bean.Role;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ModelConfigService {

    public ModelConfig buildModelConfig(List<Role> roles, List<LinesMapping> linesMappings) {
        ModelConfig modelConfig = new ModelConfig();

        List<ModelConfig.RoleModelConfig> roleConfigs = roles.stream().map(role -> {
            ModelConfig.RoleModelConfig roleConfig = new ModelConfig.RoleModelConfig();
            roleConfig.setRole(role);
            return roleConfig;
        }).toList();
        List<ModelConfig.LinesConfig> linesConfigs = linesMappings.stream().map(linesMapping -> {
            ModelConfig.LinesConfig linesConfig = new ModelConfig.LinesConfig();
            linesConfig.setLinesMapping(linesMapping);
            return linesConfig;
        }).toList();

        ModelConfig.RoleModelConfig titleRoleConfig = new ModelConfig.RoleModelConfig();
        titleRoleConfig.setRole(new Role("标题", "未知", "未知"));

        ModelConfig.RoleModelConfig asideRoleConfig = new ModelConfig.RoleModelConfig();
        asideRoleConfig.setRole(new Role("旁白", "未知", "未知"));

        List<ModelConfig.RoleModelConfig> commonRoleConfigs = new ArrayList<>(List.of(titleRoleConfig, asideRoleConfig));

        modelConfig.setCommonRoleConfigs(commonRoleConfigs);
        modelConfig.setRoleConfigs(roleConfigs);
        modelConfig.setLinesConfigs(linesConfigs);
        return modelConfig;
    }
}
