package com.example.novelcastserver.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${user.dir}")
    private String userDir;

    @Autowired
    private PathConfig pathConfig;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/webui/**")
                .setViewName("forward:/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String novelCastPath = new File(userDir, "novelCast").getAbsolutePath();
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + novelCastPath + "/");
    }
}