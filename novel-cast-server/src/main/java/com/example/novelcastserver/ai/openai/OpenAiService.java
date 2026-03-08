package com.example.novelcastserver.ai.openai;

import com.example.novelcastserver.ai.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.Objects;

@Slf4j
public class OpenAiService implements AiService {

    private final ChatClient chatClient;

    public OpenAiService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Flux<String> call(String systemMessage, String userMessage) {
        try {
            String content = chatClient
                    .prompt()
                    .system(systemMessage)
                    .user(userMessage)
                    .call()
                    .content();

            return Flux.just(Objects.requireNonNull(content));
        } catch (Exception e) {
            logError(e);
            return Flux.error(e);
        }
    }

    @Override
    public Flux<String> stream(String systemMessage, String userMessage) {
        return chatClient.prompt()
                .system(systemMessage)
                .user(userMessage)
                .stream()
                .content()
                .doOnNext(System.out::print)
                .doOnError(this::logError);
    }

    /**
     * 统一的错误日志打印方法
     */
    private void logError(Throwable ex) {
        // 5. 针对 WebClient 错误特殊处理，打印响应体
        if (ex instanceof WebClientResponseException wcre) {
            log.error("AI API 调用失败 - 状态码: {}, 响应体: {}",
                    wcre.getStatusCode(),
                    wcre.getResponseBodyAsString(),
                    ex);
        } else {
            log.error("AI 调用发生未知错误", ex);
        }
    }
}
