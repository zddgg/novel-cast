package com.example.novelcastserver.service;

import com.example.novelcastserver.bean.*;
import com.example.novelcastserver.config.PathConfig;
import com.example.novelcastserver.utils.AudioUtils;
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

@Service
public class ChapterService {

    private final PathConfig pathConfig;

    public ChapterService(PathConfig pathConfig) {
        this.pathConfig = pathConfig;
    }

    public ArrayList<SpeechConfig> createRoleSpeechesConfig(String project, String chapter) throws IOException {
        ChapterInfo chapterInfo = pathConfig.getChapterInfo(project, chapter);
        ModelConfig modelConfig = pathConfig.getModelConfig(project, chapter);
        ProjectConfig projectConfig = pathConfig.getProjectConfig(project);

        ProjectConfig.ProjectGlobalConfig projectGlobalConfig = projectConfig.getGlobalConfig();
        List<ProjectConfig.ProjectRoleConfig> projectRoleConfigs = projectConfig.getRoleConfigs();
        Map<String, ProjectConfig.ProjectRoleConfig> projectRoleConfigMap = projectRoleConfigs.stream()
                .collect(Collectors.toMap(ProjectConfig.ProjectRoleConfig::getRole, Function.identity(), (v1, v2) -> v1));

        // 角色配置
        Map<String, ModelConfig.RoleModelConfig> roleConfigMap = modelConfig.getRoleConfigs()
                .stream()
                .collect(Collectors.toMap(r -> r.getRole().getRole(), roleConfig -> roleConfig, (a, b) -> b, HashMap::new));

        // 台词配置
        Map<String, ModelConfig.LinesConfig> linesConfigMap = modelConfig.getLinesConfigs()
                .stream()
                .collect(Collectors.toMap(r -> r.getLinesMapping().getLinesIndex(), linesConfig -> linesConfig, (a, b) -> b, HashMap::new));

        ArrayList<SpeechConfig> speechConfigs = new ArrayList<>();
        chapterInfo.getLineInfos().forEach(lineInfo -> {
            lineInfo.getSentenceInfos().forEach(sentenceInfo -> {

                SpeechConfig speechConfig = new SpeechConfig();

                Model selectModel = projectGlobalConfig.getDefaultModel().getModel();

                String linesIndex = lineInfo.getIndex() + "-" + sentenceInfo.getIndex();
                // 具体角色台词部分
                if (linesConfigMap.containsKey(linesIndex)) {
                    ModelConfig.LinesConfig linesConfig = linesConfigMap.get(linesIndex);
                    LinesMapping linesMapping = linesConfig.getLinesMapping();
                    Model linesConfigModel = linesConfig.getModel();

                    String role = linesMapping.getRole();
                    speechConfig.setRole(role);

                    // 观众配置
                    if (StringUtils.equals("观众", role)) {

                        // 项目配置
                        List<Model> models = projectGlobalConfig.getViewersModel().getModels();
                        if (!CollectionUtils.isEmpty(models)) {
                            selectModel = models.get(new Random().nextInt(models.size()));
                        }

                        // 角色配置
                        ModelConfig.RoleModelConfig viewersRoleConfig = roleConfigMap.get("观众");
                        if (Objects.nonNull(viewersRoleConfig) && !CollectionUtils.isEmpty(viewersRoleConfig.getModels())) {
                            selectModel = viewersRoleConfig.getModels().get(new Random().nextInt(viewersRoleConfig.getModels().size()));
                        }

                        // 台词配置
                        if (Objects.nonNull(linesConfigModel)
                                && StringUtils.isNotBlank(linesConfigModel.getGroup())
                                && StringUtils.isNotBlank(linesConfigModel.getName())) {
                            selectModel = linesConfigModel;
                        }

                    } else {
                        // 有名字的角色配置

                        // 项目配置
                        ProjectConfig.ProjectRoleConfig projectRoleConfig = projectRoleConfigMap.get(role);
                        if (Objects.nonNull(projectRoleConfig) && !CollectionUtils.isEmpty(projectRoleConfig.getModels())) {
                            List<Model> models = projectRoleConfig.getModels();
                            selectModel = models.get(new Random().nextInt(models.size()));
                        }

                        // 角色配置
                        ModelConfig.RoleModelConfig roleConfig = roleConfigMap.get(role);
                        if (Objects.nonNull(roleConfig) && !CollectionUtils.isEmpty(roleConfig.getModels())) {
                            List<Model> models = roleConfig.getModels();
                            selectModel = models.get(new Random().nextInt(models.size()));
                        }

                        // 台词配置
                        if (Objects.nonNull(linesConfigModel)
                                && StringUtils.isNotBlank(linesConfigModel.getGroup())
                                && StringUtils.isNotBlank(linesConfigModel.getName())) {
                            selectModel = linesConfigModel;
                        }
                    }
                } else if (linesIndex.startsWith("0-0")) {

                    speechConfig.setRole("标题");

                    // 项目配置
                    ProjectConfig.ProjectModelsConfig titleModel = projectGlobalConfig.getTitleModel();
                    if (Objects.nonNull(titleModel) && !CollectionUtils.isEmpty(titleModel.getModels())) {
                        List<Model> models = titleModel.getModels();
                        selectModel = models.get(new Random().nextInt(models.size()));
                    }

                } else {
                    speechConfig.setRole("旁白");

                    // 项目配置
                    ProjectConfig.ProjectModelsConfig asideModel = projectGlobalConfig.getAsideModel();
                    if (Objects.nonNull(asideModel) && !CollectionUtils.isEmpty(asideModel.getModels())) {
                        List<Model> models = asideModel.getModels();
                        selectModel = models.get(new Random().nextInt(models.size()));
                    }

                    // 模型选择页配置
                    ModelConfig.RoleModelConfig asideConfig = roleConfigMap.get("旁白");
                    if (Objects.nonNull(asideConfig) && !CollectionUtils.isEmpty(asideConfig.getModels())) {
                        List<Model> models = asideConfig.getModels();
                        selectModel = models.get(new Random().nextInt(models.size()));
                    }
                }

                speechConfig.setLinesIndex(linesIndex);
                speechConfig.setLines(sentenceInfo.getContent());
                speechConfig.setGroup(selectModel.getGroup());
                speechConfig.setName(selectModel.getName());
                speechConfig.setMood("默认");

                speechConfigs.add(speechConfig);
            });
        });

        return speechConfigs;
    }

