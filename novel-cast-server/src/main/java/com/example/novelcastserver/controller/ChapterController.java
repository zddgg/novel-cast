package com.example.novelcastserver.controller;

import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson2.JSON;
import com.example.novelcastserver.bean.*;
import com.example.novelcastserver.config.PathConfig;
import com.example.novelcastserver.exception.BizException;
import com.example.novelcastserver.service.AiInferenceService;
import com.example.novelcastserver.service.ChapterService;
import com.example.novelcastserver.service.ModelConfigService;
import com.example.novelcastserver.utils.AudioUtils;
import com.example.novelcastserver.utils.ChapterExtractor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/chapter")
public class ChapterController {

    public static final Map<String, String> audioCreateProcessMap = new HashMap<>();

    @Resource
    private RestTemplate restTemplate;

    private final PathConfig pathConfig;

    private final ChapterService chapterService;

    private final AiInferenceService aiInferenceService;

    private final ModelConfigService modelConfigService;

    public ChapterController(PathConfig pathConfig, ChapterService chapterService, AiInferenceService aiInferenceService, ModelConfigService modelConfigService) {
        this.pathConfig = pathConfig;
        this.chapterService = chapterService;
        this.aiInferenceService = aiInferenceService;
        this.modelConfigService = modelConfigService;
    }


