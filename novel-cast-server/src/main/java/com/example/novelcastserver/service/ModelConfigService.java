package com.example.novelcastserver.service;

import com.example.novelcastserver.bean.*;
import com.example.novelcastserver.config.PathConfig;
import com.example.novelcastserver.utils.ForEach;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
public class ModelConfigService {

    private final PathConfig pathConfig;

    public ModelConfigService(PathConfig pathConfig) {
        this.pathConfig = pathConfig;
    }

    public ModelConfig buildModelConfig(String project, List<Role> roles, List<LinesMapping> linesMappings) throws IOException {
        ProjectConfig projectConfig = pathConfig.getProjectConfig(project);

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

        GsvModel defaultGsvModel = projectConfig.getGlobalConfig().getDefaultModel().getGsvModel();
        Model defaultModel = projectConfig.getGlobalConfig().getDefaultModel().getModel();
        String defaultMood = projectConfig.getGlobalConfig().getDefaultModel().getMood();

        ModelConfig.RoleModelConfig titleRoleConfig = new ModelConfig.RoleModelConfig();
        titleRoleConfig.setRole(new Role("标题", "未知", "未知"));

        titleRoleConfig.setGsvModel(defaultGsvModel);
        titleRoleConfig.setModel(defaultModel);
        titleRoleConfig.setMood(defaultMood);

        if (Objects.nonNull(projectConfig.getGlobalConfig().getTitleModel().getGsvModel())) {
            titleRoleConfig.setGsvModel(projectConfig.getGlobalConfig().getTitleModel().getGsvModel());
        }
        if (Objects.nonNull(projectConfig.getGlobalConfig().getTitleModel().getModel())) {
            titleRoleConfig.setModel(projectConfig.getGlobalConfig().getTitleModel().getModel());
        }
        if (Objects.nonNull(projectConfig.getGlobalConfig().getTitleModel().getMood())) {
            titleRoleConfig.setMood(projectConfig.getGlobalConfig().getTitleModel().getMood());
        }

        ModelConfig.RoleModelConfig asideRoleConfig = new ModelConfig.RoleModelConfig();
        asideRoleConfig.setRole(new Role("旁白", "未知", "未知"));

        asideRoleConfig.setGsvModel(defaultGsvModel);
        asideRoleConfig.setModel(defaultModel);
        asideRoleConfig.setMood(defaultMood);

        if (Objects.nonNull(projectConfig.getGlobalConfig().getAsideModel().getGsvModel())) {
            asideRoleConfig.setGsvModel(projectConfig.getGlobalConfig().getAsideModel().getGsvModel());
        }
        if (Objects.nonNull(projectConfig.getGlobalConfig().getAsideModel().getModel())) {
            asideRoleConfig.setModel(projectConfig.getGlobalConfig().getAsideModel().getModel());
        }
        if (Objects.nonNull(projectConfig.getGlobalConfig().getAsideModel().getMood())) {
            asideRoleConfig.setMood(projectConfig.getGlobalConfig().getAsideModel().getMood());
        }

        List<ModelConfig.RoleModelConfig> commonRoleConfigs = new ArrayList<>(List.of(titleRoleConfig, asideRoleConfig));


        Map<String, ProjectConfig.ProjectRoleConfig> roleModelMap = projectConfig.getRoleConfigs().stream()
                .collect(Collectors.toMap(ProjectConfig.ProjectRoleConfig::getRole, Function.identity(), (v1, _) -> v1));
        List<ModelConfig.RoleModelConfig> newRoleConfigs = roleConfigs.stream().peek(roleModelConfig -> {
            roleModelConfig.setGsvModel(defaultGsvModel);
            if (roleModelMap.containsKey(roleModelConfig.getRole().getRole())) {
                ProjectConfig.ProjectRoleConfig config = roleModelMap.get(roleModelConfig.getRole().getRole());
                roleModelConfig.setGsvModel(config.getGsvModel());
                roleModelConfig.setModel(config.getModel());
                roleModelConfig.setMood(config.getMood());
            }
        }).toList();

        modelConfig.setCommonRoleConfigs(commonRoleConfigs);
        modelConfig.setRoleConfigs(newRoleConfigs);
        modelConfig.setLinesConfigs(linesConfigs);
        return modelConfig;
    }

    public List<GsvModel> getGsvModels() throws IOException {
        Path gsvModelPath = Path.of(pathConfig.getModelPath(), PathConfig.GPT_SoVITS_MODEL);
        List<GsvModel> gsvModels = new ArrayList<>();
        if (Files.exists(gsvModelPath)) {
            ForEach.forEach(Files.list(gsvModelPath), (groupIndex, group) -> {
                if (Files.exists(group)) {
                    try {
                        ForEach.forEach(Files.list(group), (modelIndex, model) -> {
                            GsvModel gsvModel = new GsvModel();
                            gsvModel.setId(modelIndex);
                            gsvModel.setName(model.getFileName().toString());
                            gsvModel.setGroup(group.getFileName().toString());
                            if (Files.exists(model)) {
                                try {
                                    ForEach.forEach(Files.list(model), (_, modelName) -> {
                                        if (modelName.getFileName().toString().endsWith("ckpt")) {
                                            gsvModel.setGptWeights(modelName.toFile().getName());
                                        }
                                        if (modelName.getFileName().toString().endsWith("pth")) {
                                            gsvModel.setSovitsWeights(modelName.toFile().getName());
                                        }

                                    });
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            gsvModels.add(gsvModel);
                        });
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        gsvModels.sort(Comparator.comparing(v -> !Objects.equals("默认", v.getGroup())));
        return gsvModels;
    }
}
