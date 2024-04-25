package com.example.novelcastserver.utils;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Wav文件多合一
 *
 * @author yujing 2020年6月15日15:43:20
 */
public class WavCompose {
    private int headLength1 = 0;
    private int headLength2 = 0;

    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[]{(byte) ((a >> 24) & 0xFF), (byte) ((a >> 16) & 0xFF), (byte) ((a >> 8) & 0xFF), (byte) (a & 0xFF)};
    }

    //字节翻转
    public static byte[] byteToByte(byte[] a) {
        if (a.length == 4)
            return new byte[]{a[3], a[2], a[1], a[0]};
        return null;
    }

    //更改文件头
    private void updateFileHead(String out, boolean ifUpdate) throws IOException {
        MappedByteBuffer buffer = null;
        try (RandomAccessFile raf = new RandomAccessFile(out, "rw");
             FileChannel channel = raf.getChannel()) { // 文件头长度

            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 44);
            // 修改头部文件
            if (ifUpdate) {
                byte[] head1 = byteToByte(intToByteArray(headLength1));
                byte[] head2 = byteToByte(intToByteArray(headLength2));
                // 进行修改操作
                buffer.put(4, head1[0]);
                buffer.put(5, head1[1]);
                buffer.put(6, head1[2]);
                buffer.put(7, head1[3]);
                buffer.put(40, head2[0]);
                buffer.put(41, head2[1]);
                buffer.put(42, head2[2]);
                buffer.put(43, head2[3]);
                buffer.force(); // 强制输出，在buffer中的改动生效到文件
            } else {
                headLength1 = byteArrayToInt(byteToByte(new byte[]{buffer.get(4), buffer.get(5), buffer.get(6), buffer.get(7)}));
                headLength2 = byteArrayToInt(byteToByte(new byte[]{buffer.get(40), buffer.get(41), buffer.get(42), buffer.get(43)}));
            }
            buffer.clear();
        }
        // 不需要显式调用 raf.close() 和 channel.close()
        // 因为在 try-with-resources 语句块结束时会自动关闭
    }

    /**
     * 将多个wav合成一个新的wav
     *
     * @param out 输出文件
     * @param in  输入文件数组
     * @throws IOException
     */
    public void addWav(String out, List<String> in) throws IOException {
        if (Files.notExists(Path.of(out))) {
            Files.createDirectories(Path.of(out).toAbsolutePath().getParent());
            Files.createFile(Path.of(out));
        }
        try (OutputStream os = new FileOutputStream(Path.of(out).toAbsolutePath().toString(), true)) { // 追加模式
            for (String inputFile : in) {
                File file1 = new File(inputFile);
                if (!file1.exists()) {
                    continue;
                }
                try (InputStream is = new FileInputStream(file1)) {
                    if (in.indexOf(inputFile) != 0) {
                        //noinspection ResultOfMethodCallIgnored
                        is.skip(44); // 跳过后面的.wav的文件头
                    }
                    byte[] tempBuffer = new byte[1024];
                    int nRed = 0;
                    // 将wav全部内容复制到out.wav
                    while ((nRed = is.read(tempBuffer)) != -1) {
                        os.write(tempBuffer, 0, nRed);
                        os.flush();
                    }
                }
            }
            // 到此完成了in数组全部wav合并成out.wav
            // 但是此时播放out.wav,音频内容仍然只是第一个音频的内容，所以还要更改out.wav的文件头
            for (String s : in) {
                updateFileHead(s, false);
            }
            updateFileHead(out, true); //头部合成
        }
    }
}
