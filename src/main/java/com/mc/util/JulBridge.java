package com.mc.util;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * 把 java.util.logging 的日志接入本项目的 Log。
 *
 * <p>为什么需要：Bukkit 插件通常用 {@code getLogger().info(...)} 输出，
 * 那是 JUL（java.util.logging）。JUL 默认使用平台编码且带冗长的日期前缀，
 * 在中文 Windows 上会既乱码又难看。这里把它重定向到统一的 Log，
 * 输出格式与宿主一致，并带插件名前缀。
 */
public final class JulBridge extends Handler {

    private JulBridge() {
        setLevel(Level.ALL);
    }

    /** 安装到根 logger，接管全部 JUL 输出。 */
    public static void install() {
        Logger root = Logger.getLogger("");
        for (Handler h : root.getHandlers()) {
            root.removeHandler(h);
        }
        root.addHandler(new JulBridge());
        root.setLevel(Level.INFO);

        // 压制第三方库的嘈杂日志
        Logger.getLogger("org.apache").setLevel(Level.WARNING);
        Logger.getLogger("com.zaxxer").setLevel(Level.WARNING);
        Logger.getLogger("io.netty").setLevel(Level.WARNING);
    }

    @Override
    public void publish(LogRecord record) {
        if (record == null) return;
        String msg = record.getMessage();
        if (msg == null) return;

        // 应用参数的格式化（{0} 占位符）
        Object[] params = record.getParameters();
        if (params != null && params.length > 0) {
            try {
                msg = java.text.MessageFormat.format(msg, params);
            } catch (Exception ignore) { }
        }

        String name = record.getLoggerName();
        String prefix = pluginPrefix(name);
        String line = prefix.isEmpty() ? msg : prefix + " " + msg;

        Level lv = record.getLevel();
        if (lv.intValue() >= Level.SEVERE.intValue()) {
            Log.error(line);
        } else if (lv.intValue() >= Level.WARNING.intValue()) {
            Log.warn(line);
        } else {
            Log.raw(Log.tsPrefix() + " [INFO] " + line);
        }
    }

    /**
     * 从 logger 名推断前缀。
     * 宿主的插件 logger 名为 "Minecraft.<插件名>"，这里转成 [插件名]。
     */
    private static String pluginPrefix(String loggerName) {
        if (loggerName == null || loggerName.isEmpty() || loggerName.equals("Minecraft")) {
            return "";
        }
        String n = loggerName;
        if (n.startsWith("Minecraft.")) {
            n = n.substring("Minecraft.".length());
        }
        // 类名形式（a.b.C）只保留最后一段，避免日志过长
        int dot = n.lastIndexOf('.');
        if (dot >= 0 && dot < n.length() - 1) {
            String last = n.substring(dot + 1);
            // 若看起来像类名（首字母大写且还有包路径），保留全名更利于排查
            if (last.matches("[A-Z].*") && !last.equals(n)) {
                return "[" + n + "]";
            }
        }
        return "[" + n + "]";
    }

    @Override
    public void flush() { }

    @Override
    public void close() { }
}