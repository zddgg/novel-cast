package com.example.novelcastserver.config;

import com.alibaba.fastjson2.JSON;
import com.example.novelcastserver.bean.*;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Data
@Configuration
@ConfigurationProperties(prefix = "novel-cast")
public class PathConfig {

    @Value("${user.dir}")
    private String userDir;

    @Autowired
    private Environment env;

    public static final String CONFIG = "配置";
    public static final String PROJECT = "项目";
    public static final String CHAPTER = "章节";
    public static final String MODELS = "模型";
    public static final String SPEECH = "语音";

    public static final String file_projectConfig = "projectConfig.json";

    public static final String file_chapterInfo = "chapterInfo.json";
    public static final String file_aiResult = "aiResult.json";
    public static final String file_aiIgnore = "aiIgnore.signal";
    public static final String file_roles = "roles.json";
    public static final String file_linesMappings = "linesMappings.json";
    public static final String file_modelConfig = "modelConfig.json";
    public static final String file_speechConfig = "speechConfig.json";
    public static final String file_processFlag = "processFlag.signal";
    public static final String dir_audio = "audio";
    public static final String file_output = "output.wav";
    public static final String file_chapterConfig = "chapterConfig.json";

    public static final String file_speechMarked = "speechMarked.json";

    private String fileSystemPath;
    private String fileSystemUrl;
    private String gptSoVitsUrl;
    private String remoteSpeechPath;

    @PostConstruct
    public void init() {
        String absPath = new File(userDir).getAbsolutePath();
        if (StringUtils.isBlank(fileSystemPath)) {
            fileSystemPath = absPath + File.separator + "novelCast" + File.separator;
        }
        fileSystemUrl = "http://localhost:" + env.getProperty("server.port") + "/files/";
        if (StringUtils.isBlank(remoteSpeechPath)) {
            remoteSpeechPath = getModelSpeechPath();
        }
    }

    public String getProjectPath() {
        return STR."\{this.fileSystemPath}\{PROJECT}\{File.separator}";
    }

    public String getProjectPath(String project) {
        return STR."\{this.fileSystemPath}\{PROJECT}\{File.separator}\{project}\{File.separator}";
    }

    public String getProjectConfigPath(String project) {
        return STR."\{getProjectPath(project)}\{CONFIG}\{File.separator}";
    }

    public String getChapterPath(String project) {
        return STR."\{getProjectPath(project)}\{CHAPTER}\{File.separator}";
    }

    public String getChapterPath(String project, String chapterName) {
        return getChapterPath(project) + chapterName + File.separator;
    }

    public String getOriginTextPath(String project, String chapterName) {
        return STR."\{getChapterPath(project, chapterName)}原文.txt";
    }

    public String getAiResultFilePath(String project, String chapterName) {
        return getChapterPath(project, chapterName) + file_aiResult;
    }

    public String getAiIgnoreFilePath(String project, String chapterName) {
        return getChapterPath(project, chapterName) + file_aiIgnore;
    }

    public String getSpeechConfigFilePath(String project, String chapter) {
        return getChapterPath(project, chapter) + file_speechConfig;
    }

    public String getRolesFilePath(String project, String chapterName) {
        return getChapterPath(project, chapterName) + file_roles;
    }

    public String getLinesMappingsFilePath(String project, String chapterName) {
        return getChapterPath(project, chapterName) + file_linesMappings;
    }

    public String getModelConfigFilePath(String project, String chapterName) {
        return getChapterPath(project, chapterName) + file_modelConfig;
    }

    public Path getLinesAudioDirPath(String project, String chapter) {
        return Path.of(getChapterPath(project, chapter) + dir_audio);
    }

    public String getOutAudioPath(String project, String chapter) {
        return getChapterPath(project, chapter) + file_output;
    }

    public String getModelPath() {
        return STR."\{this.fileSystemPath}\{MODELS}\{File.separator}";
    }

    public String getRemoteSpeechPath() {
        if (StringUtils.isEmpty(remoteSpeechPath)) {
            return getModelSpeechPath();
        }
        return this.remoteSpeechPath;
    }

    public String getModelSpeechPath() {
        return STR."\{this.fileSystemPath}\{MODELS}\{File.separator}\{SPEECH}\{File.separator}";
    }

    public String getModelMarkedJsonPath() {
        return STR."\{getModelPath()}\{CONFIG}\{File.separator}\{file_speechMarked}";
    }

    public String getModelSpeechUrlBase() {
        return this.fileSystemUrl + MODELS + "/" + SPEECH + "/";
    }

    public String getLinesAudioUrl(String project, String chapterName, String fileName) {
        return this.fileSystemUrl + PROJECT + "/" + project + "/" + CHAPTER + "/" + chapterName + "/audio/" + fileName;
    }

    public String getOutAudioUrl(String project, String chapterName, String fileName) {
        return this.fileSystemUrl + PROJECT + "/" + project + "/" + CHAPTER + "/" + chapterName + "/" + fileName;
    }

    public ChapterInfo getChapterInfo(String project, String chapter) throws IOException {
        Path path = Path.of(getChapterPath(project, chapter) + file_chapterInfo);
        return JSON.parseObject(Optional.ofNullable(Files.readString(path)).orElse("{}"), ChapterInfo.class);
    }

    public String getChapterInfoPath(String project, String chapter) throws IOException {
        return getChapterPath(project, chapter) + file_chapterInfo;
    }

    public ModelConfig getModelConfig(String project, String chapter) throws IOException {
        Path path = Path.of(getChapterPath(project, chapter) + file_modelConfig);
        return JSON.parseObject(Optional.ofNullable(Files.readString(path)).orElse("{}"), ModelConfig.class);
    }

    public SpeechConfig getSpeechConfig(String project, String chapter) throws IOException {
        Path path = Path.of(getChapterPath(project, chapter) + file_speechConfig);
        if (Files.notExists(path)) {
            return null;
        }
        return JSON.parseObject(Files.readString(path), SpeechConfig.class);
    }

    public void writeSpeechConfig(String project, String chapter, SpeechConfig speechConfig) throws IOException {
        Path path = Path.of(getChapterPath(project, chapter) + file_speechConfig);
        Files.write(path, JSON.toJSONBytes(speechConfig));
    }

    public ProjectConfig getProjectConfig(String project) throws IOException {
        Path path = Path.of(getProjectConfigPath(project) + file_projectConfig);
        return JSON.parseObject(Optional.ofNullable(Files.readString(path)).orElse("{}"), ProjectConfig.class);
    }

    public Path getProcessFlagPath(String project, String chapter) {
        return Path.of(getChapterPath(project, chapter) + file_processFlag);
    }
}
