package com.mc.server;

import com.mc.plugin.LoadedPlugin;
import com.mc.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 服务器主体：组装插件管理器、调度器、控制台发送者，
 * 并提供 20 TPS 主循环与控制台指令读入。
 *
 * <p>本内核不实现网络协议与游戏世界，是一个「插件宿主」：
 * 负责插件加载、生命周期、事件派发、指令路由、任务调度。
 * 因此它能运行不依赖 NMS 的插件（纯 API 插件），
 * 而依赖游戏世界的插件会在初始化时明确报错。
 */
public class McServer implements Server {

    public static final String VERSION = "1.0.0";
    public static final String MINECRAFT_TARGET = "26.3";

    private final File rootDir;
    private final UUID serverId = UUID.randomUUID();
    private final Logger logger = Logger.getLogger("Minecraft");
    private final PluginManagerImpl pluginManager;
    private final McScheduler scheduler = new McScheduler();
    private final ServicesManagerImpl servicesManager = new ServicesManagerImpl();
    private final ConsoleSender consoleSender;
    private volatile boolean running;
    private long tick;
    private String name = "Minecraft Server";
    private final List<String> consoleQueue = new ArrayList<>();

    public McServer(File rootDir) {
        this.rootDir = rootDir;
        this.pluginManager = new PluginManagerImpl(this);
        this.consoleSender = new ConsoleSender(this);
        // 注入静态 Bukkit.getServer()
        Bukkit.setServer(this);
    }

    // ---------- 启动 ----------

    public void start() {
        rootDir.mkdirs();
        loadProperties();
        running = true;

        Log.info("======================================");
        Log.info("  Minecraft 服务端内核 v" + VERSION);
        Log.info("  目标版本 MC " + MINECRAFT_TARGET);
        Log.info("  运行目录 " + rootDir.getAbsolutePath());
        Log.info("======================================");

        // 先扫描并报告各生态插件的情况
        com.mc.loader.PluginScanner scanner = new com.mc.loader.PluginScanner();
        com.mc.loader.PluginScanner.ScanResult scan =
                scanner.scan(new File(rootDir, "plugins"));
        if (!scan.byEcosystem.isEmpty() || !scan.unknown.isEmpty()) {
            scanner.printReport(scan);
        }

        pluginManager.loadPlugins();

        Log.info("服务端已启动，输入 help 查看可用指令，stop 关闭");
        runMainLoop();
    }

    private void loadProperties() {
        File props = new File(rootDir, "server.properties");
        if (!props.exists()) {
            try {
                com.mc.util.IoUtil.writeAll(props,
                        "motd=一个 Minecraft 服务端\n"
                                + "server-port=25565\n"
                                + "max-players=20\n"
                                + "online-mode=true\n"
                                + "view-distance=10\n");
            } catch (Exception ignore) { }
            return;
        }
        try {
            for (String line : com.mc.util.IoUtil.readLines(props)) {
                if (line.startsWith("motd=")) {
                    name = line.substring("motd=".length()).trim();
                }
            }
        } catch (Exception ignore) { }
    }

    /** 20 TPS 主循环。 */
    private void runMainLoop() {
        long lastTick = System.currentTimeMillis();
        long tickNs = 50_000_000L;
        long next = System.nanoTime();

        java.io.BufferedReader console = new java.io.BufferedReader(
                new java.io.InputStreamReader(System.in));

        Thread reader = new Thread(() -> {
            try {
                String line;
                while (running && (line = console.readLine()) != null) {
                    synchronized (consoleQueue) {
                        consoleQueue.add(line);
                    }
                }
            } catch (Exception ignore) { }
        }, "MC-Console");
        reader.setDaemon(true);
        reader.start();

        while (running) {
            next += tickNs;
            scheduler.tick();
            tick++;

            List<String> pending;
            synchronized (consoleQueue) {
                pending = new ArrayList<>(consoleQueue);
                consoleQueue.clear();
            }
            for (String cmd : pending) handleConsoleCommand(cmd);

            long sleepNs = next - System.nanoTime();
            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else if (System.nanoTime() - next > tickNs * 5) {
                next = System.nanoTime();
            }
        }
    }

