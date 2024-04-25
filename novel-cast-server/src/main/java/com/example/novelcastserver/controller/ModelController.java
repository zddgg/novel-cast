package com.example.novelcastserver.controller;

import com.alibaba.fastjson2.JSON;
import com.example.novelcastserver.bean.*;
import com.example.novelcastserver.config.PathConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/model")
public class ModelController {

    private final PathConfig pathConfig;

    public ModelController(PathConfig pathConfig) {
        this.pathConfig = pathConfig;
    }

    @PostMapping("speechModels")
    public Result<List<SpeechModelGroup>> speechModels() throws IOException {
        String baseUrl = pathConfig.getModelSpeechUrlBase();
        Path modelPath = Path.of(pathConfig.getModelSpeechPath());
        List<SpeechModelGroup> speechModelGroups = new ArrayList<>();
        if (Files.exists(modelPath)) {
            AtomicInteger indexes = new AtomicInteger(0);
            speechModelGroups = Files.list(modelPath)
                    .map(path -> {
                        try {
                            SpeechModelGroup speechModelGroup = new SpeechModelGroup();
                            speechModelGroup.setIndex(indexes.incrementAndGet());
                            speechModelGroup.setGroup(path.getFileName().toString());

                            List<SpeechModel> speechModels = Files.list(path)
                                    .map(path1 -> {
                                        try {
                                            SpeechModel speechModel = new SpeechModel();
                                            speechModel.setName(path1.getFileName().toString());

                                            Path speechMarkedPath = Path.of(pathConfig.getModelMarkedJsonPath());
                                            if (Files.exists(speechMarkedPath)) {
                                                String speechMarkedStr = Files.readString(speechMarkedPath);
                                                List<SpeechModelMarked> speechModelMarkeds = JSON.parseArray(StringUtils.isBlank(speechMarkedStr) ? "[]" : speechMarkedStr, SpeechModelMarked.class);
                                                Map<String, SpeechModelMarked> speechModelMarkedMap = speechModelMarkeds.stream()
                                                        .collect(Collectors.toMap(
                                                                speechModelMarked -> speechModelMarked.getGroup() + "-" + speechModelMarked.getName(),
                                                                Function.identity(),
                                                                (v1, v2) -> v1
                                                        ));
                                                String key = path.getFileName().toString() + "-" + path1.getFileName().toString();
                                                if (speechModelMarkedMap.containsKey(key)) {
                                                    SpeechModelMarked speechModelMarked = speechModelMarkedMap.get(key);
                                                    speechModel.setGender(speechModelMarked.getGender());
                                                    speechModel.setAgeGroup(speechModelMarked.getAgeGroup());
                                                }
                                            }

                                            List<Mood> moods = Files.list(path1)
                                                    .filter(Files::isDirectory)
                                                    .map(path2 -> {
                                                        try {
                                                            Mood mood = new Mood();
                                                            mood.setName(path2.getFileName().toString());
                                                            Files.list(path2).filter(path3 -> path3.getFileName().toString().endsWith(".wav")).forEach(path3 -> {
                                                                mood.setUrl(baseUrl + path.getFileName().toString()
                                                                        + "/" + path1.getFileName().toString()
                                                                        + "/" + path2.getFileName().toString()
                                                                        + "/" + path3.getFileName());
                                                                mood.setText(path3.getFileName().toString().replace(".wav", ""));
                                                            });
                                                            return mood;
                                                        } catch (IOException e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                    }).sorted(Comparator.comparing(m -> !StringUtils.equals(m.getName(), "默认"))).toList();
                                            speechModel.setMoods(moods);
                                            return speechModel;
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                                    .toList();

                            speechModelGroup.setSpeechModels(speechModels);
                            return speechModelGroup;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        }
        return Result.success(speechModelGroups);
    }

    @PostMapping("speechMarked")
    public Result<Object> speechMarked(@RequestBody SpeechModelMarked req) throws IOException {
        Path speechMarkedPath = Path.of(pathConfig.getModelMarkedJsonPath());
        List<SpeechModelMarked> speechModelMarkeds = new ArrayList<>();
        if (Files.exists(speechMarkedPath)) {
            String speechMarkedStr = Files.readString(speechMarkedPath);
            speechModelMarkeds = JSON.parseArray(StringUtils.isBlank(speechMarkedStr) ? "[]" : speechMarkedStr, SpeechModelMarked.class);
            Map<String, SpeechModelMarked> speechModelMarkedMap = speechModelMarkeds.stream()
                    .collect(Collectors.toMap(
                            speechModelMarked -> speechModelMarked.getGroup() + "-" + speechModelMarked.getName(),
                            Function.identity(),
                            (v1, v2) -> v1
                    ));
            speechModelMarkedMap.put(req.getGroup() + "-" + req.getName(), req);
            speechModelMarkeds = speechModelMarkedMap.values().stream().toList();
        } else {
            speechModelMarkeds.add(req);
        }
        Files.write(speechMarkedPath, JSON.toJSONString(speechModelMarkeds).getBytes());
        return Result.success();
    }

}
