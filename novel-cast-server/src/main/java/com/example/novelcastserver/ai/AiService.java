package com.example.novelcastserver.ai;

import reactor.core.publisher.Flux;

public interface AiService {

    Flux<String> call(String systemMessage, String userMessage);

    Flux<String> stream(String systemMessage, String userMessage);
}
