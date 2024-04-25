package com.example.novelcastserver.utils;

import com.example.novelcastserver.bean.*;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.IntStream.range;

public class ChapterExtractor {

    public static final String CHAPTER_TITLE_PATTERN = "^\\s*第.{1,9}[章节卷集部篇回]\\s.*";

    public static final String QUOTATION_MARK = "(?<=\")[^\"]*(?=\")|(?<=“)[^”]*(?=”)";

    public static Pattern pattern = Pattern.compile("“[^”]+”");

    public static List<ChapterParse> extractor(String filePath) throws IOException {
        List<ChapterParse> chapterPars = new ArrayList<>();
        StringBuilder preface = new StringBuilder();

        Charset charset = detectCharset(filePath);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), charset))) {
            String line;
            StringBuilder chapterContent = new StringBuilder();
            ChapterParse chapterParse = null;

            while ((line = reader.readLine()) != null) {

                if (isChapterTitle(line)) {
                    if (chapterParse != null) {
                        chapterParse.setContent(chapterContent.toString());
                        chapterPars.add(chapterParse);
                        chapterContent.setLength(0); // Resetting the content for the next chapterParse
                    }

                    chapterParse = new ChapterParse();
                    chapterParse.setTitle(line.trim());
                }
//                else {
//                    if (chapterParse == null) {
//                        // If content is null, it means this part is before the first chapterParse
//                        preface.append(line).append("\n");
//                    } else {
//                        // Append line to chapter content if it's not a title
//                        chapterContent.append(line).append("\n");
//                    }
//                }
                if (chapterParse == null) {
                    // If content is null, it means this part is before the first chapterParse
                    preface.append(line).append("\n");
                } else {
                    // Append line to chapter content if it's not a title
                    chapterContent.append(line).append("\n");
                }
            }

            // Process the last chapterParse
            if (chapterParse != null) {
                chapterParse.setContent(chapterContent.toString());
                chapterPars.add(chapterParse);
            }
        }

        if (!preface.isEmpty()) {
            ChapterParse prefaceChapter = new ChapterParse();
            prefaceChapter.setTitle(preface.toString().split("\\n")[0].trim()); // Setting the title from the first line
            prefaceChapter.setContent(preface.toString());
            prefaceChapter.setPrologue(true);
            chapterPars.addFirst(prefaceChapter);
        }

        return chapterPars;
    }

    public static boolean isChapterTitle(String line) {
        return line.matches(CHAPTER_TITLE_PATTERN);
    }

    private static Charset detectCharset(String filePath) throws IOException {
        try (InputStream inputStream = new FileInputStream(filePath);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            UniversalDetector detector = new UniversalDetector(null);
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                detector.handleData(buffer, 0, bytesRead);
            }
            detector.dataEnd();
            String detectedCharset = detector.getDetectedCharset();

            return detectedCharset == null ? Charset.defaultCharset() : Charset.forName(detectedCharset);
        }
    }

    public static String generateMarkedText(String content) {
        StringBuilder sb = new StringBuilder();

        StringBuilder fixedContent = new StringBuilder();
        String[] lines = content.split("\n");
        for (String line : lines) {
            // 标题不做处理
            if (!isChapterTitle(line)) {
                // 去除行首的空格
                String trimmedLine = line.stripLeading();
                fixedContent.append(trimmedLine).append("\n");
            } else {
                sb.append(line).append("\n");
            }
        }

        String formatStr = fixedContent.toString();

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(QUOTATION_MARK, java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher matcher = pattern.matcher(formatStr);
        int i = 1;
        int lastIndex = 0;
        while (matcher.find()) {
            String matchText = formatStr.substring(lastIndex, matcher.start() - 1);
            sb.append(matchText);
            sb.append("<[").append(i).append("]>");
            lastIndex = matcher.end() + 1;
            i++;
        }
        sb.append(formatStr.substring(lastIndex));
        return sb.toString();
    }

    public static List<ChapterInfo> parseChapter(String filePath) throws IOException {
        List<ChapterParse> chapterParses = ChapterExtractor.extractor(filePath);
        List<ChapterInfo> chapterInfos = new ArrayList<>();
        for (int i = 0; i < chapterParses.size(); i++) {
            ChapterInfo chapterInfo = new ChapterInfo();
            chapterInfo.setIndex(i);
            chapterInfo.setTitle(chapterParses.get(i).getTitle());
            chapterInfo.setLineInfos(parseChapterInfo(chapterParses.get(i).getContent()));
            chapterInfos.add(chapterInfo);
        }
        return chapterInfos;
    }

    public static List<ChapterInfo.LineInfo> parseChapterInfo(String chapter) {
        List<ChapterInfo.LineInfo> lineInfos = new ArrayList<>();
        String[] split = chapter.split("\n");
        range(0, split.length).forEach(i -> {
            String line = split[i];
            ChapterInfo.LineInfo lineInfo = new ChapterInfo.LineInfo();
            String trimmedLine = line.stripLeading();
            if (!trimmedLine.isEmpty()) {
                List<ChapterInfo.SentenceInfo> sentenceInfos = parseLineInfo(trimmedLine);
                lineInfo.setIndex(i);
                lineInfo.setSentenceInfos(sentenceInfos);
                lineInfos.add(lineInfo);
            }
        });
        return lineInfos;
    }

    public static List<ChapterInfo.SentenceInfo> parseLineInfo(String line) {
        Matcher matcher = pattern.matcher(line);
        List<ChapterInfo.SentenceInfo> sentenceInfos = new ArrayList<>();
        int lastIndex = 0;
        int i = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start > lastIndex) {
                sentenceInfos.add(new ChapterInfo.SentenceInfo(i++, line.substring(lastIndex, start)));
            }
            sentenceInfos.add(new ChapterInfo.SentenceInfo(i++, matcher.group(), true));
            lastIndex = end;
        }
        if (lastIndex < line.length()) {
            sentenceInfos.add(new ChapterInfo.SentenceInfo(i, line.substring(lastIndex)));
        }
        return sentenceInfos;
    }
}