    /**
     * step0: chapterInfo.json
     * step1: aiResult.json
     * step3: modelConfig.json
     * step4: audio/**
     *
     * @param vo
     * @return
     * @throws IOException
     */
    @PostMapping("pageList")
    public Result<Page<Chapter>> chapterPageList(@RequestBody ChapterVO vo) throws IOException {
        String project = vo.getProject();
        Path projectPath = Path.of(pathConfig.getChapterPath(project));
        if (Files.notExists(projectPath)) {
            throw new BizException("项目不存在~");
        }

        List<Chapter> list = Files.list(projectPath).map(path -> {
                    Chapter chapter = new Chapter();
                    String chapterName = path.getFileName().toString();
                    chapter.setChapterName(chapterName);
                    AtomicInteger processStep = new AtomicInteger(0);
                    try {
                        Files.list(path).forEach(path1 -> {
                            if ("chapterConfig.json".equals(path1.getFileName().toString())) {
                                try {
                                    ChapterConfig chapterConfig = JSON.parseObject(Files.readString(path1), ChapterConfig.class);
                                    if (Objects.nonNull(chapterConfig)
                                            && Objects.nonNull(chapterConfig.getProcessStep())) {
                                        processStep.set(chapterConfig.getProcessStep());
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            if (path1.getFileName().toString().startsWith("output")) {
                                chapter.setOutAudioUrl(pathConfig.getOutAudioUrl(project, chapterName, path1.getFileName().toString()));
                            }
                        });
                    } catch (IOException e) {
                        throw new BizException(e.getMessage());
                    }

                    try {

                        ProjectConfig projectConfig = pathConfig.getProjectConfig(project);
                        if (Objects.nonNull(projectConfig)
                                && Objects.nonNull(projectConfig.getAudioConfig())
                                && Objects.nonNull(projectConfig.getAudioConfig().getAudioMergeInterval())
                                && projectConfig.getAudioConfig().getAudioMergeInterval() != 0) {
                            chapter.setAudioMergeInterval(projectConfig.getAudioConfig().getAudioMergeInterval());
                        }

                        SpeechConfig speechConfig = pathConfig.getSpeechConfig(project, chapterName);
                        if (Objects.nonNull(speechConfig)) {
                            chapter.setRoleSpeechConfigs(speechConfig.getRoleSpeechConfigs());

                            if (Objects.nonNull(speechConfig.getAudioMergeInterval())
                                    && speechConfig.getAudioMergeInterval() != 0) {
                                chapter.setAudioMergeInterval(speechConfig.getAudioMergeInterval());
                            }
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    chapter.setStep(processStep.get());

                    return chapter;
                })
                .sorted(Comparator.comparingInt(s -> Integer.parseInt(s.getChapterName().split("-")[0])))
                .toList();

        List<Chapter> records = ListUtil.page(Math.toIntExact(vo.getCurrent() - 1), Math.toIntExact(vo.getPageSize()), list);
        Page<Chapter> page = new Page<>(vo.getCurrent(), vo.getPageSize(), list.size());
        page.setRecords(records);
        return Result.success(page);
    }


    @PostMapping(value = "aiResult")
    public Result<AiResult> aiResult(@RequestBody ChapterVO vo) throws IOException {
        AiResult aiResult = new AiResult();

        Path rolesJsonPath = Path.of(pathConfig.getRolesFilePath(vo.getProject(), vo.getChapterName()));

        Path linesMappingsJsonPath = Path.of(pathConfig.getLinesMappingsFilePath(vo.getProject(), vo.getChapterName()));

        if (Files.notExists(rolesJsonPath) && Files.notExists(linesMappingsJsonPath)) {
            String aiResultJsonPathStr = pathConfig.getAiResultFilePath(vo.getProject(), vo.getChapterName());
            Path aiResultJsonPath = Path.of(aiResultJsonPathStr);
            if (Files.exists(aiResultJsonPath)) {
                String text = Files.readString(aiResultJsonPath);
                if (text.startsWith("```json") || text.endsWith("```")) {
                    text = text.replace("```json", "").replace("```", "");
                    Files.writeString(aiResultJsonPath, text);
                }
                aiResult = JSON.parseObject(text, AiResult.class);
                aiResult = chapterService.reCombineAiResult(vo.getProject(), vo.getChapterName(), aiResult);
                chapterService.saveRoleAndLinesMapping(vo.getProject(), vo.getChapterName(), aiResult);
            }
        } else {
            aiResult.setRoles(JSON.parseArray(Files.readString(rolesJsonPath), Role.class));

            List<LinesMapping> linesMappings = JSON.parseArray(Files.readString(linesMappingsJsonPath), LinesMapping.class);
            ChapterInfo chapterInfo = pathConfig.getChapterInfo(vo.getProject(), vo.getChapterName());

            Map<String, String> contentMap = new HashMap<>();
            chapterInfo.getLineInfos().forEach(lineInfo -> {
                lineInfo.getSentenceInfos().forEach(sentenceInfo -> {
                    contentMap.put(lineInfo.getIndex() + "-" + sentenceInfo.getIndex(), sentenceInfo.getContent());
                });
            });

            for (LinesMapping linesMapping : linesMappings) {
                if (contentMap.containsKey(linesMapping.getLinesIndex())) {
                    linesMapping.setLines(contentMap.get(linesMapping.getLinesIndex()));
                }
            }
            aiResult.setLinesMappings(linesMappings);
        }

        return Result.success(aiResult);
    }


    @PostMapping(value = "aiInference")
    public Flux<String> aiInference(@RequestBody ChapterVO vo) throws IOException {
        return aiInferenceService.roleAndLinesInference(vo, true);
    }

    @PostMapping(value = "aiReInference")
    public Flux<String> aiReInference(@RequestBody ChapterVO vo) throws IOException {
        return aiInferenceService.roleAndLinesInference(vo, false);
    }

    @PostMapping(value = "aiResultFormat")
    public Result<Object> aiResultFormat(@RequestBody AiResultFormatVO vo) throws IOException {
        AiResult aiResult = aiInferenceService.aiResultFormat(vo);
        ModelConfig modelConfig = modelConfigService.buildModelConfig(aiResult.getRoles(), aiResult.getLinesMappings());
        return Result.success(modelConfig);
    }

    @PostMapping(value = "saveAiReInferenceResult")
    public Result<Object> saveAiReInferenceResult(@RequestBody AiResultFormatVO vo) throws IOException {
        AiResult aiResult = aiInferenceService.aiResultFormat(vo);

        Path aiResultPath = Path.of(pathConfig.getAiResultFilePath(vo.getProject(), vo.getChapterName()));
        Files.write(aiResultPath, JSON.toJSONBytes(aiResultPath));

        chapterService.saveRoleAndLinesMapping(vo.getProject(), vo.getChapterName(), aiResult);

        // 删除ignore文件
        Path aiIgnoreJsonPath = Path.of(pathConfig.getAiIgnoreFilePath(vo.getProject(), vo.getChapterName()));
        Files.deleteIfExists(aiIgnoreJsonPath);

        // 删除modelConfig配置
        Path modelConfigPath = Path.of(pathConfig.getModelConfigFilePath(vo.getProject(), vo.getChapterName()));
        Files.deleteIfExists(modelConfigPath);

        return Result.success();
    }

    @PostMapping("ignoreAiResult")
    public Result<Object> ignoreAiResult(@RequestBody ChapterVO vo) throws IOException {
        Files.write(Path.of(pathConfig.getAiIgnoreFilePath(vo.getProject(), vo.getChapterName())), "".getBytes());
        Files.write(Path.of(pathConfig.getRolesFilePath(vo.getProject(), vo.getChapterName())), "[]".getBytes());
        Files.write(Path.of(pathConfig.getLinesMappingsFilePath(vo.getProject(), vo.getChapterName())), "[]".getBytes());
        Files.write(Path.of(pathConfig.getAiResultFilePath(vo.getProject(), vo.getChapterName())), "{}".getBytes());
        return Result.success();
    }

    @PostMapping("detail")
    public Result<ChapterInfo> chapterDetail(@RequestBody ChapterVO vo) throws IOException {
        Path chapterPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()));
        if (Files.notExists(chapterPath) || !Files.isDirectory(chapterPath)) {
            throw new BizException("文件不存在");
        }

        Path chapterInfoPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()) + "chapterInfo.json");

        ChapterInfo chapterInfo = JSON.parseObject(Files.readAllBytes(chapterInfoPath), ChapterInfo.class);

        if (Objects.nonNull(chapterInfo) && CollectionUtils.isEmpty(chapterInfo.getLineInfos())) {
            Path originTextPath = Path.of(pathConfig.getOriginTextPath(vo.getProject(), vo.getChapterName()));
            List<ChapterInfo.LineInfo> lineInfos = ChapterExtractor.parseChapterInfo(Files.readString(originTextPath), Collections.emptyList());
            chapterInfo.setLineInfos(lineInfos);
        }

        return Result.success(chapterInfo);
    }


    @PostMapping("lines")
    public Result<LinesParseVO> lines(@RequestBody ChapterVO vo) throws IOException {

        Path chapterPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()));
        if (Files.notExists(chapterPath) || !Files.isDirectory(chapterPath)) {
            throw new BizException("文件不存在");
        }

