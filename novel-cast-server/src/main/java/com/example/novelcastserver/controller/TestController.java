package com.example.novelcastserver.controller;

import com.example.novelcastserver.config.PathConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
public class TestController {

    private final PathConfig pathConfig;

    public TestController(PathConfig pathConfig) {
        this.pathConfig = pathConfig;
    }

    @GetMapping("test")
    public Flux<String> test() {
        String longString = """
                这是一个非常长的字符串，我们希望每秒输出10个字符。这是一个非常长的字符串，我们希望每秒输出10个字符。这是一个非常长的字符串，我们希望每秒输出10个字符。
                这是一个非常长的字符串，我们希望每秒输出10个字符。这是一个非常长的字符串，我们希望每秒输出10个字符。这是一个非常长的字符串，我们希望每秒输出10个字符。
                这是一个非常长的字符串，我们希望每秒输出10个字符。这是一个非常长的字符串，我们希望每秒输出10个字符。这是一个非常长的字符串，我们希望每秒输出10个字符。
                """;
        int charactersPerSecond = 10;

        return Flux.interval(Duration.ofMillis(1000))
                .map(i -> longString.substring((int) Math.min((i * charactersPerSecond), longString.length()), (int) Math.min((i + 1) * charactersPerSecond, longString.length())))
                .takeWhile(s -> !s.isEmpty())
                .doOnNext(System.out::println); // 打印每个输出片段，仅用于演示
    }
}
