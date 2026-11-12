package com.mc;

import com.mc.server.McServer;
import com.mc.util.I18n;
import com.mc.util.Log;

import java.io.File;

/**
 * 入口。
 *
 * <p>用法：
 * <pre>
 *   java -jar mc-server.jar                          在当前目录启动
 *   java -jar mc-server.jar --dir myserver           指定运行目录
 *   java -jar mc-server.jar --scan-only              只扫描插件并退出
 *   java -jar mc-server.jar --lang en_US             指定语言（zh_CN / en_US）
 * </pre>
 */
public class Main {

    public static void main(String[] args) {
        File dir = new File(".");
        boolean scanOnly = false;
        String shellMode = null;
        String version = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dir":
                    if (i + 1 < args.length) dir = new File(args[++i]);
                    break;
                case "--scan-only":
                    scanOnly = true;
                    break;
                case "--lang":
                    if (i + 1 < args.length) {
                        I18n.setLanguage(args[++i]);
                    }
                    break;
                case "--shell":
                    if (i + 1 < args.length) shellMode = args[++i];
                    break;
                case "--version":
                    if (i + 1 < args.length) version = args[++i];
                    break;
                case "--help":
                case "-h":
                    printUsage();
                    return;
                default:
                    break;
            }
        }

        dir.mkdirs();
        Log.init(new File(dir, "logs/latest.log"));
        com.mc.util.JulBridge.install();

        // 外壳模式：运行真正的原版/Paper 服务端
        if ("vanilla".equalsIgnoreCase(shellMode) || "paper".equalsIgnoreCase(shellMode)) {
            printBanner(dir);
            Log.info("shell.mode", shellMode.toLowerCase());
            com.mc.shell.VanillaShell shell = new com.mc.shell.VanillaShell(dir, version);
            Runtime.getRuntime().addShutdownHook(new Thread(shell::shutdown, "Shell-Shutdown"));
            try {
                int code = shell.run();
                System.exit(code);
            } catch (Throwable e) {
                Log.error(I18n.get("shell.fail", e.getClass().getSimpleName() + ": " + e.getMessage()));
                System.exit(1);
            }
            return;
        }

        if (scanOnly) {
            com.mc.loader.PluginScanner scanner = new com.mc.loader.PluginScanner();
            com.mc.loader.PluginScanner.ScanResult r =
                    scanner.scan(new File(dir, "plugins"));
            scanner.printReport(r);
            System.exit(0);
        }

        McServer server = new McServer(dir);
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown, "MC-Shutdown"));
        try {
            server.start();
        } catch (Throwable e) {
            Log.error(I18n.get("error.init_failed", e.getClass().getSimpleName() + ": " + e.getMessage()));
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printBanner(File dir) {
        Log.info("server.starting");
        Log.info("server.version", McServer.VERSION);
        Log.info("server.target", McServer.MINECRAFT_TARGET);
        Log.info("server.dir", dir.getAbsolutePath());
        Log.info("server.ending");
    }

    private static void printUsage() {
        boolean zh = "zh_CN".equals(I18n.getLanguage());
        System.out.println(I18n.get("server.version", McServer.VERSION));
        System.out.println();
        if (zh) {
            System.out.println("用法：");
            System.out.println("  java -jar mc-server.jar                    在当前目录启动（内核模式）");
            System.out.println("  java -jar mc-server.jar --dir <目录>       指定运行目录");
            System.out.println("  java -jar mc-server.jar --lang zh_CN|en_US 指定语言");
            System.out.println("  java -jar mc-server.jar --scan-only        只扫描插件后退出");
            System.out.println("  java -jar mc-server.jar --shell vanilla    外壳模式：下载并运行官方服务端");
            System.out.println("  java -jar mc-server.jar --shell vanilla --version 1.21.8");
            System.out.println("  java -jar mc-server.jar --help             显示本帮助");
            System.out.println();
            System.out.println("两种模式：");
            System.out.println("  内核模式（默认）：自研 Bukkit API，直接加载 plugins/ 里的插件 JAR");
            System.out.println("  外壳模式（--shell）：运行真正的官方 server.jar，控制台输出翻译为中文");
            System.out.println();
            System.out.println("插件放在 <目录>/plugins/ 下，支持：");
            System.out.println("  Bukkit / Paper 插件（plugin.yml、paper-plugin.yml）—— 可运行");
            System.out.println("  Fabric 模组（fabric.mod.json）—— 仅识别");
            System.out.println("  Forge 模组（META-INF/mods.toml）—— 仅识别");
        } else {
            System.out.println("Usage:");
            System.out.println("  java -jar mc-server.jar                    Start in current directory (kernel mode)");
            System.out.println("  java -jar mc-server.jar --dir <dir>        Specify working directory");
            System.out.println("  java -jar mc-server.jar --lang zh_CN|en_US Set language");
            System.out.println("  java -jar mc-server.jar --scan-only        Scan plugins and exit");
            System.out.println("  java -jar mc-server.jar --shell vanilla    Shell mode: download and run official server");
            System.out.println("  java -jar mc-server.jar --shell vanilla --version 1.21.8");
            System.out.println("  java -jar mc-server.jar --help             Show this help");
            System.out.println();
            System.out.println("Two modes:");
            System.out.println("  Kernel mode (default): self-built Bukkit API, loads plugin JARs from plugins/");
            System.out.println("  Shell mode (--shell): runs the real official server.jar, console output translated");
            System.out.println();
            System.out.println("Plugins go in <dir>/plugins/, supported:");
            System.out.println("  Bukkit / Paper plugins (plugin.yml, paper-plugin.yml) — Runnable");
            System.out.println("  Fabric mods (fabric.mod.json) — Identified Only");
            System.out.println("  Forge mods (META-INF/mods.toml) — Identified Only");
        }
    }
}