        LinesParseVO linesParseVO = new LinesParseVO();

        Path chapterInfoPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()) + "chapterInfo.json");
        ChapterInfo chapterInfo = JSON.parseObject(Files.readAllBytes(chapterInfoPath), ChapterInfo.class);

        List<Lines> linesList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(chapterInfo.getLineInfos())) {
            chapterInfo.getLineInfos().forEach(lineInfo -> {
                lineInfo.getSentenceInfos().forEach(sentenceInfo -> {
                    if (Optional.ofNullable(sentenceInfo.getLinesFlag()).orElse(false)) {
                        Lines lines = new Lines();
                        lines.setIndex(lineInfo.getIndex() + "-" + sentenceInfo.getIndex());
                        lines.setLines(sentenceInfo.getContent());
                        lines.setDelFlag(sentenceInfo.getLinesDelFlag());
                        linesList.add(lines);
                    }
                });
            });
        }
        linesParseVO.setLinesList(linesList);

        ProjectConfig projectConfig = pathConfig.getProjectConfig(vo.getProject());
        ProjectConfig.ProjectTextConfig textConfig = projectConfig.getTextConfig();
        if (Objects.nonNull(textConfig)) {
            linesParseVO.setLinesModifiers(textConfig.getLinesModifiers());
        }

        Path chapterConfigPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()) + PathConfig.file_chapterConfig);
        if (Files.exists(chapterConfigPath)) {
            ChapterConfig chapterConfig = JSON.parseObject(Optional.ofNullable(Files.readString(chapterConfigPath))
                    .orElse("{}"), ChapterConfig.class);
            if (Objects.nonNull(chapterConfig) && !CollectionUtils.isEmpty(chapterConfig.getLinesModifiers())) {
                linesParseVO.setLinesModifiers(chapterConfig.getLinesModifiers());
            }
        }

        return Result.success(linesParseVO);
    }

    @PostMapping("parseLines")
    public Result<Object> parseLines(@RequestBody LinesParseVO vo) throws IOException {

        Path chapterPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()));
        if (Files.notExists(chapterPath) || !Files.isDirectory(chapterPath)) {
            throw new BizException("文件不存在");
        }

        Path chapterInfoPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()) + "chapterInfo.json");
        if (Files.exists(chapterPath)) {
            ChapterInfo chapterInfo = JSON.parseObject(Files.readAllBytes(chapterInfoPath), ChapterInfo.class);
            List<String> linesModifiers = vo.getLinesModifiers();

            Path originTextPath = Path.of(pathConfig.getOriginTextPath(vo.getProject(), vo.getChapterName()));
            List<ChapterInfo.LineInfo> lineInfos = ChapterExtractor.parseChapterInfo(Files.readString(originTextPath), linesModifiers);
            chapterInfo.setLineInfos(lineInfos);

            Files.write(chapterInfoPath, JSON.toJSONString(chapterInfo).getBytes());

            ChapterConfig chapterConfig = chapterService.setStep(vo.getProject(), vo.getChapterName(), 1);
            chapterConfig.setLinesModifiers(vo.getLinesModifiers());

            Path chapterConfigpath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName())
                    + PathConfig.file_chapterConfig);
            Files.write(chapterConfigpath, JSON.toJSONBytes(chapterConfig));
        }

        return Result.success();
    }

    @PostMapping("linesUpdate")
    public Result<Object> linesUpdate(@RequestBody ChapterVO vo) throws IOException {

        Path chapterPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()));
        if (Files.notExists(chapterPath) || !Files.isDirectory(chapterPath)) {
            throw new BizException("文件不存在");
        }

        Path chapterInfoPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()) + "chapterInfo.json");
        ChapterInfo chapterInfo = pathConfig.getChapterInfo(vo.getProject(), vo.getChapterName());

        List<Lines> linesList = vo.getLinesList();
        if (!CollectionUtils.isEmpty(linesList)) {
            Map<String, Lines> linesMap = linesList.stream()
                    .collect(Collectors.toMap(Lines::getIndex, Function.identity()));

            for (ChapterInfo.LineInfo lineInfo : chapterInfo.getLineInfos()) {
                for (ChapterInfo.SentenceInfo sentenceInfo : lineInfo.getSentenceInfos()) {
                    if (linesMap.containsKey(lineInfo.getIndex() + "-" + sentenceInfo.getIndex())) {
                        Lines lines = linesMap.get(lineInfo.getIndex() + "-" + sentenceInfo.getIndex());
                        if (!StringUtils.equals(lines.getLines(), sentenceInfo.getContent())) {
                            sentenceInfo.setContent(lines.getLines());
                        }
                        if (Objects.equals(lines.getDelFlag(), Boolean.TRUE)) {
                            sentenceInfo.setLinesDelFlag(Boolean.TRUE);
                        }
                    }
                }
            }
            Files.write(chapterInfoPath, JSON.toJSONString(chapterInfo).getBytes());
        }

        return Result.success();
    }

    @PostMapping("roles")
    public Result<List<Role>> roles(@RequestBody RoleVO vo) throws IOException {
        Path rolesJsonPath = Path.of(pathConfig.getRolesFilePath(vo.getProject(), vo.getChapterName()));
        List<Role> roles = new ArrayList<>();
        if (Files.exists(rolesJsonPath)) {
            Path linesMappingsJsonPath = Path.of(pathConfig.getLinesMappingsFilePath(vo.getProject(), vo.getChapterName()));
            Map<String, Long> linesRoleCountMap = JSON.parseArray(Files.readString(linesMappingsJsonPath), LinesMapping.class)
                    .stream()
                    .collect(Collectors.groupingBy(LinesMapping::getRole, Collectors.counting()));

            roles = JSON.parseArray(Files.readString(rolesJsonPath), Role.class)
                    .stream().filter(role -> StringUtils.isBlank(role.getBackup()))
                    .sorted(Comparator.comparingLong((Role r) -> linesRoleCountMap.get(r.getRole())).reversed())
                    .toList();
        }
        return Result.success(roles);
    }

    @PostMapping("updateRoles")
    public Result<Object> updateRoles(@RequestBody RoleVO vo) throws IOException {
        Path filePath = Path.of(pathConfig.getRolesFilePath(vo.getProject(), vo.getChapterName()));
        List<Role> roles = vo.getRoles();
        Files.write(filePath, JSON.toJSONBytes(roles));

        Map<String, String> backupMap = roles.stream()
                .filter(role -> StringUtils.isNotBlank(role.getBackup()))
                .collect(Collectors.toMap(Role::getRole, Role::getBackup));
        Path linesMappingsJsonPath = Path.of(pathConfig.getLinesMappingsFilePath(vo.getProject(), vo.getChapterName()));
        if (!CollectionUtils.isEmpty(backupMap) && Files.exists(linesMappingsJsonPath)) {
            List<LinesMapping> linesMappings = JSON.parseArray(Files.readString(linesMappingsJsonPath), LinesMapping.class);
            for (LinesMapping linesMapping : linesMappings) {
                if (backupMap.containsKey(linesMapping.getRole())) {
                    linesMapping.setRole(backupMap.get(linesMapping.getRole()));
                }
            }
            Files.write(linesMappingsJsonPath, JSON.toJSONString(linesMappings).getBytes());
        }

        return Result.success();
    }

    @PostMapping("queryModelConfig")
    public Result<ModelConfig> queryModelConfig(@RequestBody ChapterVO vo) throws IOException {
        ModelConfig modelConfig = new ModelConfig();
        Path modelConfigPath = Path.of(pathConfig.getModelConfigFilePath(vo.getProject(), vo.getChapterName()));
        if (Files.notExists(modelConfigPath) || StringUtils.isBlank(Files.readString(modelConfigPath))) {

            Path rolesJsonPath = Path.of(pathConfig.getRolesFilePath(vo.getProject(), vo.getChapterName()));
            List<Role> roles = new ArrayList<>();
            if (Files.exists(rolesJsonPath)) {
                roles = JSON.parseArray(Files.readString(rolesJsonPath), Role.class);
            }

            Path linesMappingsJsonPath = Path.of(pathConfig.getLinesMappingsFilePath(vo.getProject(), vo.getChapterName()));
            List<LinesMapping> linesMappings = new ArrayList<>();
            if (Files.exists(linesMappingsJsonPath)) {
                linesMappings = JSON.parseArray(Files.readString(linesMappingsJsonPath), LinesMapping.class);
            }

            List<Role> combineRoles = ChapterService.combineRoles(roles, linesMappings);

            modelConfig = modelConfigService.buildModelConfig(combineRoles, linesMappings);
        } else {
            modelConfig = JSON.parseObject(Files.readString(modelConfigPath), ModelConfig.class);
        }

        Path aiResultPath = Path.of(pathConfig.getAiResultFilePath(vo.getProject(), vo.getChapterName()));
        modelConfig.setAiProcess(Files.exists(aiResultPath));

        Path aiIgnorePath = Path.of(pathConfig.getAiIgnoreFilePath(vo.getProject(), vo.getChapterName()));
        modelConfig.setAiIgnore(Files.exists(aiIgnorePath));

        Path speechConfigPath = Path.of(pathConfig.getSpeechConfigFilePath(vo.getProject(), vo.getChapterName()));
        modelConfig.setHasSpeechConfig(Files.exists(speechConfigPath));

        return Result.success(modelConfig);
    }

    @PostMapping("updateModelConfig")
    public Result<Object> updateModelConfig(@RequestBody ModelConfigVO vo) throws IOException {
        // 保存modelConfig
        Path modelConfigPath = Path.of(pathConfig.getModelConfigFilePath(vo.getProject(), vo.getChapterName()));
        Files.write(modelConfigPath, JSON.toJSONBytes(vo.getModelConfig()));


        // 角色合并到旁白以及台词内容的修改写回chapterInfo文件
        ChapterInfo chapterInfo = pathConfig.getChapterInfo(vo.getProject(), vo.getChapterName());

        List<ModelConfig.LinesConfig> linesConfigs = vo.getModelConfig().getLinesConfigs();
        if (!CollectionUtils.isEmpty(linesConfigs)) {

            Map<String, LinesMapping> linesMappingMap = new HashMap<>();
            for (ModelConfig.LinesConfig linesConfig : linesConfigs) {
                linesMappingMap.put(linesConfig.getLinesMapping().getLinesIndex(), linesConfig.getLinesMapping());
            }

            for (ChapterInfo.LineInfo lineInfo : chapterInfo.getLineInfos()) {
                for (ChapterInfo.SentenceInfo sentenceInfo : lineInfo.getSentenceInfos()) {
                    String index = lineInfo.getIndex() + "-" + sentenceInfo.getIndex();
                    if (linesMappingMap.containsKey(index)) {
                        LinesMapping linesMapping = linesMappingMap.get(index);
                        sentenceInfo.setContent(linesMapping.getLines());
                        if (StringUtils.equals("旁白", linesMapping.getRole())) {
                            sentenceInfo.setLinesFlag(false);
                        }
                    }
                }
            }

            Path chapterInfoPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()) + "chapterInfo.json");
            Files.write(chapterInfoPath, JSON.toJSONBytes(chapterInfo));

        }

        chapterService.setStep(vo.getProject(), vo.getChapterName(), 2);

        return Result.success();
    }

    @PostMapping("createSpeechConfig")
    public Result<Object> createSpeechConfig(@RequestBody ChapterVO vo) throws IOException {
        // 生成语音配置
        List<RoleSpeechConfig> roleSpeechConfigs = chapterService.createRoleSpeechesConfig(vo.getProject(), vo.getChapterName());
        SpeechConfig speechConfig = pathConfig.getSpeechConfig(vo.getProject(), vo.getChapterName());
        if (Objects.isNull(speechConfig)) {
            speechConfig = new SpeechConfig();
        }
        speechConfig.setRoleSpeechConfigs(roleSpeechConfigs);
        pathConfig.writeSpeechConfig(vo.getProject(), vo.getChapterName(), speechConfig);
        chapterService.setStep(vo.getProject(), vo.getChapterName(), 3);
        return Result.success();
    }

    @PostMapping("startSpeechesCreate")
    public Result<Object> startSpeechesCreate(@RequestBody ChapterVO vo) throws IOException {

        SpeechConfig speechConfig = chapterService.querySpeechConfig(vo.getProject(), vo.getChapterName());
        Path processFlag = pathConfig.getProcessFlagPath(vo.getProject(), vo.getChapterName());
        if (Files.notExists(processFlag)) {
            Files.createFile(processFlag);
            CompletableFuture.runAsync(() -> {
                        try {
                            int a = 0;
                            for (RoleSpeechConfig roleSpeechConfig : speechConfig.getRoleSpeechConfigs()) {
                                try {
                                    audioCreateProcessMap.put(vo.getProject() + "-" + vo.getChapterName(), roleSpeechConfig.getLinesIndex());
                                    createAudio(vo.getProject(), vo.getChapterName(), roleSpeechConfig);
                                } catch (Exception e) {
                                    log.error(e.getMessage(), e);
                                    throw new RuntimeException(e);
                                }
                                a++;
                            }
                            pathConfig.writeSpeechConfig(vo.getProject(), vo.getChapterName(), speechConfig);
                            audioCreateProcessMap.remove(vo.getProject() + "-" + vo.getChapterName());
                            log.info("全部音频生成结束，文件数: [{}]", a);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } finally {
                            try {
                                Files.deleteIfExists(processFlag);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }, Executors.newFixedThreadPool(1))
                    .exceptionally(e -> {
                        log.error(e.getMessage(), e);
                        return null;
                    });
        }
        speechConfig.setProcessFlag(Files.exists(processFlag));
        speechConfig.setCreatingIndex(audioCreateProcessMap.get(vo.getProject() + "-" + vo.getChapterName()));
        return Result.success(speechConfig);
    }


    public void createAudio(String project, String chapter, RoleSpeechConfig roleSpeechConfig) throws IOException {

        String group = roleSpeechConfig.getGroup();
        String model = roleSpeechConfig.getName();

        String moodPath = group + File.separator + model + File.separator + roleSpeechConfig.getMood();
        String defaultMoodPath = group + File.separator + model + File.separator + "默认";
        if (Files.exists(Path.of(pathConfig.getModelSpeechPath() + moodPath))) {
            Files.list(Path.of(pathConfig.getModelSpeechPath() + moodPath)).forEach(path -> {
                if (path.getFileName().toString().endsWith(".wav")) {
                    roleSpeechConfig.setPromptAudioPath(pathConfig.getRemoteSpeechPath() + moodPath + "/" + path.getFileName().toString());
                    roleSpeechConfig.setPromptText(path.getFileName().toString().replace(".wav", ""));
                }
            });
        } else {
            Files.list(Path.of(pathConfig.getModelSpeechPath() + defaultMoodPath)).forEach(path -> {
                if (path.getFileName().toString().endsWith(".wav")) {
                    roleSpeechConfig.setPromptAudioPath(pathConfig.getRemoteSpeechPath() + defaultMoodPath + "/" + path.getFileName().toString());
                    roleSpeechConfig.setPromptText(path.getFileName().toString().replace(".wav", ""));
                }
            });
        }

        String speechDir = pathConfig.getChapterPath(project, chapter) + "audio" + File.separator;
        if (Files.notExists(Path.of(speechDir))) {
            Files.createDirectories(Path.of(speechDir));
        }

        long time = new Date().getTime();
        String fileName = roleSpeechConfig.getLinesIndex() + "-" + time + ".wav";
        Path wavPath = Path.of(speechDir + fileName);

        long audioDuration;
        if (List.of("。", "……").contains(roleSpeechConfig.getLines())) {
            audioDuration = 1000;
            try {
                deleteFileByPrefix(project, chapter, roleSpeechConfig.getLinesIndex());
                AudioUtils.generateSilentAudio(wavPath.toString(), audioDuration);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {

            HashMap<String, String> map = new HashMap<>();

            map.put("ref_audio_path", roleSpeechConfig.getPromptAudioPath());
            map.put("prompt_text", roleSpeechConfig.getPromptText());
            map.put("prompt_lang", "zh");
            map.put("text", roleSpeechConfig.getLines());
            map.put("text_lang", roleSpeechConfig.getTextLanguage());
            map.put("text_split_method", "cut0");

            log.info("音频参数: [{}]", JSON.toJSONString(map));
            log.info("生成音频, role: [{}], model: [{}], mood: [{}], content: [{}], linesIndex: [{}]", roleSpeechConfig.getRole(),
                    roleSpeechConfig.getName(), roleSpeechConfig.getMood(), roleSpeechConfig.getLines(), roleSpeechConfig.getLinesIndex());

            ResponseEntity<byte[]> response = restTemplate.postForEntity(pathConfig.getGptSoVitsUrl(), map, byte[].class);


            if (response.getStatusCode().is2xxSuccessful()) {


                deleteFileByPrefix(project, chapter, roleSpeechConfig.getLinesIndex());

                Files.write(wavPath, response.getBody());
                log.info("音频生成成功, linesIndex: [{}]", roleSpeechConfig.getLinesIndex());
            }
        }
    }

    private void deleteFileByPrefix(String project, String chapter, String linesIndex) throws IOException {
        String speechDir = pathConfig.getChapterPath(project, chapter) + "audio" + File.separator;

        final ArrayList<Path> paths = new ArrayList<>();
        Files.list(Path.of(speechDir)).forEach(path -> {
            if (path.getFileName().toString().startsWith(linesIndex)) {
                paths.add(path);
            }
        });
        if (!CollectionUtils.isEmpty(paths)) {
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }


    @PostMapping("querySpeechConfig")
    public Result<SpeechConfig> querySpeechConfig(@RequestBody ChapterVO vo) throws IOException {
        Path processFlag = pathConfig.getProcessFlagPath(vo.getProject(), vo.getChapterName());
        SpeechConfig speechConfig = chapterService.querySpeechConfig(vo.getProject(), vo.getChapterName());

        if (Objects.nonNull(speechConfig)) {

            speechConfig.setProcessFlag(Files.exists(processFlag));
            speechConfig.setCreatingIndex(audioCreateProcessMap.get(vo.getProject() + "-" + vo.getChapterName()));

            if (Objects.isNull(speechConfig.getAudioMergeInterval())) {
                ProjectConfig projectConfig = pathConfig.getProjectConfig(vo.getProject());
                AudioConfig audioConfig = projectConfig.getAudioConfig();
                if (Objects.nonNull(audioConfig)) {
                    speechConfig.setAudioMergeInterval(audioConfig.getAudioMergeInterval());
                }
            }
        }

        return Result.success(speechConfig);
    }

    @PostMapping("createSpeech")
    public Result<Object> createSpeech(@RequestBody SpeechCreate speechCreate) throws IOException {
        RoleSpeechConfig newConfig = speechCreate.getRoleSpeechConfig();
        createAudio(speechCreate.getProject(), speechCreate.getChapterName(), newConfig);
        SpeechConfig speechConfig = chapterService.querySpeechConfig(speechCreate.getProject(), speechCreate.getChapterName());

        boolean updateFlag = false;
        ChapterInfo chapterInfo = pathConfig.getChapterInfo(speechCreate.getProject(), speechCreate.getChapterName());
        for (ChapterInfo.LineInfo lineInfo : chapterInfo.getLineInfos()) {
            for (ChapterInfo.SentenceInfo sentenceInfo : lineInfo.getSentenceInfos()) {
                String index = lineInfo.getIndex() + "-" + sentenceInfo.getIndex();
                if (StringUtils.equals(index, newConfig.getLinesIndex())
                        && !StringUtils.equals(sentenceInfo.getContent(), newConfig.getLines())) {
                    sentenceInfo.setContent(newConfig.getLines());
                    updateFlag = true;
                }
            }
        }

        if (updateFlag) {
            Path chapterInfoPath = Path.of(pathConfig.getChapterInfoPath(speechCreate.getProject(), speechCreate.getChapterName()));
            Files.write(chapterInfoPath, JSON.toJSONBytes(chapterInfo));
        }

        for (RoleSpeechConfig roleSpeechConfig : speechConfig.getRoleSpeechConfigs()) {
            if (StringUtils.equals(roleSpeechConfig.getLinesIndex(), newConfig.getLinesIndex())) {
                BeanUtils.copyProperties(newConfig, roleSpeechConfig);
            }
        }
        pathConfig.writeSpeechConfig(speechCreate.getProject(), speechCreate.getChapterName(), speechConfig);
        return Result.success();
    }

    @PostMapping("combineAudio")
    public Result<Object> combineAudio(@RequestBody SpeechCombineVO vo) throws IOException {
        if (Objects.nonNull(vo.getSpeechConfig()) && !CollectionUtils.isEmpty(vo.getSpeechConfig().getRoleSpeechConfigs())) {
            // 保存语音速度 合并间隔
            pathConfig.writeSpeechConfig(vo.getProject(), vo.getChapterName(), vo.getSpeechConfig());
            chapterService.combineAudio(vo.getProject(), vo.getChapterName(), vo.getSpeechConfig());
            chapterService.setStep(vo.getProject(), vo.getChapterName(), 4);
        }
        return Result.success();
    }
}
