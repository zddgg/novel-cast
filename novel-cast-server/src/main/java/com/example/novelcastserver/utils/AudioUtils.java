package com.example.novelcastserver.utils;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @Author huyi @Date 2021/9/30 15:33 @Description: 静音音频工具类
 */
public class AudioUtils {

    public static void generateSilentAudio(String output, long durationInMilliseconds) throws Exception {
        Path outputPath = Path.of(output);

        Files.createDirectories(outputPath.getParent());
        Files.deleteIfExists(outputPath);

        // 将毫秒转换为秒
        float durationInSeconds = durationInMilliseconds / 1000f;

        // 构建 FFmpeg 命令
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-f", "lavfi",
                "-i", String.format("anullsrc=duration=%.1fs", durationInSeconds),
                "-c:a", "pcm_s16le",
                "-ar", "32000",
                "-ac", "1",
                output
        );

        // 启动 FFmpeg 进程
        Process process = processBuilder.start();

        // 等待 FFmpeg 进程执行完成
        process.waitFor();
    }


    public static Long getAudioTime(String filePath) throws Exception {
        FFmpegFrameGrabber grabberOne = null;
        try {
            grabberOne = FFmpegFrameGrabber.createDefault(filePath);
            grabberOne.start();
            // 计算时长
            return grabberOne.getLengthInTime() / 1000;
        } finally {
            if (Objects.nonNull(grabberOne)) {
                grabberOne.close();
            }
        }

    }

    public static void audioConcat(String output, List<String> inputs) throws IOException {
        List<FFmpegFrameGrabber> grabbers = new ArrayList<>();
        List<Integer> sampleRates = new ArrayList<>();
        FFmpegFrameGrabber firstGrabber = null;
        for (String srcFile : inputs) {
            firstGrabber = new FFmpegFrameGrabber(srcFile);
            firstGrabber.start();
            grabbers.add(firstGrabber);
            sampleRates.add(firstGrabber.getSampleRate());
        }
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(output, firstGrabber.getAudioChannels());
        recorder.setSampleRate(sampleRates.getFirst());
        recorder.start();
        Frame frame;
        for (FFmpegFrameGrabber grabber : grabbers) {
            while ((frame = grabber.grabFrame()) != null) {
                if (frame.samples == null) {
                    break;
                }
                recorder.recordSamples(frame.samples);
            }
        }
        for (FFmpegFrameGrabber grabber : grabbers) {
            grabber.stop();
        }
        recorder.stop();
    }

    public static void audioSpeedControl(String input, Float speed, String output) throws Exception {
        Path outputPath = Path.of(output);

        Files.createDirectories(outputPath.getParent());
        Files.deleteIfExists(outputPath);

        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-i", input,
                "-filter:a", String.format("\"atempo=%.1f\"", speed),
                "-vn",
                output
        );
        Process process = processBuilder.start();
        process.waitFor();
    }
}