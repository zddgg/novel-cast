package com.example.novelcastserver.controller;

import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson2.JSON;
import com.example.novelcastserver.bean.*;
import com.example.novelcastserver.config.PathConfig;
import com.example.novelcastserver.exception.BizException;
import com.example.novelcastserver.service.AiInferenceService;
import com.example.novelcastserver.service.ChapterService;
import com.example.novelcastserver.utils.AudioUtils;
import com.example.novelcastserver.utils.ChapterExtractor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    @Resource
    private RestTemplate restTemplate;

    private final PathConfig pathConfig;

    private final ChapterService chapterService;

    private final AiInferenceService aiInferenceService;

    public ChapterController(PathConfig pathConfig, ChapterService chapterService, AiInferenceService aiInferenceService) {
        this.pathConfig = pathConfig;
        this.chapterService = chapterService;
        this.aiInferenceService = aiInferenceService;
    }


    /**
     * step0: chapterInfo.json
     * step1: aiResult.json
     * step2: lines.json || roles.json || linesMappings.json
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

//        ProjectConfig projectConfig = pathConfig.getProjectConfig(vo.getProject());

        List<Chapter> list = Files.list(projectPath).map(path -> {
                    Chapter chapter = new Chapter();
                    String chapterName = path.getFileName().toString();
                    chapter.setChapterName(chapterName);
                    AtomicInteger i = new AtomicInteger(1);
                    try {
                        Files.list(path).forEach(path1 -> {
                            if ("chapterInfo.json".equals(path1.getFileName().toString())) {
                                try {
                                    ChapterInfo chapterInfo = JSON.parseObject(Files.readString(path1), ChapterInfo.class);
                                    if (Objects.nonNull(chapterInfo) && Objects.nonNull(chapterInfo.getPrologue()) && chapterInfo.getPrologue()) {
                                        i.set(Math.max(i.get(), 3));
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            if ("aiResult.json".equals(path1.getFileName().toString())) {
                                i.set(Math.max(i.get(), 2));
                            }
                            if ("modelConfig.json".equals(path1.getFileName().toString())) {
                                i.set(Math.max(i.get(), 3));
                            }
                            if ("audio".equals(path1.getFileName().toString())) {
                                try {
                                    if (Files.list(path1).findAny().isPresent()) {
                                        i.set(Math.max(i.get(), 4));
                                    }
                                } catch (IOException e) {
                                    throw new BizException(e.getMessage());
                                }
                            }
                            if ("output.wav".equals(path1.getFileName().toString())) {
                                i.set(Math.max(i.get(), 5));
                                chapter.setOutAudioUrl(pathConfig.getOutAudioUrl(project, chapterName));
                            }
                        });
                    } catch (IOException e) {
                        throw new BizException(e.getMessage());
                    }

                    try {
                        chapter.setSpeechConfigs(pathConfig.getSpeechConfigs(project, chapterName));
//                        if (Objects.nonNull(projectConfig)) {
//                            chapter.setAudioConfig(projectConfig.getAudioConfig());
//                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    chapter.setStep(i.get());
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
                aiResult = genRoleAndMapping(vo, aiResult);
            }
        } else {
            aiResult.setRoles(JSON.parseArray(Files.readString(rolesJsonPath), Role.class));
            aiResult.setLinesMappings(JSON.parseArray(Files.readString(linesMappingsJsonPath), LinesMapping.class));
        }

        return Result.success(aiResult);
    }


    @PostMapping(value = "aiInference")
    public Flux<String> aiInference(@RequestBody ChapterVO vo) throws IOException {
//
//        String longString = AiInferenceService.resTemp;
//        int charactersPerSecond = 40;
//
//        return Flux.interval(Duration.ofMillis(200))
//                .map(i -> longString.substring((int) Math.min((i * charactersPerSecond), longString.length()), (int) Math.min((i + 1) * charactersPerSecond, longString.length())))
//                .takeWhile(s -> !s.isEmpty())
//                .doOnNext(System.out::println); // 打印每个输出片段，仅用于演示
//


        Path chapterInfoPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()) + "chapterInfo.json");
        ChapterInfo chapterInfo = JSON.parseObject(Files.readAllBytes(chapterInfoPath), ChapterInfo.class);

        String aiResultJsonPathStr = pathConfig.getAiResultFilePath(vo.getProject(), vo.getChapterName());
        Path aiResultJsonPath = Path.of(aiResultJsonPathStr);
        if (Files.exists(aiResultJsonPath)) {
            Files.write(aiResultJsonPath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        }

        File outputFile = new File(aiResultJsonPath.toFile().getAbsolutePath());
        return aiInferenceService.roleAndLinesInference(chapterInfo)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(s -> {
                    System.out.println(s);
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
                        writer.write(s);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).doOnComplete(() -> {
                    try {
                        parseAiInference(vo);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

    }

    @PostMapping("parseAiInference")
    public Result<Object> parseAiInference(@RequestBody ChapterVO vo) throws IOException {
        String aiResultJsonPathStr = pathConfig.getAiResultFilePath(vo.getProject(), vo.getChapterName());
        Path aiResultJsonPath = Path.of(aiResultJsonPathStr);

        String text = Files.readString(aiResultJsonPath);
        if (text.startsWith("```json") || text.endsWith("```")) {
            text = text.replace("```json", "").replace("```", "");
        }
        AiResult aiResult = JSON.parseObject(text, AiResult.class);
        genRoleAndMapping(vo, aiResult);
        return Result.success();
    }

    private AiResult genRoleAndMapping(ChapterVO vo, AiResult aiResult) throws IOException {

        ChapterInfo chapterInfo = pathConfig.getChapterInfo(vo.getProject(), vo.getChapterName());

        Map<String, ChapterInfo.SentenceInfo> sentenceInfoMap = new HashMap<>();
        chapterInfo.getLineInfos().forEach(lineInfo -> {
            lineInfo.getSentenceInfos().forEach(sentenceInfo -> {
                sentenceInfoMap.put(lineInfo.getIndex() + "-" + sentenceInfo.getIndex(), sentenceInfo);
            });
        });

        List<Role> roles = aiResult.getRoles();
        List<LinesMapping> linesMappings = aiResult.getLinesMappings();
        for (LinesMapping linesMapping : linesMappings) {
            linesMapping.setLines(sentenceInfoMap.get(linesMapping.getLinesIndex()).getContent());
        }

        List<Lines> linesList = linesMappings.stream().map(linesMapping -> {
            Lines lines = new Lines();
            lines.setIndex(linesMapping.getLinesIndex());
            lines.setLines(sentenceInfoMap.get(linesMapping.getLinesIndex()).getContent());
            return lines;
        }).toList();
        Path linesJsonPath = Path.of(pathConfig.getLinesFilePath(vo.getProject(), vo.getChapterName()));
        Files.write(linesJsonPath, JSON.toJSONString(linesList).getBytes());

        // 大模型总结的角色列表有时候会多也会少
        Path rolesJsonPath = Path.of(pathConfig.getRolesFilePath(vo.getProject(), vo.getChapterName()));
        List<Role> combineRoles = combineRoles(roles, linesMappings);
        Files.write(rolesJsonPath, JSON.toJSONString(combineRoles).getBytes());

        Path linesMappingsJsonPath = Path.of(pathConfig.getLinesMappingsFilePath(vo.getProject(), vo.getChapterName()));
        Files.write(linesMappingsJsonPath, JSON.toJSONString(linesMappings).getBytes());

        AiResult result = new AiResult();
        result.setLinesMappings(linesMappings);
        result.setRoles(combineRoles);
        return result;
    }

    @PostMapping("ignoreAiResult")
    public Result<Object> ignoreAiResult(@RequestBody ChapterVO vo) throws IOException {
        Files.write(Path.of(pathConfig.getLinesFilePath(vo.getProject(), vo.getChapterName())), "[]".getBytes());
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
        return Result.success(chapterInfo);
    }


    @PostMapping("lines")
    public Result<List<Lines>> lines(@RequestBody ChapterVO vo) throws IOException {

        Path chapterPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()));
        if (Files.notExists(chapterPath) || !Files.isDirectory(chapterPath)) {
            throw new BizException("文件不存在");
        }

        Path chapterInfoPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()) + "chapterInfo.json");
        ChapterInfo chapterInfo = JSON.parseObject(Files.readAllBytes(chapterInfoPath), ChapterInfo.class);

        List<Lines> linesList = new ArrayList<>();
        chapterInfo.getLineInfos().forEach(lineInfo -> {
            lineInfo.getSentenceInfos().forEach(sentenceInfo -> {
                if (Optional.ofNullable(sentenceInfo.getLines()).orElse(false)) {
                    Lines lines = new Lines();
                    lines.setIndex(lineInfo.getIndex() + "-" + sentenceInfo.getIndex());
                    lines.setLines(sentenceInfo.getContent());
                    linesList.add(lines);
                }
            });
        });

        return Result.success(linesList);
    }

    @PostMapping("reCreateLines")
    public Result<Object> reCreateLines(@RequestBody ChapterVO vo) throws IOException {

        Path chapterPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()));
        if (Files.notExists(chapterPath) || !Files.isDirectory(chapterPath)) {
            throw new BizException("文件不存在");
        }

        Path chapterInfoPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()) + "chapterInfo.json");
        if (Files.exists(chapterPath)) {
            ChapterInfo chapterInfo = JSON.parseObject(Files.readAllBytes(chapterInfoPath), ChapterInfo.class);


            Path originTextPath = Path.of(pathConfig.getOriginTextPath(vo.getProject(), vo.getChapterName()));
            List<ChapterInfo.LineInfo> lineInfos = ChapterExtractor.parseChapterInfo(Files.readString(originTextPath));
            chapterInfo.setLineInfos(lineInfos);

            Files.write(chapterInfoPath, JSON.toJSONString(chapterInfo).getBytes());
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
        ChapterInfo chapterInfo = JSON.parseObject(Files.readAllBytes(chapterInfoPath), ChapterInfo.class);

        List<Lines> linesList = vo.getLinesList();
        if (!CollectionUtils.isEmpty(linesList)) {
            Map<String, Lines> linesMap = linesList.stream()
                    .collect(Collectors.toMap(Lines::getLines, Function.identity()));

            for (ChapterInfo.LineInfo lineInfo : chapterInfo.getLineInfos()) {
                for (ChapterInfo.SentenceInfo sentenceInfo : lineInfo.getSentenceInfos()) {
                    if (linesMap.containsKey(lineInfo.getIndex() + "-" + sentenceInfo.getIndex())) {
                        Lines lines = linesMap.get(lineInfo.getIndex() + "-" + sentenceInfo.getIndex());
                        if (!StringUtils.equals(lines.getLines(), sentenceInfo.getContent())) {
                            sentenceInfo.setContent(lines.getLines());
                        }
                        if (Objects.equals(lines.getDelFlag(), Boolean.TRUE)) {
                            lines.setDelFlag(Boolean.FALSE);
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
        Path speechConfigPath = Path.of(pathConfig.getModelConfigFilePath(vo.getProject(), vo.getChapterName()));
        if (Files.notExists(speechConfigPath) || StringUtils.isBlank(Files.readString(speechConfigPath))) {

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

            List<Role> combineRoles = combineRoles(roles, linesMappings);

            List<ModelConfig.RoleModelConfig> roleConfigs = combineRoles.stream().map(role -> {
                ModelConfig.RoleModelConfig roleConfig = new ModelConfig.RoleModelConfig();
                roleConfig.setRole(role);
                return roleConfig;
            }).toList();
            List<ModelConfig.LinesConfig> linesConfigs = linesMappings.stream().map(linesMapping -> {
                ModelConfig.LinesConfig linesConfig = new ModelConfig.LinesConfig();
                linesConfig.setLinesMapping(linesMapping);
                return linesConfig;
            }).toList();

            ModelConfig.RoleModelConfig asideRoleConfig = new ModelConfig.RoleModelConfig();
            asideRoleConfig.setRole(new Role("旁白", "未知", "未知"));

            List<ModelConfig.RoleModelConfig> newRoleConfigs = new ArrayList<>(List.of(asideRoleConfig));
            newRoleConfigs.addAll(roleConfigs);

            modelConfig.setRoleConfigs(newRoleConfigs);
            modelConfig.setLinesConfigs(linesConfigs);
        } else {
            modelConfig = JSON.parseObject(Files.readString(speechConfigPath), ModelConfig.class);
        }

        return Result.success(modelConfig);
    }

    @PostMapping("updateModelConfig")
    public Result<Object> updateModelConfig(@RequestBody ModelConfigVO vo) throws IOException {
        Path modelConfigPath = Path.of(pathConfig.getModelConfigFilePath(vo.getProject(), vo.getChapterName()));
        Files.write(modelConfigPath, JSON.toJSONBytes(vo.getModelConfig()));
        return Result.success();
    }

    private List<Role> combineRoles(List<Role> roles, List<LinesMapping> linesMappings) {
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

    @PostMapping("startSpeechesCreate")
    public Result<Object> startSpeechesCreate(@RequestBody ChapterVO vo) throws IOException {

        List<SpeechConfig> speechConfigs = chapterService.queryRoleSpeeches(vo.getProject(), vo.getChapterName());

        CompletableFuture.runAsync(() -> {
                    Path processFlag = pathConfig.getProcessFlagPath(vo.getProject(), vo.getChapterName());
                    if (Files.notExists(processFlag)) {
                        try {
                            Files.createFile(processFlag);
                            int a = 0;
                            for (SpeechConfig speechConfig : speechConfigs) {
                                try {
                                    createAudio(vo.getProject(), vo.getChapterName(), speechConfig);
                                } catch (Exception e) {
                                    log.error(e.getMessage(), e);
                                    throw new RuntimeException(e);
                                }
                                a++;
                            }
                            pathConfig.writeSpeechConfigs(vo.getProject(), vo.getChapterName(), speechConfigs);
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
                    }
                }, Executors.newFixedThreadPool(1))
                .exceptionally(e -> {
                    log.error(e.getMessage(), e);
                    return null;
                });
        return Result.success(speechConfigs);
    }


    public SpeechConfig createAudio(String project, String chapter, SpeechConfig speechConfig) throws IOException {

        String group = speechConfig.getGroup();
        String model = speechConfig.getName();

        String moodPath = group + File.separator + model + File.separator + speechConfig.getMood();
        String defaultMoodPath = group + File.separator + model + File.separator + "默认";
        if (Files.exists(Path.of(pathConfig.getModelSpeechPath() + moodPath))) {
            Files.list(Path.of(pathConfig.getModelSpeechPath() + moodPath)).forEach(path -> {
                if (path.getFileName().toString().endsWith(".wav")) {
                    speechConfig.setPromptAudioPath(pathConfig.getRemoteSpeechPath() + moodPath + "/" + path.getFileName().toString());
                    speechConfig.setPromptText(path.getFileName().toString().replace(".wav", ""));
                }
            });
        } else {
            Files.list(Path.of(pathConfig.getModelSpeechPath() + defaultMoodPath)).forEach(path -> {
                if (path.getFileName().toString().endsWith(".wav")) {
                    speechConfig.setPromptAudioPath(pathConfig.getRemoteSpeechPath() + defaultMoodPath + "/" + path.getFileName().toString());
                    speechConfig.setPromptText(path.getFileName().toString().replace(".wav", ""));
                }
            });
        }

        String speechDir = pathConfig.getChapterPath(project, chapter) + "audio" + File.separator;
        if (Files.notExists(Path.of(speechDir))) {
            Files.createDirectories(Path.of(speechDir));
        }

        long time = new Date().getTime();
        String fileName = speechConfig.getLinesIndex() + "-" + time + ".wav";
        Path wavPath = Path.of(speechDir + fileName);

        long audioDuration = 0;
        if (List.of("。", "……").contains(speechConfig.getLines())) {
            audioDuration = 1000;
            AudioUtils.makeSilenceWav(wavPath.toString(), 1000L);
        } else {

            HashMap<String, String> map = new HashMap<>();
            map.put("refer_wav_path", speechConfig.getPromptAudioPath());
            map.put("prompt_text", speechConfig.getPromptText());
            map.put("prompt_language", "zh");
            map.put("text", speechConfig.getLines());
            map.put("text_language", "zh");
            log.info("音频参数: [{}]", JSON.toJSONString(map));
            log.info("生成音频, role: [{}], model: [{}], mood: [{}], content: [{}], linesIndex: [{}]", speechConfig.getRole(),
                    speechConfig.getName(), speechConfig.getMood(), speechConfig.getLines(), speechConfig.getLinesIndex());

            ResponseEntity<byte[]> response = restTemplate.postForEntity(pathConfig.getGptSoVitsUrl(), map, byte[].class);


            if (response.getStatusCode().is2xxSuccessful()) {

                final ArrayList<Path> paths = new ArrayList<>();
                Files.list(Path.of(speechDir)).forEach(path -> {
                    if (path.getFileName().toString().startsWith(speechConfig.getLinesIndex())) {
                        paths.add(path);
                    }
                });
                if (!CollectionUtils.isEmpty(paths)) {
                    for (Path path : paths) {
                        Files.deleteIfExists(path);
                    }
                }

                Files.write(wavPath, response.getBody());
                log.info("音频生成成功, linesIndex: [{}]", speechConfig.getLinesIndex());

                try {
                    audioDuration = AudioUtils.getAudioTime(wavPath.toAbsolutePath().toString());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        speechConfig.setDuration(audioDuration);
        String filePath = pathConfig.getLinesAudioUrl(project, chapter, fileName);
        speechConfig.setAudioUrl(filePath);
        return speechConfig;
    }

    @PostMapping("createSpeechesConfig")
    public Result<Object> createSpeechesConfig(@RequestBody ChapterVO vo) throws IOException {
        List<SpeechConfig> speechConfigs = chapterService.createRoleSpeechesConfig(vo.getProject(), vo.getChapterName());
        pathConfig.writeSpeechConfigs(vo.getProject(), vo.getChapterName(), speechConfigs);
        return Result.success();
    }

    @PostMapping("querySpeechConfigs")
    public Result<RoleSpeechVO> querySpeechConfigs(@RequestBody ChapterVO vo) throws IOException {
        RoleSpeechVO roleSpeechVO = new RoleSpeechVO();
        Path processFlag = pathConfig.getProcessFlagPath(vo.getProject(), vo.getChapterName());
        List<SpeechConfig> speechConfigs = chapterService.queryRoleSpeeches(vo.getProject(), vo.getChapterName());
        roleSpeechVO.setProcessFlag(Files.exists(processFlag));
        roleSpeechVO.setSpeechConfigs(speechConfigs);
        return Result.success(roleSpeechVO);
    }

    @PostMapping("createSpeech")
    public Result<Object> createSpeech(@RequestBody SpeechCreate speechCreate) throws IOException {
        SpeechConfig newConfig = speechCreate.getSpeechConfig();
        createAudio(speechCreate.getProject(), speechCreate.getChapterName(), newConfig);
        List<SpeechConfig> speechConfigs = chapterService.queryRoleSpeeches(speechCreate.getProject(), speechCreate.getChapterName());

        for (SpeechConfig speechConfig : speechConfigs) {
            if (StringUtils.equals(speechConfig.getLinesIndex(), newConfig.getLinesIndex())) {
                BeanUtils.copyProperties(newConfig, speechConfig);
            }
        }
        pathConfig.writeSpeechConfigs(speechCreate.getProject(), speechCreate.getChapterName(), speechConfigs);
        return Result.success();
    }

    @PostMapping("combineAudio")
    public Result<Object> combineAudio(@RequestBody ChapterVO vo) throws IOException {
        chapterService.combineAudio(vo.getProject(), vo.getChapterName());
        return Result.success();
    }
}
