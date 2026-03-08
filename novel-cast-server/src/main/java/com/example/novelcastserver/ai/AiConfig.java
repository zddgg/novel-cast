package com.example.novelcastserver.ai;

import com.example.novelcastserver.ai.openai.OpenAiService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public AiService openAiService(ChatClient.Builder chatClientBuilder) {
        return new OpenAiService(chatClientBuilder.build());
    }
}
