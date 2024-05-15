package com.example.novelcastserver.v2.service.impl;

import com.alibaba.fastjson2.JSON;
import com.example.novelcastserver.config.PathConfig;
import com.example.novelcastserver.utils.ForEach;
import com.example.novelcastserver.v2.service.ModelService;
import com.example.novelcastserver.v2.vo.Group;
import com.example.novelcastserver.v2.vo.GsvModel;
import com.example.novelcastserver.v2.vo.GsvTimbre;
import com.example.novelcastserver.v2.vo.ModelConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ModelServiceImpl implements ModelService {

    private final PathConfig pathConfig;

    public ModelServiceImpl(PathConfig pathConfig) {
        this.pathConfig = pathConfig;
    }

    @Override
    public ModelConfig syncModelConfig() throws IOException {
        Path modelConfigPath = Path.of(pathConfig.getModelPath() + PathConfig.MODEL_CONFIG);
        ModelConfig modelConfig;
        if (Files.notExists(modelConfigPath) || StringUtils.isBlank(Files.readString(modelConfigPath))) {
            modelConfig = buildModelConfig();
        } else {
            ModelConfig newModelConfig = buildModelConfig();
            modelConfig = JSON.parseObject(Files.readString(modelConfigPath), ModelConfig.class);
            modelConfig = mergeModelConfig(newModelConfig, modelConfig);
        }
        Files.write(modelConfigPath, JSON.toJSONBytes(modelConfig));
        return modelConfig;
    }

    @Override
    public List<GsvModel> getAllGsvModels() {
        Path gsvAudioPath = Path.of(pathConfig.getModelPath() + PathConfig.GPT_SoVITS_AUDIO);
        return List.of();
    }

    public ModelConfig buildModelConfig() throws IOException {
        Path gsvModelPath = Path.of(pathConfig.getModelPath(), PathConfig.GPT_SoVITS_MODEL);
        List<Group> gsvModelGroups = new ArrayList<>();
        List<GsvModel> gsvModels = new ArrayList<>();
        if (Files.exists(gsvModelPath)) {
            ForEach.forEach(Files.list(gsvModelPath), (groupIndex, group) -> {
                gsvModelGroups.add(new Group(groupIndex, group.getFileName().toString()));
                if (Files.exists(group)) {
                    try {
                        ForEach.forEach(Files.list(group), (modelIndex, model) -> {
                            GsvModel gsvModel = new GsvModel();
                            gsvModel.setId(modelIndex);
                            gsvModel.setName(model.getFileName().toString());
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

        Path gsvAudioPath = Path.of(pathConfig.getModelPath(), PathConfig.GPT_SoVITS_AUDIO);
        List<Group> gsvAudioGroups = new ArrayList<>();
        List<GsvTimbre> gsvTimbres = new ArrayList<>();
        if (Files.exists(gsvAudioPath)) {
            ForEach.forEach(Files.list(gsvAudioPath), (groupIndex, group) -> {
                gsvAudioGroups.add(new Group(groupIndex, group.getFileName().toString()));
                if (Files.exists(group)) {
                    try {
                        ForEach.forEach(Files.list(group), (modelIndex, timbre) -> {
                            GsvTimbre gsvTimbre = new GsvTimbre();
                            gsvTimbre.setId(modelIndex);
                            gsvTimbre.setName(timbre.getFileName().toString());
                            gsvTimbre.setGroup(group.getFileName().toString());
                            List<GsvTimbre.Mood> moods = new ArrayList<>();
                            if (Files.exists(timbre)) {
                                try {
                                    ForEach.forEach(Files.list(timbre), (moodIndex, timbreMood) -> {
                                        if (Files.exists(timbreMood)) {
                                            GsvTimbre.Mood mood = new GsvTimbre.Mood();
                                            mood.setId(moodIndex);
                                            mood.setName(timbreMood.getFileName().toString());
                                            List<GsvTimbre.Audio> audios = new ArrayList<>();
                                            if (Files.exists(timbreMood)) {
                                                try {
                                                    ForEach.forEach(Files.list(timbreMood), (audioIndex, moodAudio) -> {
                                                        GsvTimbre.Audio audio = new GsvTimbre.Audio();
                                                        audio.setId(audioIndex);
                                                        audio.setName(moodAudio.getFileName().toString());
                                                        audio.setText(moodAudio.toFile().getName());
                                                        audios.add(audio);
                                                    });
                                                } catch (IOException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }
                                            mood.setAudios(audios);
                                            moods.add(mood);
                                        }
                                    });
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            gsvTimbre.setMoods(moods);
                            gsvTimbres.add(gsvTimbre);
                        });
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        ModelConfig modelConfig = new ModelConfig();
        modelConfig.setGsvModelGroups(gsvModelGroups);
        modelConfig.setGsvModels(gsvModels);
        modelConfig.setGsvTimbreGroups(gsvAudioGroups);
        modelConfig.setGsvTimbres(gsvTimbres);
        return modelConfig;
    }

    // 按照自定义顺序排序
    public ModelConfig mergeModelConfig(ModelConfig newModelConfig, ModelConfig oldModelConfig) {
        Map<String, Integer> oldGsvModelGroupIdMap = oldModelConfig.getGsvModelGroups()
                .stream()
                .collect(Collectors.toMap(Group::getName, Group::getId));

        List<Group> newGsvModelGroups = new ArrayList<>();
        ForEach.forEach(newModelConfig.getGsvModelGroups()
                        .stream()
                        .sorted(Comparator.comparingInt(v -> oldGsvModelGroupIdMap.getOrDefault(v.getName(), Integer.MAX_VALUE))),
                (index, item) -> {
                    item.setId(index);
                    newGsvModelGroups.add(item);
                });


        Map<String, Integer> oldGsvModelIdMap = oldModelConfig.getGsvModels()
                .stream()
                .collect(Collectors.toMap(GsvModel::getName, GsvModel::getId));

        List<GsvModel> newGsvModels = new ArrayList<>();
        ForEach.forEach(newModelConfig.getGsvModels()
                        .stream()
                        .sorted(Comparator.comparingInt(v -> oldGsvModelIdMap.getOrDefault(v.getName(), Integer.MAX_VALUE))),
                (index, item) -> {
                    item.setId(index);
                    newGsvModels.add(item);
                });


        Map<String, Integer> oldGsvAudioGroupIdMap = oldModelConfig.getGsvTimbreGroups()
                .stream()
                .collect(Collectors.toMap(Group::getName, Group::getId));

        List<Group> newGsvTimbreGroups = new ArrayList<>();
        ForEach.forEach(newModelConfig.getGsvTimbreGroups()
                        .stream()
                        .sorted(Comparator.comparingInt(v -> oldGsvAudioGroupIdMap.getOrDefault(v.getName(), Integer.MAX_VALUE))),
                (index, item) -> {
                    item.setId(index);
                    newGsvTimbreGroups.add(item);
                });


        Map<String, Integer> oldGsvTimbreIdMap = oldModelConfig.getGsvTimbreGroups()
                .stream()
                .collect(Collectors.toMap(Group::getName, Group::getId));

        List<GsvTimbre> newGsvTimbres = new ArrayList<>();
        ForEach.forEach(newModelConfig.getGsvTimbres()
                        .stream()
                        .sorted(Comparator.comparingInt(v -> oldGsvTimbreIdMap.getOrDefault(v.getName(), Integer.MAX_VALUE))),
                (index, item) -> {
                    item.setId(index);
                    newGsvTimbres.add(item);
                });

        return new ModelConfig(newGsvModelGroups, newGsvModels, newGsvTimbreGroups, newGsvTimbres);
    }
}
