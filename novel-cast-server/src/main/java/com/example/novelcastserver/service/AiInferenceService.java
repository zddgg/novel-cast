package com.example.novelcastserver.service;

import com.alibaba.fastjson2.JSON;
import com.example.novelcastserver.bean.ChapterInfo;
import com.example.novelcastserver.bean.Lines;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AiInferenceService {

    private final OpenAiChatClient chatClient;

    public AiInferenceService(OpenAiChatClient chatClient) {
        this.chatClient = chatClient;
    }


    public Flux<String> roleAndLinesInference(ChapterInfo chapterInfo) {
        List<Lines> linesList = new ArrayList<>();
        chapterInfo.getLineInfos().forEach(lineInfo -> {
            lineInfo.getSentenceInfos().forEach(sentenceInfo -> {
                if (Optional.ofNullable(sentenceInfo.getLines()).orElse(false)) {
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
                  "role": "少女",
                  "gender": "女",
                  "ageGroup": "孩童"
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
                  "linesIndex": "0-0",
                  "role": "中年男子",
                  "gender": "男",
                  "ageGroup": "中年",
                  "mood": "平静",
                  "lines": "“斗之力，三段！”"
                },
                {
                  "linesIndex": "2-0",
                  "role": "中年男子",
                  "gender": "男",
                  "ageGroup": "中年",
                  "mood": "漠然",
                  "lines": "“萧炎，斗之力，三段！级别：低级！”"
                },
                {
                  "linesIndex": "4-0",
                  "role": "观众",
                  "gender": "未知",
                  "ageGroup": "未知",
                  "mood": "嘲讽",
                  "lines": "“三段？嘿嘿，果然不出我所料，这个‘天才’这一年又是在原地踏步！”"
                },
                {
                  "linesIndex": "5-0",
                  "role": "观众",
                  "gender": "未知",
                  "ageGroup": "未知",
                  "mood": "失望",
                  "lines": "“哎，这废物真是把家族的脸都给丢光了。”"
                },
                {
                  "linesIndex": "6-0",
                  "role": "观众",
                  "gender": "未知",
                  "ageGroup": "未知",
                  "mood": "轻蔑",
                  "lines": "“要不是族长是他的父亲，这种废物，早就被驱赶出家族，任其自生自灭了，哪还有机会待在家族中白吃白喝。”"
                },
                {
                  "linesIndex": "7-0",
                  "role": "观众",
                  "gender": "未知",
                  "ageGroup": "未知",
                  "mood": "感慨",
                  "lines": "“唉，昔年那名闻乌坦城的天才少年，如今怎么落魄成这般模样了啊？”"
                },
                {
                  "linesIndex": "8-0",
                  "role": "观众",
                  "gender": "未知",
                  "ageGroup": "未知",
                  "mood": "惊讶",
                  "lines": "“谁知道呢，或许做了什么亏心事，惹得神灵降怒了吧……”"
                },
                {
                  "linesIndex": "11-0",
                  "role": "萧炎",
                  "gender": "男",
                  "ageGroup": "青少年",
                  "mood": "落寞",
                  "lines": "“这些人，都如此刻薄势力吗？或许是因为三年前他们曾经在自己面前露出过最谦卑的笑容，所以，如今想要讨还回去吧……”"
                },
                {
                  "linesIndex": "12-0",
                  "role": "中年男子",
                  "gender": "男",
                  "ageGroup": "中年",
                  "mood": "平静",
                  "lines": "“下一个，萧媚！”"
                },
                {
                  "linesIndex": "17-0",
                  "role": "中年男子",
                  "gender": "男",
                  "ageGroup": "中年",
                  "mood": "平静",
                  "lines": "“斗之气：七段！”"
                },
                {
                  "linesIndex": "18-0",
                  "role": "中年男子",
                  "gender": "男",
                  "ageGroup": "中年",
                  "mood": "赞赏",
                  "lines": "“萧媚，斗之气：七段！级别：高级！”"
                },
                {
                  "linesIndex": "19-0",
                  "role": "萧媚",
                  "gender": "女",
                  "ageGroup": "青少年",
                  "mood": "兴奋",
                  "lines": "“耶！”"
                },
                {
                  "linesIndex": "20-0",
                  "role": "观众",
                  "gender": "未知",
                  "ageGroup": "未知",
                  "mood": "羡慕",
                  "lines": "“啧啧，七段斗之气，真了不起，按这进度，恐怕顶多只需要三年时间，她就能称为一名真正的斗者了吧……”"
                },
                {
                  "linesIndex": "21-0",
                  "role": "观众",
                  "gender": "未知",
                  "ageGroup": "未知",
                  "mood": "赞赏",
                  "lines": "“不愧是家族中种子级别的人物啊……”"
                },
                {
                  "linesIndex": "25-0",
                  "role": "萧媚",
                  "gender": "女",
                  "ageGroup": "青少年",
                  "mood": "落寞",
                  "lines": "“唉……”"
                },
                {
                  "linesIndex": "31-0",
                  "role": "中年男子",
                  "gender": "男",
                  "ageGroup": "中年",
                  "mood": "平静",
                  "lines": "“下一个，萧薰儿！”"
                },
                {
                  "linesIndex": "39-0",
                  "role": "中年男子",
                  "gender": "男",
                  "ageGroup": "中年",
                  "mood": "平静",
                  "lines": "“斗之气：九段！级别：高级！”"
                },
                {
                  "linesIndex": "41-0",
                  "role": "观众",
                  "gender": "未知",
                  "ageGroup": "未知",
                  "mood": "敬畏",
                  "lines": "“……竟然到九段了，真是恐怖！家族中年轻一辈的第一人，恐怕非薰儿小姐莫属了。”"
                },
                {
                  "linesIndex": "44-1",
                  "role": "中年测验员",
                  "gender": "男",
                  "ageGroup": "中年",
                  "mood": "严肃",
                  "lines": "“薰儿小姐，半年之后，你应该便能凝聚斗气之旋，如果你成功的话，那么以十四岁年龄成为一名真正的斗者，你是萧家百年内的第二人！”"
                },
                {
                  "linesIndex": "46-0",
                  "role": "萧薰儿",
                  "gender": "女",
                  "ageGroup": "青少年",
                  "mood": "平静",
                  "lines": "“谢谢。”"
                },
                {
                  "linesIndex": "47-0",
                  "role": "萧炎",
                  "gender": "男",
                  "ageGroup": "青少年",
                  "mood": "自卑",
                  "lines": "“萧炎哥哥。”"
                },
                {
                  "linesIndex": "48-0",
                  "role": "萧炎",
                  "gender": "男",
                  "ageGroup": "青少年",
                  "mood": "自卑",
                  "lines": "“我现在还有资格让你怎么叫么？”"
                },
                {
                  "linesIndex": "49-0",
                  "role": "萧薰儿",
                  "gender": "女",
                  "ageGroup": "青少年",
                  "mood": "温柔",
                  "lines": "“萧炎哥哥，以前你曾经与薰儿说过，要能放下，才能拿起，提放自如，是自在人！”"
                },
                {
                  "linesIndex": "50-0",
                  "role": "萧炎",
                  "gender": "男",
                  "ageGroup": "青少年",
                  "mood": "落寞",
                  "lines": "“呵呵，自在人？我也只会说而已，你看我现在的模样，象自在人吗？而且……这世界，本来就不属于我。”"
                },
                {
                  "linesIndex": "51-1",
                  "role": "萧薰儿",
                  "gender": "女",
                  "ageGroup": "青少年",
                  "mood": "赞赏",
                  "lines": "“萧炎哥哥，虽然并不知道你究竟是怎么回事，不过，薰儿相信，你会重新站起来，取回属于你的荣耀与尊严……”"
                },
                {
                  "linesIndex": "51-3",
                  "role": "萧薰儿",
                  "gender": "女",
                  "ageGroup": "青少年",
                  "mood": "赞赏",
                  "lines": "“当年的萧炎哥哥，的确很吸引人……”"
                },
                {
                  "linesIndex": "52-0",
                  "role": "萧炎",
                  "gender": "男",
                  "ageGroup": "青少年",
                  "mood": "尴尬",
                  "lines": "“呵呵……”"
                }
              ]
            }
            """;
}
