package com.example.novelcastserver.utils;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * @Author huyi @Date 2021/9/30 15:33 @Description: 静音音频工具类
 */
public class AudioUtils {

    /**
     * 根据PCM文件构建wav的header字段
     *
     * @param srate   Sample rate - 8000, 16000, etc.
     * @param channel Number of channels - Mono = 1, Stereo = 2, etc..
     * @param format  Number of bits per sample (16 here)
     */
    private static byte[] buildWavHeader(int dataLength, int srate, int channel, int format) {
        byte[] header = new byte[44];

        long totalDataLen = dataLength + 36;
        long bitrate = srate * channel * format;

        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = (byte) format;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channel;
        header[23] = 0;
        header[24] = (byte) (srate & 0xff);
        header[25] = (byte) ((srate >> 8) & 0xff);
        header[26] = (byte) ((srate >> 16) & 0xff);
        header[27] = (byte) ((srate >> 24) & 0xff);
        header[28] = (byte) ((bitrate / 8) & 0xff);
        header[29] = (byte) (((bitrate / 8) >> 8) & 0xff);
        header[30] = (byte) (((bitrate / 8) >> 16) & 0xff);
        header[31] = (byte) (((bitrate / 8) >> 24) & 0xff);
        header[32] = (byte) ((channel * format) / 8);
        header[33] = 0;
        header[34] = 16;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (dataLength & 0xff);
        header[41] = (byte) ((dataLength >> 8) & 0xff);
        header[42] = (byte) ((dataLength >> 16) & 0xff);
        header[43] = (byte) ((dataLength >> 24) & 0xff);

        return header;
    }

    /**
     * 默认写入的pcm数据是16000采样率，16bit，可以按照需要修改
     *
     * @param filePath
     * @param pcmData
     */
    private static boolean writeToFile(String filePath, byte[] pcmData) {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(filePath));
            byte[] header = buildWavHeader(pcmData.length, 32000, 1, 16);
            bos.write(header, 0, 44);
            bos.write(pcmData);
            bos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return false;
    }

    /**
     * 生成静音音频
     *
     * @param filePath 输出文件地址
     * @param duration 音频时长
     */
    public static void makeSilenceWav(String filePath, Long duration) {
        List<Byte> oldBytes = new ArrayList<>();
        IntStream.range(0, (int) (duration * 32)).forEach(x -> oldBytes.add((byte) 0));
        byte[] byteArray = new byte[oldBytes.size()];
        for (int i = 0; i < oldBytes.size(); i++) {
            // 将Byte对象转换为byte类型，并赋值给byte数组
            byteArray[i] = oldBytes.get(i);
        }
        writeToFile(filePath, byteArray);
    }

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