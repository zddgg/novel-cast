package com.example.novelcastserver.controller;

import com.alibaba.fastjson2.JSON;
import com.example.novelcastserver.bean.*;
import com.example.novelcastserver.config.PathConfig;
import com.example.novelcastserver.utils.ChapterExtractor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/project")
public class ProjectController {

    private final PathConfig pathConfig;

    public ProjectController(PathConfig pathConfig) {
        this.pathConfig = pathConfig;
    }

    @PostMapping("list")
    public Result<List<Project>> list() throws IOException {
        Path projectPath = Path.of(pathConfig.getProjectPath());
        if (Files.notExists(projectPath)) {
            Files.createDirectory(projectPath);
        }
        List<Project> list = Files.list(projectPath).map(path -> {
            Project project = new Project();
            String projectDir = path.getFileName().toString();
            project.setProjectName(projectDir);
            try {
                project.setChapterNum((int) Files.list(Path.of(pathConfig.getChapterPath(projectDir))).count());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            project.setProcessState("处理中");
            project.setStatus("online");
            return project;
        }).toList();

        return Result.success(list);
    }

    @PostMapping("createProject")
    public Result<Object> createProject(@RequestParam("project") String project,
                                        @RequestParam("file") MultipartFile file) throws IOException {

        String projectPath = pathConfig.getProjectPath(project);
        if (Files.exists(Path.of(projectPath))) {
            return Result.failure("Project已存在");
        }

        String backupFilePath = pathConfig.getProjectConfigPath(project) + "原文.txt";
        Files.createDirectories(Path.of(backupFilePath).getParent());
        Files.write(Path.of(backupFilePath), file.getBytes());

        if (Files.notExists(Path.of(projectPath))) {
            Files.createDirectory(Path.of(projectPath));
        }

        List<ChapterParse> chapterParses = ChapterExtractor.extractor(backupFilePath);
        for (int i = 0; i < chapterParses.size(); i++) {
            ChapterParse chapterParse = chapterParses.get(i);
            ChapterInfo chapterInfo = new ChapterInfo();
            chapterInfo.setIndex(i);
            chapterInfo.setTitle(chapterParse.getTitle());
            chapterInfo.setPrologue(chapterParse.getPrologue());
            chapterInfo.setLineInfos(ChapterExtractor.parseChapterInfo(chapterParse.getContent()));

            String chapterName = chapterInfo.getIndex() + "-" + chapterInfo.getTitle();
            Path chapterPath = Path.of(pathConfig.getChapterPath(project, chapterName));
            if (Files.notExists(chapterPath)) {
                Files.createDirectories(chapterPath);
            }

            Path originTextPath = Path.of(pathConfig.getOriginTextPath(project, chapterName));
            Files.createFile(originTextPath);
            Files.writeString(originTextPath, chapterParse.getContent());

            Path chapterInfoPath = Path.of(pathConfig.getChapterPath(project, chapterName) + "chapterInfo.json");
            Files.writeString(chapterInfoPath, JSON.toJSONString(chapterInfo));

        }
        return Result.success();
    }

    @PostMapping("queryProjectConfig")
    public Result<Object> queryProjectConfig(@RequestBody ChapterVO chapterVO) throws IOException {
        String projectConfigPath = pathConfig.getProjectConfigPath(chapterVO.getProject());
        Path path = Path.of(projectConfigPath + "projectConfig.json");
        ProjectConfig projectConfig = new ProjectConfig();
        if (Files.exists(path)) {
            projectConfig = JSON.parseObject(Files.readString(Path.of(projectConfigPath + "projectConfig.json")), ProjectConfig.class);
        }
        return Result.success(projectConfig);
    }

    @PostMapping("createProjectConfig")
    public Result<Object> createProjectConfig(@RequestBody ProjectConfig configs) throws IOException {
        String projectConfigPath = pathConfig.getProjectConfigPath(configs.getProject());
        Files.createDirectories(Path.of(projectConfigPath));
        configs.setProject(null);
        Files.write(Path.of(projectConfigPath + "projectConfig.json"), JSON.toJSONBytes(configs));
        return Result.success();
    }


    @PostMapping("preCheckProjectConfig")
    public Result<Boolean> preCheckProjectConfig(@RequestBody ChapterVO chapterVO) throws IOException {
        String projectConfigPath = pathConfig.getProjectConfigPath(chapterVO.getProject());
        Path path = Path.of(projectConfigPath + "projectConfig.json");
        return Result.success(Files.exists(path));
    }
}
