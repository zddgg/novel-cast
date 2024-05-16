package com.example.novelcastserver.controller;

import com.alibaba.fastjson2.JSON;
import com.example.novelcastserver.bean.*;
import com.example.novelcastserver.config.PathConfig;
import com.example.novelcastserver.utils.ChapterExtractor;
import com.example.novelcastserver.utils.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.novelcastserver.config.PathConfig.file_projectConfig;

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
                Path chapterDirPath = Path.of(pathConfig.getChapterPath(projectDir));
                int chapterNum = 0;
                if (Files.exists(chapterDirPath)) {
                    chapterNum = (int) Files.list(chapterDirPath).count();
                }
                project.setChapterNum(chapterNum);
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
        Path chapterDirPath = Path.of(pathConfig.getChapterPath(chapterVO.getProject()));
        int chapterNum = 0;
        if (Files.exists(chapterDirPath)) {
            chapterNum = (int) Files.list(chapterDirPath).count();
        }
        projectConfig.setChapterNum(chapterNum);
        return Result.success(projectConfig);
    }

    @PostMapping("createProjectConfig")
    public Result<Object> createProjectConfig(@RequestBody ProjectConfig config) throws IOException {
        String projectConfigPath = pathConfig.getProjectConfigPath(config.getProject());
        Files.createDirectories(Path.of(projectConfigPath));
        Files.write(Path.of(projectConfigPath + "projectConfig.json"), JSON.toJSONBytes(config));
        return Result.success();
    }

    @PostMapping("preCheckProjectConfig")
    public Result<Boolean> preCheckProjectConfig(@RequestBody ChapterVO chapterVO) throws IOException {
        String projectConfigPath = pathConfig.getProjectConfigPath(chapterVO.getProject());
        Path path = Path.of(projectConfigPath + "projectConfig.json");
        return Result.success(Files.exists(path));
    }

    @PostMapping("modifiersTest")
    public Result<Object> modifiersTest(@RequestBody ModifiersTestVO vo) throws IOException {
        List<String> strings = new ArrayList<>();
        if (!CollectionUtils.isEmpty(vo.getLinesModifiers()) && StringUtils.isNotBlank(vo.getTestText())) {
            Pattern pattern = ChapterExtractor.buildModifiersPatternStr(vo.getLinesModifiers());
            Matcher matcher = pattern.matcher(vo.getTestText());
            while (matcher.find()) {
                strings.add(matcher.group());
            }
        }
        return Result.success(strings);
    }

    @PostMapping("splitTmpChapters")
    public Result<Object> splitTmpChapters(@RequestBody SplitChapterVO vo) throws IOException {
        String project = vo.getProject();

        List<String> strings = splitChapters(project, vo.getChapterTitlePattern(), true);

        return Result.success(strings);
    }

    private List<String> splitChapters(String project, String chapterTitlePattern, boolean save) throws IOException {
        String backupFilePath = pathConfig.getProjectConfigPath(project) + "原文.txt";

        List<String> chapterTitles = new ArrayList<>();

        if (Files.exists(Path.of(backupFilePath))) {

            Path chapterDirPath = Path.of(pathConfig.getChapterPath(project));
            FileUtils.deleteDirectoryAll(chapterDirPath);
            Files.createDirectories(chapterDirPath);

            List<ChapterParse> chapterParses = ChapterExtractor.extractor(backupFilePath, chapterTitlePattern);

            for (int i = 0; i < chapterParses.size(); i++) {
                ChapterParse chapterParse = chapterParses.get(i);

                if (save) {
                    ChapterInfo chapterInfo = new ChapterInfo();
                    chapterInfo.setIndex(i);
                    chapterInfo.setTitle(chapterParse.getTitle());
                    chapterInfo.setPrologue(chapterParse.getPrologue());

                    String chapterName = chapterInfo.getIndex() + "-" + chapterInfo.getTitle();
                    Path chapterPath = Path.of(pathConfig.getChapterPath(project, chapterName));
                    if (Files.notExists(chapterPath)) {
                        Files.createDirectories(chapterPath);
                    }

                    Path originTextPath = Path.of(pathConfig.getOriginTextPath(project, chapterName));
                    Files.writeString(originTextPath, chapterParse.getContent());

                    Path chapterInfoPath = Path.of(pathConfig.getChapterPath(project, chapterName) + "chapterInfo.json");
                    Files.writeString(chapterInfoPath, JSON.toJSONString(chapterInfo));

                    Path chapterInfoBkPath = Path.of(pathConfig.getChapterPath(project, chapterName) + "chapterInfo.json.bk");
                    Files.writeString(chapterInfoBkPath, JSON.toJSONString(chapterInfo));
                }
                chapterTitles.add(chapterParse.getTitle());
            }
        }

        return chapterTitles;
    }

    @PostMapping("deleteProject")
    public Result<Object> deleteProject(@RequestBody ChapterVO chapterVO) throws IOException {
        Path projectPath = Path.of(pathConfig.getProjectPath(chapterVO.getProject()));
        if (Files.exists(projectPath) && Files.isDirectory(projectPath)) {
            FileUtils.deleteDirectoryAll(projectPath);
        }
        return Result.success();
    }

    @PostMapping("loadProjectRoleModel")
    public Result<Object> loadProjectRoleModel(@RequestBody LoadProjectRoleModel vo) throws IOException {
        List<ProjectConfig.ProjectRoleConfig> roleConfigs = new ArrayList<>();

        ProjectConfig projectConfig = pathConfig.getProjectConfig(vo.getProject());
        if (Objects.nonNull(projectConfig)
                && !CollectionUtils.isEmpty(projectConfig.getRoleConfigs())
                && !CollectionUtils.isEmpty(vo.getRoles())) {
            roleConfigs = projectConfig.getRoleConfigs()
                    .stream()
                    .filter(projectRoleConfig ->
                            vo.getRoles().contains(projectRoleConfig.getRole())).toList();
        }
        return Result.success(roleConfigs);
    }

    @PostMapping("checkProjectRoleModel")
    public Result<Object> checkProjectRoleModel(@RequestBody ProjectConfig.ProjectRoleConfig roleConfig) throws IOException {
        ProjectConfig projectConfig = pathConfig.getProjectConfig(roleConfig.getProject());
        List<ProjectConfig.ProjectRoleConfig> roleConfigs = projectConfig.getRoleConfigs();
        List<String> roleList = roleConfigs.stream().map(ProjectConfig.ProjectRoleConfig::getRole).toList();
        if (roleList.contains(roleConfig.getRole())) {
            for (ProjectConfig.ProjectRoleConfig config : roleConfigs) {
                if (config.getRole().equals(roleConfig.getRole())) {
                    return Result.success(false);
                }
            }
        } else {
            roleConfigs.add(roleConfig);
        }
        Files.write(Path.of(pathConfig.getProjectConfigPath(roleConfig.getProject())), JSON.toJSONBytes(projectConfig));
        return Result.success(true);
    }

    @PostMapping("setProjectRoleModel")
    public Result<Object> setProjectRoleModel(@RequestBody ProjectConfig.ProjectRoleConfig roleConfig) throws IOException {
        ProjectConfig projectConfig = pathConfig.getProjectConfig(roleConfig.getProject());
        List<ProjectConfig.ProjectRoleConfig> roleConfigs = projectConfig.getRoleConfigs();
        List<String> roleList = roleConfigs.stream().map(ProjectConfig.ProjectRoleConfig::getRole).toList();
        if (roleList.contains(roleConfig.getRole())) {
            for (ProjectConfig.ProjectRoleConfig config : roleConfigs) {
                if (config.getRole().equals(roleConfig.getRole())) {
                    config.setGsvModel(roleConfig.getGsvModel());
                    config.setModel(roleConfig.getModel());
                    config.setMood(roleConfig.getMood());
                }
            }
        } else {
            roleConfigs.add(roleConfig);
        }
        Files.write(Path.of(pathConfig.getProjectConfigPath(roleConfig.getProject()) + file_projectConfig),
                JSON.toJSONBytes(projectConfig));
        return Result.success();
    }
}