    /** 处理控制台输入。 */
    private void handleConsoleCommand(String raw) {
        String line = raw.trim();
        if (line.isEmpty()) return;
        if (line.startsWith("/")) line = line.substring(1);

        if (line.equalsIgnoreCase("stop") || line.equalsIgnoreCase("exit")) {
            Log.info("收到关闭指令，正在停止服务端...");
            shutdown();
            return;
        }
        if (line.equalsIgnoreCase("plugins") || line.equalsIgnoreCase("pl")) {
            listPlugins();
            return;
        }
        if (line.equalsIgnoreCase("version") || line.equalsIgnoreCase("ver")) {
            Log.info("服务端内核 v" + VERSION + "，目标 MC " + MINECRAFT_TARGET
                    + "，Java " + System.getProperty("java.version"));
            return;
        }
        if (line.equalsIgnoreCase("help") || line.equalsIgnoreCase("?")) {
            printHelp();
            return;
        }
        if (line.equalsIgnoreCase("tps")) {
            Log.info("当前 tick " + tick + "，活跃任务 " + scheduler.getActiveTaskCount()
                    + "，事件处理器 " + pluginManager.getHandlerCount());
            return;
        }
        if (line.equalsIgnoreCase("say") || line.startsWith("say ")) {
            String msg = line.length() > 3 ? line.substring(3).trim() : "";
            broadcastMessage(msg);
            return;
        }

        if (pluginManager.dispatchCommand(line)) return;

        Log.warn("未知指令：" + line + "（输入 help 查看可用指令）");
    }

    private void listPlugins() {
        Collection<LoadedPlugin> all = pluginManager.getAllLoaded();
        if (all.isEmpty()) {
            Log.info("没有已加载的插件");
            return;
        }
        Log.info("已加载插件（" + all.size() + " 个）：");
        for (LoadedPlugin lp : all) {
            String state = lp.isEnabled() ? "已启用" : "未启用";
            if (!lp.isUsable()) state = "加载失败";
            Log.raw("  - " + lp.getName() + " v"
                    + (lp.getDescription() == null ? "?" : lp.getDescription().getVersion())
                    + " [" + state + "]");
        }
    }

    private void printHelp() {
        Log.info("可用指令：");
        Log.raw("  plugins  列出已加载插件");
        Log.raw("  version  显示版本信息");
        Log.raw("  tps      运行状态统计");
        Log.raw("  say <消息> 广播");
        Log.raw("  help     显示本帮助");
        Log.raw("  stop     关闭服务端");
        List<String> cmds = pluginManager.getRegisteredCommands();
        if (!cmds.isEmpty()) {
            Log.raw("  插件指令：" + String.join(", ", cmds));
        }
    }

    // ---------- Server 接口 ----------

    @Override public String getName() { return name; }
    @Override public String getVersion() { return "Minecraft " + MINECRAFT_TARGET; }

    /**
     * 返回与真实 Bukkit 一致的版本串。
     *
     * <p>插件（如 LuckPerms）会解析这个字符串判断服务端版本，
     * 格式必须是 {@code <MC版本>-R<修订>-SNAPSHOT}，否则会被误判为 1.7/1.8。
     */
    @Override public String getBukkitVersion() { return MINECRAFT_TARGET + "-R0.1-SNAPSHOT"; }
    @Override public Logger getLogger() { return logger; }
    @Override public PluginManager getPluginManager() { return pluginManager; }
    @Override public org.bukkit.plugin.ServicesManager getServicesManager() { return servicesManager; }
    @Override public PluginManager getPluginManagerRef() { return pluginManager; }
    @Override public Plugin getPlugin(String name) { return pluginManager.getPlugin(name); }

    @Override
    public Collection<? extends Plugin> getPlugins() {
        return pluginManager.getPlugins();
    }

    @Override
    public void broadcastMessage(String message, String... args) {
        String msg = args != null && args.length > 0 ? String.format(message, (Object[]) args) : message;
        Log.info("[广播] " + msg);
    }

    @Override
    public Collection<String> getOnlinePlayers() {
        return new ArrayList<>();
    }

    @Override public boolean isRunning() { return running; }
    @Override public long getTickCount() { return tick; }
    @Override public UUID getServerId() { return serverId; }

    @Override
    public void shutdown() {
        if (!running) return;
        running = false;
        Log.info("正在禁用插件...");
        pluginManager.disableAll();
        scheduler.shutdown();
        Log.info("服务端已关闭");
    }

    public void reload() {
        Log.info("重载插件...");
        pluginManager.disableAll();
        pluginManager.loadPlugins();
        Log.info("插件重载完成");
    }

    // ---------- 附加访问器 ----------

    public File getRootDir() { return rootDir; }
    @Override public McScheduler getScheduler() { return scheduler; }
    public ConsoleSender getConsoleSender() { return consoleSender; }
    public PluginManagerImpl getPluginManagerImpl() { return pluginManager; }
}