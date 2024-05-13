package com.example.novelcastserver.ai;

import com.example.novelcastserver.ai.glm.GlmAiService;
import com.example.novelcastserver.ai.openai.OpenAiService;
import com.example.novelcastserver.ai.qwen.QwenAiService;
import com.example.novelcastserver.ai.spark.SparkAiService;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;

@Configuration
public class AiConfig {

    @Bean
    @ConditionalOnProperty(prefix = "ai", name = {"type"}, havingValue = "openai", matchIfMissing = true)
    public AiService openAiService(OpenAiChatClient chatClient) {
        return new OpenAiService(chatClient);
    }

    @Bean
    @ConditionalOnProperty(prefix = "ai", name = {"type"}, havingValue = "qwen")
    public AiService qwenAiService() {
        return new QwenAiService();
    }

    @Bean
    @ConditionalOnProperty(prefix = "ai", name = {"type"}, havingValue = "spark")
    public AiService sparkAiService() {
        return new SparkAiService(new ReactorNettyWebSocketClient());
    }

    @Bean
    @ConditionalOnProperty(prefix = "ai", name = {"type"}, havingValue = "glm")
    public AiService glmAiService() {
        WebClient webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        return new GlmAiService(webClient);
    }
}
