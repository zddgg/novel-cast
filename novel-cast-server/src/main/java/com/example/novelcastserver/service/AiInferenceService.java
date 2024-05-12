package com.example.novelcastserver.service;

import com.alibaba.fastjson2.JSON;
import com.example.novelcastserver.bean.*;
import com.example.novelcastserver.config.PathConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class AiInferenceService {

    private final PathConfig pathConfig;
    private final OpenAiChatClient chatClient;
    private final ChapterService chapterService;

    public AiInferenceService(PathConfig pathConfig, OpenAiChatClient chatClient, ChapterService chapterService) {
        this.pathConfig = pathConfig;
        this.chatClient = chatClient;
        this.chapterService = chapterService;
    }

    public Flux<String> roleAndLinesInference(ChapterInfo chapterInfo) {

//        String longString = AiInferenceService.resTemp;
//        int charactersPerSecond = 80;
//
//        // 打印每个输出片段，仅用于演示
//        return Flux.interval(Duration.ofMillis(200))
//                .map(i -> longString.substring((int) Math.min((i * charactersPerSecond), longString.length()), (int) Math.min((i + 1) * charactersPerSecond, longString.length())))
//                .takeWhile(s -> !s.isEmpty());


        List<Lines> linesList = new ArrayList<>();
        chapterInfo.getLineInfos().forEach(lineInfo -> {
            lineInfo.getSentenceInfos().forEach(sentenceInfo -> {
                if (Objects.equals(sentenceInfo.getLinesFlag(), Boolean.TRUE)
                        && !Objects.equals(sentenceInfo.getLinesDelFlag(), Boolean.TRUE)) {
                    Lines lines = new Lines();
                    lines.setIndex(lineInfo.getIndex() + "-" + sentenceInfo.getIndex());
                    lines.setLines(sentenceInfo.getContent());
                    linesList.add(lines);
                }
            });
        });

        String lines = JSON.toJSONString(linesList);
        StringBuilder sb = new StringBuilder();
        chapterInfo.getLineInfos().forEach(lineInfo -> {
            lineInfo.getSentenceInfos().forEach(sentenceInfo -> {
                sb.append(sentenceInfo.getContent());
            });
            sb.append("\n");
        });
        String content = sb.toString();

        String userQuery = STR."""
                要求如下：
                \{parseStep}

                输出格式如下：
                \{outputFormat}

                台词部分：
                \{lines}

                原文部分：
                \{content}
                """;

        SystemMessage systemMessage = new SystemMessage("你是一个小说内容台词分析员，你会精确的找到台词在原文中的位置并分析属于哪个角色，以及角色在说这句台词时的上下文环境及情绪等。");
        UserMessage userMessage = new UserMessage(userQuery);
        log.info("systemMessage: {}", systemMessage.getContent());
        log.info("userMessage: {}", userMessage.getContent());
        return chatClient.stream(new Prompt(List.of(systemMessage, userMessage)))
                .mapNotNull(chatResponse -> chatResponse.getResult().getOutput().getContent());


//        ChatResponse chatResponse = chatClient.call(new Prompt(List.of(systemMessage, userMessage)));
//        System.out.println(JSON.toJSONString(chatResponse));
//        return Flux.just(chatResponse.getResult().getOutput().getContent());
    }

    public AiResult aiResultFormat(AiResultFormatVO vo) throws IOException {
        String text = vo.getAiResultText();
        AiResult aiResult = parseAiResult(text);
        aiResult = chapterService.reCombineAiResult(vo.getProject(), vo.getChapterName(), aiResult);
        return aiResult;
    }

    public Flux<String> roleAndLinesInference(ChapterVO vo, Boolean saveResult) throws IOException {
        Path chapterInfoPath = Path.of(pathConfig.getChapterPath(vo.getProject(), vo.getChapterName()) + "chapterInfo.json");
        ChapterInfo chapterInfo = JSON.parseObject(Files.readAllBytes(chapterInfoPath), ChapterInfo.class);

        Path aiResultJsonPath = Path.of(pathConfig.getAiResultFilePath(vo.getProject(), vo.getChapterName()));

        boolean hasLines = false;

        for (ChapterInfo.LineInfo lineInfo : chapterInfo.getLineInfos()) {
            for (ChapterInfo.SentenceInfo sentenceInfo : lineInfo.getSentenceInfos()) {
                if (Objects.equals(sentenceInfo.getLinesFlag(), Boolean.TRUE)) {
                    hasLines = true;
                }
            }
        }

        if (!hasLines) {
            if (Files.notExists(aiResultJsonPath)) {
                Files.write(aiResultJsonPath, "{}".getBytes());
            }
            return Flux.empty();
        }

        // 不是重新生成时清空文件
        if (saveResult && Files.exists(aiResultJsonPath)) {
            Files.write(aiResultJsonPath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        }

        File outputFile = new File(aiResultJsonPath.toFile().getAbsolutePath());
        return this.roleAndLinesInference(chapterInfo)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(s -> {
                    System.out.println(s);
                    if (saveResult) {
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
                            writer.write(s);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).onErrorResume(e -> {
                    if (e instanceof WebClientResponseException.Unauthorized) {
                        return Flux.just("ai api 接口认证异常，请检查api key, " + e.getMessage() + " error");
                    }
                    return Flux.just(e.getMessage() + " error");
                })
                .doOnComplete(() -> {
                    if (saveResult) {
                        try {
                            parseAiInference(vo);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }

    public Result<Object> parseAiInference(ChapterVO vo) throws IOException {
        String aiResultJsonPathStr = pathConfig.getAiResultFilePath(vo.getProject(), vo.getChapterName());
        Path aiResultJsonPath = Path.of(aiResultJsonPathStr);

        if (Files.exists(aiResultJsonPath)) {
            String text = Files.readString(aiResultJsonPath);
            AiResult aiResult = parseAiResult(text);
            aiResult = chapterService.reCombineAiResult(vo.getProject(), vo.getChapterName(), aiResult);
            chapterService.saveRoleAndLinesMapping(vo.getProject(), vo.getChapterName(), aiResult);
        }
        return Result.success();
    }

    public AiResult parseAiResult(String text) {
        if (text.startsWith("```json") || text.endsWith("```")) {
            text = text.replace("```json", "").replace("```", "");
        }
        return JSON.parseObject(text, AiResult.class);
    }

    static String parseStep = """
            1. 分析下面原文中有哪些角色，他们的性别和年龄段，角色中有观众、群众之类的角色时统一使用'观众'，性别和年龄段取值范围如下：
            性别：男、女、未知。
            年龄段：孩童、青少年、青年、中年、老年、未知。

            2. 请分析下面台词部分的内容是属于原文部分中哪个角色的，然后结合上下文分析当时的情绪，取值范围如下：
            情绪：快乐、悲伤、愤怒、焦虑、紧张、平静、落寞、赞赏、温柔、嘲讽、失望、轻蔑、羡慕、惊讶、兴奋、感慨、自卑、尴尬、震惊、敬畏、严肃、自卑、未知。

            3. 严格按照台词文本中的顺序在原文文本中查找。每行台词都做一次处理，不能合并台词。
            4. 台词文本中有27行台词，所以你分析结果也要有27个。
            5. 保证输出字符是JSON字符串，不要加代码块提示
            """;

    static String outputFormat = """
            {
              "roles": [
                {
                  "role": "萧炎",
                  "gender": "男",
                  "ageGroup": "青少年"
                }
              ],
              "linesMappings": [
                {
                  "linesIndex": "这里的值是台词前的序号",
                  "role": "萧炎",
                  "gender": "男",
                  "ageGroup": "青少年",
                  "mood": "自卑"
                }
              ]
            }
            """;

    public static String resTemp = """
             {
               "roles": [
                 {
                   "role": "萧炎",
                   "gender": "男",
                   "ageGroup": "青少年"
                 },
                 {
                   "role": "中年男子",
                   "gender": "男",
                   "ageGroup": "中年"
                 },
                 {
                   "role": "观众",
                   "gender": "未知",
                   "ageGroup": "未知"
                 },
                 {
                   "role": "萧媚",
                   "gender": "女",
                   "ageGroup": "青少年"
                 },
                 {
                   "role": "萧薰儿",
                   "gender": "女",
                   "ageGroup": "青少年"
                 }
               ],
               "linesMappings": [
                 {
                   "linesIndex": "1-0",
                   "role": "中年男子",
                   "gender": "男",
                   "ageGroup": "中年",
                   "mood": "漠然"
                 },
                 {
                   "linesIndex": "3-0",
                   "role": "中年男子",
                   "gender": "男",
                   "ageGroup": "中年",
                   "mood": "漠然"
                 },
                 {
                   "linesIndex": "5-0",
                   "role": "观众",
                   "gender": "未知",
                   "ageGroup": "未知",
                   "mood": "嘲讽"
                 },
                 {
                   "linesIndex": "6-0",
                   "role": "观众",
                   "gender": "未知",
                   "ageGroup": "未知",
                   "mood": "轻蔑"
                 },
                 {
                   "linesIndex": "7-0",
                   "role": "观众",
                   "gender": "未知",
                   "ageGroup": "未知",
                   "mood": "轻蔑"
                 },
                 {
                   "linesIndex": "8-0",
                   "role": "观众",
                   "gender": "未知",
                   "ageGroup": "未知",
                   "mood": "惋惜"
                 },
                 {
                   "linesIndex": "9-0",
                   "role": "观众",
                   "gender": "未知",
                   "ageGroup": "未知",
                   "mood": "疑惑"
                 },
                 {
                   "linesIndex": "12-0",
                   "role": "萧炎",
                   "gender": "男",
                   "ageGroup": "青少年",
                   "mood": "落寞"
                 },
                 {
                   "linesIndex": "13-0",
                   "role": "测试员",
                   "gender": "未知",
                   "ageGroup": "未知",
                   "mood": "平静"
                 },
                 {
                   "linesIndex": "18-0",
                   "role": "测试员",
                   "gender": "未知",
                   "ageGroup": "未知",
                   "mood": "平静"
                 },
                 {
                   "linesIndex": "19-0",
                   "role": "测试员",
                   "gender": "未知",
                   "ageGroup": "未知",
                   "mood": "平静"
                 },
                 {
                   "linesIndex": "20-0",
                   "role": "萧媚",
                   "gender": "女",
                   "ageGroup": "青少年",
                   "mood": "得意"
                 },
                 {
                   "linesIndex": "21-0",
                   "role": "观众",
                   "gender": "未知",
                   "ageGroup": "未知",
                   "mood": "羡慕"
                 },
                 {
                   "linesIndex": "22-0",
                   "role": "观众",
                   "gender": "未知",
                   "ageGroup": "未知",
                   "mood": "赞赏"
                 },
                 {
                   "linesIndex": "26-0",
                   "role": "萧媚",
                   "gender": "女",
                   "ageGroup": "青少年",
                   "mood": "感慨"
                 },
                 {
                   "linesIndex": "32-0",
                   "role": "测试员",
                   "gender": "未知",
                   "ageGroup": "未知",
                   "mood": "平静"
                 },
                 {
                   "linesIndex": "40-0",
                   "role": "测试员",
                   "gender": "未知",
                   "ageGroup": "未知",
                   "mood": "惊讶"
                 },
                 {
                   "linesIndex": "42-0",
                   "role": "观众",
                   "gender": "未知",
                   "ageGroup": "未知",
                   "mood": "敬畏"
                 },
                 {
                   "linesIndex": "45-1",
                   "role": "中年测验员",
                   "gender": "男",
                   "ageGroup": "中年",
                   "mood": "赞赏"
                 },
                 {
                   "linesIndex": "47-0",
                   "role": "萧薰儿",
                   "gender": "女",
                   "ageGroup": "青少年",
                   "mood": "平静"
                 },
                 {
                   "linesIndex": "48-0",
                   "role": "萧薰儿",
                   "gender": "女",
                   "ageGroup": "青少年",
                   "mood": "尊敬"
                 },
                 {
                   "linesIndex": "49-0",
                   "role": "萧炎",
                   "gender": "男",
                   "ageGroup": "青少年",
                   "mood": "苦涩"
                 },
                 {
                   "linesIndex": "50-0",
                   "role": "萧薰儿",
                   "gender": "女",
                   "ageGroup": "青少年",
                   "mood": "温柔"
                 },
                 {
                   "linesIndex": "51-0",
                   "role": "萧炎",
                   "gender": "男",
                   "ageGroup": "青少年",
                   "mood": "自嘲"
                 },
                 {
                   "linesIndex": "52-1",
                   "role": "萧薰儿",
                   "gender": "女",
                   "ageGroup": "青少年",
                   "mood": "认真"
                 },
                 {
                   "linesIndex": "52-3",
                   "role": "萧薰儿",
                   "gender": "女",
                   "ageGroup": "青少年",
                   "mood": "赞赏"
                 },
                 {
                   "linesIndex": "53-0",
                   "role": "萧炎",
                   "gender": "男",
                   "ageGroup": "青少年",
                   "mood": "尴尬"
                 }
               ]
             }
            """;
}
