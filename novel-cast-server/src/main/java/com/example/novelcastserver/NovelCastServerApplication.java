package com.example.novelcastserver;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class NovelCastServerApplication {

    public static void main(String[] args) {
        Loader.load(opencv_java.class);
        ConfigurableApplicationContext context = SpringApplication.run(NovelCastServerApplication.class, args);
        System.out.println();
        System.out.println(STR."启动成功: http://127.0.0.1:\{context.getEnvironment().getProperty("server.port")}");
    }
}
