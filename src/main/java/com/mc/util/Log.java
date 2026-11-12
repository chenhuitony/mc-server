package com.mc.util;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志与中文控制台输出。
 *
 * <p>编码策略：真实控制台时跟随控制台编码（Windows 中文环境常为 GBK）；
 * 被重定向到管道或文件时统一 UTF-8，避免字节错乱。
 */
public final class Log {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static PrintStream out;
    private static PrintStream file;

    private Log() {}

    public static void init(File logFile) {
        String enc = "UTF-8";
        if (System.console() != null) {
            String se = System.getProperty("stdout.encoding");
            if (se != null) enc = se;
        }
        try {
            out = new PrintStream(new java.io.BufferedOutputStream(
                    new FileOutputStream(FileDescriptor.out), 8192), true, Charset.forName(enc));
        } catch (Exception e) {
            out = System.out;
        }
        if (logFile != null) {
            try {
                logFile.getParentFile().mkdirs();
                file = new PrintStream(new FileOutputStream(logFile, true), true, "UTF-8");
            } catch (Exception ignore) { }
        }
    }

    private static String ts() {
        return "[" + LocalDateTime.now().format(TS) + "]";
    }

    /** 供 JulBridge 复用的时间戳前缀。 */
    public static String tsPrefix() {
        return ts();
    }

    private static void emit(String line) {
        out.println(line);
        out.flush();
        if (file != null) {
            file.println(line);
            file.flush();
        }
    }

    public static void info(String m)  { emit(ts() + " [INFO] " + m); }
    public static void ok(String m)    { emit(ts() + " [ OK ] " + m); }
    public static void warn(String m)  { emit(ts() + " [WARN] " + m); }
    public static void error(String m) { emit(ts() + "[ERROR] " + m); }

    /** 插件输出的原样日志，不重复加时间戳前缀时使用。 */
    public static void raw(String m)   { emit(m); }

    public static void progress(int pct, long got, long total) {
        String g = String.format("%.1f", got / 1024.0 / 1024.0);
        String t = total > 0 ? String.format("%.1f MB", total / 1024.0 / 1024.0) : "?";
        int bars = Math.max(0, Math.min(20, pct / 5));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) sb.append(i < bars ? '#' : '-');
        out.print("\r" + ts() + " [>>>] [" + sb + "] " + pct + "% " + g + "MB / " + t + "  ");
        out.flush();
    }

    public static void progressDone() {
        out.println();
        out.flush();
    }
}