package com.mc.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 通用 IO / 文本 / 校验工具。 */
public final class IoUtil {

    private IoUtil() {}

    public static List<String> readLines(File f) throws Exception {
        return Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
    }

    public static String readAll(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    public static void writeAll(File f, String text) throws Exception {
        File parent = f.getParentFile();
        if (parent != null) parent.mkdirs();
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    public static void copyStream(InputStream in, OutputStream out) throws Exception {
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
    }

    public static void copyResource(InputStream in, File dest) throws Exception {
        File parent = dest.getParentFile();
        if (parent != null) parent.mkdirs();
        try (OutputStream out = new FileOutputStream(dest)) {
            copyStream(in, out);
        }
    }

    public static String sha1(File f) throws Exception {
        return hash(f, "SHA-1");
    }

    public static String sha256(File f) throws Exception {
        return hash(f, "SHA-256");
    }

    private static String hash(File f, String algo) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algo);
        try (InputStream in = Files.newInputStream(f.toPath())) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static long sizeOf(File f) {
        return f == null ? 0 : (f.isFile() ? f.length() : 0);
    }

    public static String humanSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024L * 1024) return String.format("%.1f KB", b / 1024.0);
        if (b < 1024L * 1024 * 1024) return String.format("%.1f MB", b / 1024.0 / 1024.0);
        return String.format("%.2f GB", b / 1024.0 / 1024.0 / 1024.0);
    }

    /** 下载文件，带进度回调与断点续传。 */
    public interface Progress { void on(int pct, long got, long total); }

    public static long download(URL url, File dest, long startOffset, Progress cb) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "MCServer/1.0");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        if (startOffset > 0) conn.setRequestProperty("Range", "bytes=" + startOffset + "-");

        int code = conn.getResponseCode();
        boolean partial = code == 206;
        if (code != 200 && !partial) {
            throw new RuntimeException("HTTP " + code + " " + url);
        }

        long total = -1;
        String cl = conn.getHeaderField("Content-Length");
        if (cl != null) total = Long.parseLong(cl) + (partial ? startOffset : 0);

        File parent = dest.getParentFile();
        if (parent != null) parent.mkdirs();

        try (InputStream in = conn.getInputStream();
             java.io.RandomAccessFile raf = new java.io.RandomAccessFile(dest, "rw")) {
            long pos = partial && startOffset > 0 ? startOffset : 0;
            raf.seek(pos);
            if (pos == 0) raf.setLength(0);

            byte[] buf = new byte[64 * 1024];
            int n;
            long last = 0;
            while ((n = in.read(buf)) != -1) {
                raf.write(buf, 0, n);
                pos += n;
                long now = System.currentTimeMillis();
                if (cb != null && now - last > 200 && total > 0) {
                    cb.on((int) (pos * 100 / total), pos, total);
                    last = now;
                }
            }
            if (cb != null && total > 0) cb.on(100, pos, total);
            return pos;
        }
    }

    /** 递归列出目录下的文件。 */
    public static List<File> listFilesRecursive(File dir) {
        List<File> out = new ArrayList<>();
        walk(dir, out);
        return out;
    }

    private static void walk(File dir, List<File> out) {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) walk(f, out);
            else out.add(f);
        }
    }

    /** 从 Map 深层取值，path 依次下钻。 */
    @SuppressWarnings("unchecked")
    public static Object dig(Map<String, Object> m, String... path) {
        Object cur = m;
        for (String p : path) {
            if (!(cur instanceof Map)) return null;
            cur = ((Map<String, Object>) cur).get(p);
        }
        return cur;
    }

    public static String digStr(Map<String, Object> m, String... path) {
        Object o = dig(m, path);
        return o == null ? null : String.valueOf(o);
    }
}