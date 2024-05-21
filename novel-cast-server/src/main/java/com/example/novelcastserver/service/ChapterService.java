package com.example.novelcastserver.service;

import com.alibaba.fastjson2.JSON;
import com.example.novelcastserver.bean.*;
import com.example.novelcastserver.config.PathConfig;
import com.example.novelcastserver.utils.AudioUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChapterService {

    private final PathConfig pathConfig;

    public ChapterService(PathConfig pathConfig) {
        this.pathConfig = pathConfig;
    }

    public ArrayList<RoleSpeechConfig> createRoleSpeechesConfig(String project, String chapter) throws IOException {
        ChapterInfo chapterInfo = pathConfig.getChapterInfo(project, chapter);
        ModelConfig modelConfig = pathConfig.getModelConfig(project, chapter);
        ProjectConfig projectConfig = pathConfig.getProjectConfig(project);

        ProjectConfig.ProjectGlobalConfig projectGlobalConfig = projectConfig.getGlobalConfig();
        List<ProjectConfig.ProjectRoleConfig> projectRoleConfigs = projectConfig.getRoleConfigs();
        ProjectConfig.ProjectTextConfig textConfig = projectConfig.getTextConfig();

        Map<String, ProjectConfig.ProjectRoleConfig> projectRoleConfigMap = projectRoleConfigs.stream()
                .collect(Collectors.toMap(ProjectConfig.ProjectRoleConfig::getRole, Function.identity(), (v1, v2) -> v1));

        // 公共角色配置
        Map<String, ModelConfig.RoleModelConfig> commonRoleConfigMap = modelConfig.getCommonRoleConfigs()
                .stream()
                .collect(Collectors.toMap(r -> r.getRole().getRole(), roleConfig -> roleConfig, (a, b) -> b, HashMap::new));


        // 角色配置
        Map<String, ModelConfig.RoleModelConfig> roleConfigMap = modelConfig.getRoleConfigs()
                .stream()
                .collect(Collectors.toMap(r -> r.getRole().getRole(), roleConfig -> roleConfig, (a, b) -> b, HashMap::new));

        // 台词配置
        Map<String, ModelConfig.LinesConfig> linesConfigMap = modelConfig.getLinesConfigs()
                .stream()
                .collect(Collectors.toMap(r -> r.getLinesMapping().getLinesIndex(), linesConfig -> linesConfig, (a, b) -> b, HashMap::new));

        ArrayList<RoleSpeechConfig> roleSpeechConfigs = new ArrayList<>();
        chapterInfo.getLineInfos().forEach(lineInfo -> {
            lineInfo.getSentenceInfos().forEach(sentenceInfo -> {

                RoleSpeechConfig roleSpeechConfig = new RoleSpeechConfig();
                roleSpeechConfig.setSpeedControl(1.0f);
                roleSpeechConfig.setTextLanguage(textConfig.getTextLanguage());

                GsvModel selectGsvModel = projectGlobalConfig.getDefaultModel().getGsvModel();
                Model selectModel = projectGlobalConfig.getDefaultModel().getModel();
                String selectMood = projectGlobalConfig.getDefaultModel().getMood();

                String linesIndex = lineInfo.getIndex() + "-" + sentenceInfo.getIndex();
                // 具体角色台词部分
                if (linesConfigMap.containsKey(linesIndex)) {
                    ModelConfig.LinesConfig linesConfig = linesConfigMap.get(linesIndex);
                    LinesMapping linesMapping = linesConfig.getLinesMapping();
                    GsvModel linesConfigGsvModel = linesConfig.getGsvModel();
                    Model linesConfigModel = linesConfig.getModel();
                    String linesConfigMood = linesConfig.getMood();

                    String role = linesMapping.getRole();
                    roleSpeechConfig.setRole(role);
                    roleSpeechConfig.setGender(linesMapping.getGender());
                    roleSpeechConfig.setAgeGroup(linesMapping.getAgeGroup());

                    // 观众配置
                    if (StringUtils.equals("观众", role)) {

                        // 项目配置
                        if (Objects.nonNull(projectGlobalConfig.getViewersModel().getGsvModel())) {
                            selectGsvModel = projectGlobalConfig.getViewersModel().getGsvModel();
                        }
                        if (Objects.nonNull(projectGlobalConfig.getViewersModel().getModel())) {
                            selectModel = projectGlobalConfig.getViewersModel().getModel();
                        }
                        if (Objects.nonNull(projectGlobalConfig.getViewersModel().getMood())) {
                            selectMood = projectGlobalConfig.getViewersModel().getMood();
                        }

                        // 角色配置
                        ModelConfig.RoleModelConfig viewersRoleConfig = roleConfigMap.get("观众");
                        if (Objects.nonNull(viewersRoleConfig)) {
                            if (Objects.nonNull(viewersRoleConfig.getGsvModel())) {
                                selectGsvModel = viewersRoleConfig.getGsvModel();
                            }
                            if (Objects.nonNull(viewersRoleConfig.getModel())) {
                                selectModel = viewersRoleConfig.getModel();
                            }
                            if (Objects.nonNull(viewersRoleConfig.getMood())) {
                                selectMood = viewersRoleConfig.getMood();
                            }
                        }

                        // 台词配置
                        if (Objects.nonNull(linesConfigGsvModel)
                                && StringUtils.isNotBlank(linesConfigGsvModel.getGroup())
                                && StringUtils.isNotBlank(linesConfigGsvModel.getName())) {
                            selectGsvModel = linesConfigGsvModel;
                        }
                        if (Objects.nonNull(linesConfigModel)
                                && StringUtils.isNotBlank(linesConfigModel.getGroup())
                                && StringUtils.isNotBlank(linesConfigModel.getName())) {
                            selectModel = linesConfigModel;
                        }
                        if (StringUtils.isNotBlank(linesConfigMood)) {
                            selectMood = linesConfigMood;
                        }

                    } else {
                        // 有名字的角色配置

                        // 项目配置
                        ProjectConfig.ProjectRoleConfig projectRoleConfig = projectRoleConfigMap.get(role);
                        if (Objects.nonNull(projectRoleConfig) && Objects.nonNull(projectRoleConfig.getGsvModel())) {
                            selectGsvModel = projectRoleConfig.getGsvModel();
                        }
                        if (Objects.nonNull(projectRoleConfig) && Objects.nonNull(projectRoleConfig.getModel())) {
                            selectModel = projectRoleConfig.getModel();
                        }
                        if (Objects.nonNull(projectRoleConfig) && Objects.nonNull(projectRoleConfig.getMood())) {
                            selectMood = projectRoleConfig.getMood();
                        }

                        // 角色配置
                        ModelConfig.RoleModelConfig roleConfig = roleConfigMap.get(role);
                        if (Objects.nonNull(roleConfig) && Objects.nonNull(roleConfig.getGsvModel())) {
                            selectGsvModel = roleConfig.getGsvModel();
                        }
                        if (Objects.nonNull(roleConfig) && Objects.nonNull(roleConfig.getModel())) {
                            selectModel = roleConfig.getModel();
                        }
                        if (Objects.nonNull(roleConfig) && Objects.nonNull(roleConfig.getMood())) {
                            selectMood = roleConfig.getMood();
                        }

                        // 台词配置
                        if (Objects.nonNull(linesConfigGsvModel)
                                && StringUtils.isNotBlank(linesConfigGsvModel.getGroup())
                                && StringUtils.isNotBlank(linesConfigGsvModel.getName())) {
                            selectGsvModel = linesConfigGsvModel;
                        }
                        if (Objects.nonNull(linesConfigModel)
                                && StringUtils.isNotBlank(linesConfigModel.getGroup())
                                && StringUtils.isNotBlank(linesConfigModel.getName())) {
                            selectModel = linesConfigModel;
                        }
                        if (StringUtils.isNotBlank(linesConfigMood)) {
                            selectMood = linesConfigMood;
                        }
                    }
                } else if (linesIndex.startsWith("0-0")) {

                    roleSpeechConfig.setRole("标题");

                    // 项目配置
                    ProjectConfig.ProjectModelConfig titleModel = projectGlobalConfig.getTitleModel();
                    if (Objects.nonNull(titleModel) && Objects.nonNull(titleModel.getGsvModel())) {
                        selectGsvModel = titleModel.getGsvModel();
                    }
                    if (Objects.nonNull(titleModel) && Objects.nonNull(titleModel.getModel())) {
                        selectModel = titleModel.getModel();
                    }
                    if (Objects.nonNull(titleModel) && Objects.nonNull(titleModel.getMood())) {
                        selectMood = titleModel.getMood();
                    }

                    // 模型选择页配置
                    ModelConfig.RoleModelConfig asideConfig = commonRoleConfigMap.get("标题");
                    if (Objects.nonNull(asideConfig) && Objects.nonNull(asideConfig.getGsvModel())) {
                        selectGsvModel = asideConfig.getGsvModel();
                    }
                    if (Objects.nonNull(asideConfig) && Objects.nonNull(asideConfig.getModel())) {
                        selectModel = asideConfig.getModel();
                    }
                    if (Objects.nonNull(asideConfig) && Objects.nonNull(asideConfig.getMood())) {
                        selectMood = asideConfig.getMood();
                    }

                } else {
                    roleSpeechConfig.setRole("旁白");

                    // 项目配置
                    ProjectConfig.ProjectModelConfig asideModel = projectGlobalConfig.getAsideModel();
                    if (Objects.nonNull(asideModel) && Objects.nonNull(asideModel.getGsvModel())) {
                        selectGsvModel = asideModel.getGsvModel();
                    }
                    if (Objects.nonNull(asideModel) && Objects.nonNull(asideModel.getModel())) {
                        selectModel = asideModel.getModel();
                    }
                    if (Objects.nonNull(asideModel) && Objects.nonNull(asideModel.getMood())) {
                        selectMood = asideModel.getMood();
                    }

                    // 模型选择页配置
                    ModelConfig.RoleModelConfig asideConfig = commonRoleConfigMap.get("旁白");
                    if (Objects.nonNull(asideConfig) && Objects.nonNull(asideConfig.getGsvModel())) {
                        selectGsvModel = asideConfig.getGsvModel();
                    }
                    if (Objects.nonNull(asideConfig) && Objects.nonNull(asideConfig.getModel())) {
                        selectModel = asideConfig.getModel();
                    }
                    if (Objects.nonNull(asideConfig) && Objects.nonNull(asideConfig.getMood())) {
                        selectMood = asideConfig.getMood();
                    }
                }

                roleSpeechConfig.setLinesIndex(linesIndex);
                roleSpeechConfig.setLines(sentenceInfo.getContent());
                roleSpeechConfig.setGroup(selectModel.getGroup());
                roleSpeechConfig.setName(selectModel.getName());
                roleSpeechConfig.setMood(selectMood);
                roleSpeechConfig.setGsvModelGroup(selectGsvModel.getGroup());
                roleSpeechConfig.setGsvModelName(selectGsvModel.getName());

                roleSpeechConfigs.add(roleSpeechConfig);
            });
        });

        return roleSpeechConfigs;
    }

    public SpeechConfig querySpeechConfig(String project, String chapter) throws IOException {
        SpeechConfig speechConfig = pathConfig.getSpeechConfig(project, chapter);
        if (Objects.nonNull(speechConfig)) {
            List<RoleSpeechConfig> roleSpeechConfigs = speechConfig.getRoleSpeechConfigs();
            roleSpeechConfigs.sort(Comparator.comparing(
                    RoleSpeechConfig::getLinesIndex, // 确保我们比较的是字符串
                    Comparator.comparing(
                            (String s) -> Integer.parseInt(s.split("-")[0])
                    ).thenComparing(
                            (String s) -> Integer.parseInt(s.split("-")[1])
                    )
            ));
            Path wavPath = pathConfig.getLinesAudioDirPath(project, chapter);
            if (Files.exists(wavPath)) {
                Map<String, String> fileNameMap = Files.list(wavPath).map(path -> path.getFileName().toString())
                        .collect(Collectors.toMap((String name) -> {
                            String[] split = name.replace(".wav", "").split("-");
                            return split[0] + "-" + split[1];
                        }, Function.identity(), (v1, v2) -> v2));
                for (RoleSpeechConfig roleSpeechConfig : roleSpeechConfigs) {
                    if (fileNameMap.containsKey(roleSpeechConfig.getLinesIndex())) {
                        String wavUrl = pathConfig.getLinesAudioUrl(project, chapter, fileNameMap.get(roleSpeechConfig.getLinesIndex()));
                        roleSpeechConfig.setAudioUrl(wavUrl);
                    }
                }
            }
        }

        return speechConfig;
    }

    public void combineAudio(String project, String chapter, SpeechConfig speechConfig) throws IOException {
        if (Objects.isNull(speechConfig) || CollectionUtils.isEmpty(speechConfig.getRoleSpeechConfigs())) {
            return;
        }

        String chapterPath = pathConfig.getChapterPath(project, chapter);
        String tmpFilePath = chapterPath + "tmp" + File.separator;

        Map<String, RoleSpeechConfig> speechConfigMap = speechConfig.getRoleSpeechConfigs().stream()
                .collect(Collectors.toMap(RoleSpeechConfig::getLinesIndex, Function.identity()));

        ProjectConfig projectConfig = pathConfig.getProjectConfig(project);
        AudioConfig audioConfig = projectConfig.getAudioConfig();

        Integer audioMergeInterval = 0;
        if (Objects.nonNull(audioConfig) && Objects.nonNull(audioConfig.getAudioMergeInterval())) {
            audioMergeInterval = audioConfig.getAudioMergeInterval() / 100 * 100;
        }
        if (Objects.nonNull(speechConfig.getAudioMergeInterval())) {
            audioMergeInterval = speechConfig.getAudioMergeInterval() / 100 * 100;
        }

        String tmpAudio = "";
        if (audioMergeInterval > 0) {
            tmpAudio = Path.of(tmpFilePath + "silentAudio.wav").toAbsolutePath().toString();
            Files.createDirectories(Path.of(tmpAudio).toAbsolutePath().getParent());
            try {
                AudioUtils.generateSilentAudio(tmpAudio, Long.valueOf(audioMergeInterval));
            } catch (Exception e) {
                log.error("生成空白音频失败, {}", e.getMessage(), e);
                audioMergeInterval = 0;
            }
        }

        Path audioDirPath = pathConfig.getLinesAudioDirPath(project, chapter);
        if (Files.exists(audioDirPath)) {

            Map<String, String> filePathMap = new HashMap<>();

            Files.list(audioDirPath).forEach(path -> {
                String[] split = path.getFileName().toString().replace(".wav", "").split("-");
                String linesIndex = split[0] + "-" + split[1];

                try {
                    RoleSpeechConfig roleSpeechConfig = speechConfigMap.get(linesIndex);
                    if (Objects.nonNull(roleSpeechConfig)
                            && !Objects.equals(roleSpeechConfig.getCombineIgnore(), Boolean.TRUE)) {

                        String targetPath = path.toAbsolutePath().toString();
                        long audioDuration;

                        if (Objects.nonNull(roleSpeechConfig.getSpeedControl())
                                && roleSpeechConfig.getSpeedControl() != 1) {
                            targetPath = tmpFilePath + path.getFileName().toString();
                            AudioUtils.audioSpeedControl(path.toAbsolutePath().toString(), roleSpeechConfig.getSpeedControl(), targetPath);

                        }
                        audioDuration = AudioUtils.getAudioTime(targetPath);

                        roleSpeechConfig.setDuration(audioDuration);

                        filePathMap.put(path.getFileName().toString(), targetPath);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            if (CollectionUtils.isEmpty(filePathMap)) {
                return;
            }

            List<String> fileNames = filePathMap.keySet().stream().sorted(Comparator.comparing(
                    String::toString, // 确保我们比较的是字符串
                    Comparator.comparing(
                            (String s) -> Integer.parseInt(s.split("-")[0])
                    ).thenComparing(
                            (String s) -> Integer.parseInt(s.split("-")[1])
                    )
            )).toList();

            List<String> mewList = new ArrayList<>();
            for (int i = 0; i < fileNames.size(); i++) {
                mewList.add(filePathMap.get(fileNames.get(i)));
                if (audioMergeInterval > 0) {
                    if (i < fileNames.size() - 1) {
                        mewList.add(tmpAudio);
                    }
                }
            }

            // 合并音频
            long time = new Date().getTime();
            String output = chapterPath + "output-" + time + ".wav";
            AudioUtils.audioConcat(output, mewList);

            // 删除之前的output
            List<Path> deletePath = new ArrayList<>();
            Files.list(Path.of(chapterPath)).forEach(path -> {
                if (path.getFileName().toString().startsWith("output")
                        && !StringUtils.equals(path.getFileName().toString(), "output-" + time + ".wav")) {
                    deletePath.add(path);
                }
            });
            for (Path path : deletePath) {
                Files.deleteIfExists(path);
            }

            // 保存音频时长
            SpeechConfig save = new SpeechConfig();
            save.setAudioMergeInterval(speechConfig.getAudioMergeInterval());
            save.setRoleSpeechConfigs(speechConfigMap.values().stream().toList());
            pathConfig.writeSpeechConfig(project, chapter, save);

            // 删除tmp文件夹
        }

    }

    public static List<Role> combineRoles(List<Role> roles, List<LinesMapping> linesMappings) {
        Map<String, Long> linesRoleCountMap = linesMappings.stream()
                .collect(Collectors.groupingBy(LinesMapping::getRole, Collectors.counting()));
        List<Role> filterRoles = roles.stream().filter(r -> linesRoleCountMap.containsKey(r.getRole())).toList();

        Set<String> filterRoleSet = filterRoles.stream().map(Role::getRole).collect(Collectors.toSet());
        List<Role> newRoleList = linesMappings.stream().filter(m -> !filterRoleSet.contains(m.getRole()))
                .map(m -> {
                    Role role = new Role();
                    role.setRole(m.getRole());
                    role.setGender(m.getGender());
                    role.setAgeGroup(m.getAgeGroup());
                    return role;
                })
                .collect(Collectors.toMap(Role::getRole, Function.identity(), (v1, v2) -> v1))
                .values().stream().toList();

        List<Role> newRoles = new ArrayList<>();
        newRoles.addAll(filterRoles);
        newRoles.addAll(newRoleList);
        newRoles.sort(Comparator.comparingLong((Role r) -> linesRoleCountMap.get(r.getRole())).reversed());
        return newRoles;
    }

    public AiResult reCombineAiResult(String project, String chapterName, AiResult aiResult) throws IOException {

        ChapterInfo chapterInfo = pathConfig.getChapterInfo(project, chapterName);

        Map<String, ChapterInfo.SentenceInfo> sentenceInfoMap = new HashMap<>();
        chapterInfo.getLineInfos().forEach(lineInfo -> {
            lineInfo.getSentenceInfos().forEach(sentenceInfo -> {
                sentenceInfoMap.put(lineInfo.getIndex() + "-" + sentenceInfo.getIndex(), sentenceInfo);
            });
        });

        List<Role> roles = aiResult.getRoles();
        List<LinesMapping> linesMappings = aiResult.getLinesMappings();
        for (LinesMapping linesMapping : linesMappings) {
            if (sentenceInfoMap.containsKey(linesMapping.getLinesIndex())) {
                linesMapping.setLines(sentenceInfoMap.get(linesMapping.getLinesIndex()).getContent());
            }
        }

        // 大模型总结的角色列表有时候会多也会少
        List<Role> combineRoles = ChapterService.combineRoles(roles, linesMappings);

        AiResult result = new AiResult();
        result.setLinesMappings(linesMappings);
        result.setRoles(combineRoles);
        return result;
    }

    public void saveRoleAndLinesMapping(String project, String chapterName, AiResult aiResult) throws IOException {
        Path rolesJsonPath = Path.of(pathConfig.getRolesFilePath(project, chapterName));
        Files.write(rolesJsonPath, JSON.toJSONString(aiResult.getRoles()).getBytes());

        Path linesMappingsJsonPath = Path.of(pathConfig.getLinesMappingsFilePath(project, chapterName));
        Files.write(linesMappingsJsonPath, JSON.toJSONString(aiResult.getLinesMappings()).getBytes());

    }

    public ChapterConfig setStep(String project, String chapterName, Integer step) throws IOException {
        Path chapterConfigpath = Path.of(pathConfig.getChapterPath(project, chapterName)
                + PathConfig.file_chapterConfig);
        ChapterConfig chapterConfig = new ChapterConfig();
        if (Files.exists(chapterConfigpath)) {
            chapterConfig = JSON.parseObject(Optional.ofNullable(Files.readString(chapterConfigpath))
                    .orElse("{}"), ChapterConfig.class);
        }
        if (Objects.isNull(chapterConfig.getProcessStep())) {
            chapterConfig.setProcessStep(1);
        }
        chapterConfig.setProcessStep(Math.max(step, chapterConfig.getProcessStep()));

        Files.write(chapterConfigpath, JSON.toJSONBytes(chapterConfig));
        return chapterConfig;
    }
}
