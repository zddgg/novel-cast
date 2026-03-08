package com.example.novelcastserver.ai.openai;

import com.example.novelcastserver.ai.AiService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

public class OpenAiService implements AiService {

    private final ChatClient chatClient;

    public OpenAiService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Flux<String> call(String systemMessage, String userMessage) {
        Prompt prompt = new Prompt(List.of(new SystemMessage(systemMessage), new SystemMessage(userMessage)));
        return Flux.just(Objects.requireNonNull(chatClient.prompt(prompt).call().content()));
    }

    @Override
    public Flux<String> stream(String systemMessage, String userMessage) {
        Prompt prompt = new Prompt(List.of(new SystemMessage(systemMessage), new SystemMessage(userMessage)));
        return chatClient.prompt(prompt)
                .stream()
                .content()
                .doOnNext(System.out::print);
    }
}
