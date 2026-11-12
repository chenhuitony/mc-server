package com.mc;

import com.mc.server.McServer;
import com.mc.util.Log;

import java.io.File;

/**
 * 入口。
 *
 * <p>用法：
 * <pre>
 *   java -jar mc-server.jar                  在当前目录启动
 *   java -jar mc-server.jar --dir myserver   指定运行目录
 *   java -jar mc-server.jar --scan-only      只扫描插件并退出
 * </pre>
 */
public class Main {

    public static void main(String[] args) {
        File dir = new File(".");
        boolean scanOnly = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dir":
                    if (i + 1 < args.length) dir = new File(args[++i]);
                    break;
                case "--scan-only":
                    scanOnly = true;
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
            Log.error("服务端异常退出：" + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Minecraft 服务端内核 v" + McServer.VERSION);
        System.out.println();
        System.out.println("用法：");
        System.out.println("  java -jar mc-server.jar                在当前目录启动");
        System.out.println("  java -jar mc-server.jar --dir <目录>   指定运行目录");
        System.out.println("  java -jar mc-server.jar --scan-only    只扫描插件后退出");
        System.out.println("  java -jar mc-server.jar --help         显示本帮助");
        System.out.println();
        System.out.println("插件放在 <目录>/plugins/ 下，支持：");
        System.out.println("  Bukkit / Paper 插件（plugin.yml、paper-plugin.yml）—— 可运行");
        System.out.println("  Fabric 模组（fabric.mod.json）—— 仅识别");
        System.out.println("  Forge 模组（META-INF/mods.toml）—— 仅识别");
    }
}