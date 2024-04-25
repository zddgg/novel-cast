package com.example.novelcastserver.start;

import com.example.novelcastserver.config.PathConfig;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class StartApplication implements ApplicationRunner {

    private final PathConfig pathConfig;

    public StartApplication(PathConfig pathConfig) {
        this.pathConfig = pathConfig;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Files.createDirectories(Path.of(pathConfig.getFileSystemPath() + PathConfig.PROJECT));
        Files.createDirectories(Path.of(pathConfig.getFileSystemPath() + PathConfig.MODELS + File.separator + PathConfig.SPEECH));
        Files.createDirectories(Path.of(pathConfig.getFileSystemPath() + PathConfig.MODELS + File.separator + PathConfig.CONFIG));
    }
}
