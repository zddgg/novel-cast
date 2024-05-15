package com.example.novelcastserver.v2.service;

import com.example.novelcastserver.v2.vo.GsvModel;
import com.example.novelcastserver.v2.vo.ModelConfig;

import java.io.IOException;
import java.util.List;

public interface ModelService {

    ModelConfig syncModelConfig() throws IOException;

    List<GsvModel> getAllGsvModels();
}
