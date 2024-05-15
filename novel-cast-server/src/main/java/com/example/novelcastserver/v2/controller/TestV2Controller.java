package com.example.novelcastserver.v2.controller;

import com.example.novelcastserver.v2.service.ModelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class TestV2Controller {

    private final ModelService modelService;

    public TestV2Controller(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping("v2t")
    public void v2t() throws IOException {
        System.out.println(modelService.syncModelConfig());
    }
}
