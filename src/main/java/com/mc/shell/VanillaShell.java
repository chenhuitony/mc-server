package com.mc.shell;

import com.mc.util.I18n;
import com.mc.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 原版服务端外壳。
 *
 * <p>下载并启动官方 server.jar（或已有的 paper.jar），
 * 通过 stdin/stdout 与其交互，把英文控制台输出翻译为当前语言。
 *
 * <p>与「内核模式」的区别：
 * <ul>
 *   <li>内核模式（默认）：自己实现 Bukkit API 子集，直接加载插件 JAR。</li>
 *   <li>外壳模式（--shell vanilla）：运行真正的官方服务端，享受完整原版功能，
 *       本程序只做启动、翻译和插件目录准备。</li>
 * </ul>
 */
public class VanillaShell {

    private final File rootDir;
    private final String wantedVersion;
    private Process process;
    private volatile boolean running;

    public VanillaShell(File rootDir, String wantedVersion) {
        this.rootDir = rootDir;
        this.wantedVersion = wantedVersion;
    }

    /** 启动外壳。 */
    public int run() throws Exception {
        rootDir.mkdirs();
        File jar = new File(rootDir, "server.jar");

        // 1. 确保 server.jar 存在
        ServerFetcher fetcher = new ServerFetcher();
        try {
            ServerFetcher.VersionInfo info = fetcher.resolve(wantedVersion);
            fetcher.download(info, jar);
        } catch (Exception e) {
            if (jar.exists()) {
                Log.warn("shell.download_skip", e.getMessage());
            } else {
                Log.error("shell.download_fail", e.getClass().getSimpleName() + ": " + e.getMessage());
                return 1;
            }
        }

        // 2. 准备 eula.txt
        File eula = new File(rootDir, "eula.txt");
        if (!eula.exists()) {
            com.mc.util.IoUtil.writeAll(eula,
                    "#By changing the setting below to TRUE you are indicating your agreement to our EULA\n"
                            + "eula=true\n");
        }

        // 3. 准备 plugins 目录
        new File(rootDir, "plugins").mkdirs();

        // 4. 启动
        List<String> cmd = new ArrayList<>();
        cmd.add(javaBin());
        cmd.add("-Xms1G");
        cmd.add("-Xmx2G");
        cmd.add("-Dfile.encoding=UTF-8");
        cmd.add("-Dstdout.encoding=UTF-8");
        cmd.add("-jar");
        cmd.add(jar.getAbsolutePath());
        cmd.add("nogui");

        Log.info("shell.starting", String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(rootDir);
        pb.redirectErrorStream(true);
        process = pb.start();
        running = true;

        // 5. 控制台输入转发给子进程
        Thread stdinPump = new Thread(() -> pumpConsole(), "Shell-Stdin");
        stdinPump.setDaemon(true);
        stdinPump.start();

        // 6. 读取子进程输出并翻译
        ConsoleTranslator translator = new ConsoleTranslator();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Log.raw(translator.translate(line));
            }
        }

        int code = process.waitFor();
        running = false;
        Log.info("shell.exited", code);
        return code;
    }

    /** 把控制台输入原样转发给子进程。 */
    private void pumpConsole() {
        try {
            OutputStream out = process.getOutputStream();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String line;
            while (running && (line = in.readLine()) != null) {
                out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
                if (line.trim().equalsIgnoreCase("stop")) break;
            }
        } catch (Exception ignore) { }
    }

    /** 停止子进程。 */
    public void shutdown() {
        running = false;
        if (process != null && process.isAlive()) {
            try {
                OutputStream out = process.getOutputStream();
                out.write("stop\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
                if (!process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (Exception e) {
                process.destroyForcibly();
            }
        }
    }

    private static String javaBin() {
        String home = System.getProperty("java.home");
        String exe = System.getProperty("os.name").toLowerCase().contains("win")
                ? "java.exe" : "java";
        File f = new File(new File(home, "bin"), exe);
        return f.exists() ? f.getAbsolutePath() : "java";
    }
}