    public List<SpeechConfig> queryRoleSpeeches(String project, String chapter) throws IOException {
        List<SpeechConfig> speechConfigs = pathConfig.getSpeechConfigs(project, chapter);
        speechConfigs.sort(Comparator.comparing(
                SpeechConfig::getLinesIndex, // 确保我们比较的是字符串
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
                    }, Function.identity()));
            for (SpeechConfig speechConfig : speechConfigs) {
                if (fileNameMap.containsKey(speechConfig.getLinesIndex())) {
                    String wavUrl = pathConfig.getLinesAudioUrl(project, chapter, fileNameMap.get(speechConfig.getLinesIndex()));
                    speechConfig.setAudioUrl(wavUrl);
                }
            }
        }
        return speechConfigs;
    }

    public void combineAudio(String project, String chapter) throws IOException {
        List<SpeechConfig> speechConfigs = pathConfig.getSpeechConfigs(project, chapter);
        Map<String, SpeechConfig> speechConfigMap = speechConfigs.stream().collect(Collectors.toMap(SpeechConfig::getLinesIndex, Function.identity()));

//        String oneSecond = Path.of("tmp/oneSecondSilence.wav").toAbsolutePath().toString();
//        Files.createDirectories(Path.of(oneSecond).toAbsolutePath().getParent());
//        AudioUtils.makeSilenceWav(oneSecond, 2000L);

        Path audioDirPath = pathConfig.getLinesAudioDirPath(project, chapter);
        if (Files.exists(audioDirPath)) {
            List<String> filePaths = new ArrayList<>();

            Files.list(audioDirPath).forEach(path -> {
                String[] split = path.getFileName().toString().replace(".wav", "").split("-");
                String linesIndex = split[0] + "-" + split[1];
                String absolutePath = path.toAbsolutePath().toString();
                filePaths.add(path.getFileName().toString());

                long audioDuration;
                try {
                    audioDuration = AudioUtils.getAudioTime(absolutePath);
                    if (speechConfigMap.containsKey(linesIndex)) {
                        speechConfigMap.get(linesIndex).setDuration(audioDuration);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            filePaths.sort(Comparator.comparing(
                    String::toString, // 确保我们比较的是字符串
                    Comparator.comparing(
                            (String s) -> Integer.parseInt(s.split("-")[0])
                    ).thenComparing(
                            (String s) -> Integer.parseInt(s.split("-")[1])
                    )
            ));

            List<String> mewList = new ArrayList<>();
            for (int i = 0; i < filePaths.size(); i++) {
                mewList.add(pathConfig.getChapterPath(project, chapter) + PathConfig.dir_audio + File.separator + filePaths.get(i));
//                if (i < filePaths.size() - 1) {
//                    mewList.add(oneSecond);
//                }
            }

            AudioUtils.combineWav(pathConfig.getOutAudioPath(project, chapter), mewList);

            pathConfig.writeSpeechConfigs(project, chapter, speechConfigMap.values().stream().toList());
        }

    }
}
