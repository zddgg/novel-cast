package com.example.novelcastserver.controller;

import com.alibaba.fastjson.JSON;
import com.example.novelcastserver.config.PathConfig;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zhipu.oapi.demo.V4OkHttpClientTest.mapStreamToAccumulator;

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

    /**
     * sse调用
     */
    private static void testSseInvoke() {
        ClientV4 client = new ClientV4.Builder("15e3a6f6ba770126ba28277b1962fb99.3nocinvpeD0x2yXw").build();

        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), "作为一名营销专家，请为智谱开放平台创作一个吸引人的slogan");
        messages.add(chatMessage);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.TRUE)
                .messages(messages)
                .build();
        ModelApiResponse sseModelApiResp = client.invokeModelApi(chatCompletionRequest);
        if (sseModelApiResp.isSuccess()) {
            AtomicBoolean isFirst = new AtomicBoolean(true);
            ChatMessageAccumulator chatMessageAccumulator = mapStreamToAccumulator(sseModelApiResp.getFlowable())
                    .doOnNext(accumulator -> {
                        {
                            if (isFirst.getAndSet(false)) {
                                System.out.print("Response: ");
                            }
                            if (accumulator.getDelta() != null && accumulator.getDelta().getTool_calls() != null) {
                                String jsonString = JSON.toJSONString(accumulator.getDelta().getTool_calls());
                                System.out.println("tool_calls: " + jsonString);
                            }
                            if (accumulator.getDelta() != null && accumulator.getDelta().getContent() != null) {
                                System.out.print(accumulator.getDelta().getContent());
                            }
                        }
                    })
                    .doOnComplete(System.out::println)
                    .lastElement()
                    .blockingGet();

            Choice choice = new Choice(chatMessageAccumulator.getChoice().getFinishReason(), 0L, chatMessageAccumulator.getDelta());
            List<Choice> choices = new ArrayList<>();
            choices.add(choice);
            ModelData data = new ModelData();
            data.setChoices(choices);
            data.setUsage(chatMessageAccumulator.getUsage());
            data.setId(chatMessageAccumulator.getId());
            data.setCreated(chatMessageAccumulator.getCreated());
            data.setRequestId(chatCompletionRequest.getRequestId());
            sseModelApiResp.setFlowable(null);
            sseModelApiResp.setData(data);
        }
        System.out.println("model output:" + JSON.toJSONString(sseModelApiResp));
    }

    public static void main(String[] args) {
        testSseInvoke();
    }
}
