package com.mc.server;

import com.mc.plugin.LoadedPlugin;
import com.mc.util.I18n;
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
    private final com.mc.util.ServerConfig config;
    private final PlayerListManager playerLists;
    private volatile boolean running;
    private long tick;
    private String name = "Minecraft Server";
    private final List<String> consoleQueue = new ArrayList<>();
    private final long startedAt = System.currentTimeMillis();

    public McServer(File rootDir) {
        this.rootDir = rootDir;
        this.config = new com.mc.util.ServerConfig(new File(rootDir, "server.properties"));
        this.playerLists = new PlayerListManager(rootDir);
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

        Log.info("server.starting");
        Log.info("server.version", VERSION);
        Log.info("server.target", MINECRAFT_TARGET);
        Log.info("server.dir", rootDir.getAbsolutePath());
        Log.info("server.ending");

        // 先扫描并报告各生态插件的情况
        com.mc.loader.PluginScanner scanner = new com.mc.loader.PluginScanner();
        com.mc.loader.PluginScanner.ScanResult scan =
                scanner.scan(new File(rootDir, "plugins"));
        if (!scan.byEcosystem.isEmpty() || !scan.unknown.isEmpty()) {
            scanner.printReport(scan);
        }

        pluginManager.loadPlugins();

        Log.info("server.started");
        runMainLoop();
    }

    private void loadProperties() {
        config.load();
        playerLists.load();
        name = config.getString("motd", "A Minecraft Server");
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

    private void handleConsoleCommand(String raw) {
        String line = raw.trim();
        if (line.isEmpty()) return;
        if (line.startsWith("/")) line = line.substring(1);

        if (line.equalsIgnoreCase("plugins") || line.equalsIgnoreCase("pl")) {
            listPlugins();
            return;
        }
        if (line.equalsIgnoreCase("version") || line.equalsIgnoreCase("ver")) {
            Log.info("cmd.version", VERSION, MINECRAFT_TARGET, System.getProperty("java.version"));
            return;
        }
        if (line.equalsIgnoreCase("help") || line.equalsIgnoreCase("?")) {
            printHelp();
            return;
        }
        if (line.equalsIgnoreCase("tps")) {
            Log.info("cmd.tps", tick, scheduler.getActiveTaskCount(), pluginManager.getHandlerCount());
            return;
        }
        if (line.equalsIgnoreCase("say") || line.startsWith("say ")) {
            String msg = line.length() > 3 ? line.substring(3).trim() : "";
            broadcastMessage(msg);
            return;
        }
        if (handleVanillaCommand(line)) return;

        if (pluginManager.dispatchCommand(line)) return;

        Log.warn("cmd.unknown", line);
    }

    /**
     * 原版服务端风格的指令集。
     *
     * <p>这些指令不涉及世界/实体，但语义、反馈文本与原版对齐，
     * 让本内核在管理层面「像一个原版服务端」。
     */
    private boolean handleVanillaCommand(String line) {
        String[] parts = line.split("\\s+");
        String cmd = parts[0].toLowerCase();
        String arg1 = parts.length > 1 ? parts[1] : null;

        switch (cmd) {
            case "list": {
                Log.info("vanilla.list", 0, config.getInt("max-players", 20));
                return true;
            }
            case "seed": {
                String seed = config.getString("level-seed");
                Log.info("vanilla.seed", seed.isEmpty() ? I18n.get("vanilla.seed_random") : seed);
                return true;
            }
            case "time": {
                long t = tick;
                long day = (t / 24000L) % 1;
                Log.info("vanilla.time", t, (int) (day * 24));
                return true;
            }
            case "difficulty": {
                if (arg1 == null) {
                    Log.info("vanilla.difficulty_get", config.getString("difficulty"));
                } else if (com.mc.util.ServerConfig.DIFFICULTIES.contains(arg1.toLowerCase())) {
                    config.set("difficulty", arg1.toLowerCase());
                    config.save();
                    Log.info("vanilla.difficulty_set", arg1.toLowerCase());
                } else {
                    Log.warn("vanilla.bad_value", arg1, "peaceful|easy|normal|hard");
                }
                return true;
            }
            case "gamemode": {
                if (arg1 == null) {
                    Log.info("vanilla.gamemode_get", config.getString("gamemode"));
                } else if (com.mc.util.ServerConfig.GAMEMODES.contains(arg1.toLowerCase())
                        || arg1.matches("[0-3]")) {
                    String gm = switch (arg1.toLowerCase()) {
                        case "0", "survival" -> "survival";
                        case "1", "creative" -> "creative";
                        case "2", "adventure" -> "adventure";
                        case "3", "spectator" -> "spectator";
                        default -> config.getString("gamemode");
                    };
                    config.set("gamemode", gm);
                    config.save();
                    Log.info("vanilla.gamemode_set", gm);
                } else {
                    Log.warn("vanilla.bad_value", arg1, "survival|creative|adventure|spectator");
                }
                return true;
            }
            case "defaultgamemode": {
                if (arg1 != null) {
                    config.set("gamemode", arg1.toLowerCase());
                    config.save();
                    Log.info("vanilla.gamemode_set", arg1.toLowerCase());
                }
                return true;
            }
            case "setworldspawn":
            case "spawnpoint": {
                Log.info("vanilla.unsupported_world", cmd);
                return true;
            }
            case "gamerule": {
                if (arg1 == null) {
                    Log.info("vanilla.gamerule_list",
                            "keepInventory, doDaylightCycle, doMobSpawning, doFireTick, "
                                    + "mobGriefing, naturalRegeneration, randomTickSpeed, sendCommandFeedback");
                } else if (parts.length > 2) {
                    Log.info("vanilla.gamerule_set", arg1, parts[2]);
                } else {
                    Log.info("vanilla.gamerule_get", arg1);
                }
                return true;
            }
            case "op": {
                if (arg1 == null) { Log.warn("vanilla.need_player"); return true; }
                if (playerLists.isOp(arg1)) {
                    Log.info("vanilla.op_already", arg1);
                } else {
                    playerLists.addOp(arg1);
                    Log.info("vanilla.op_add", arg1);
                }
                return true;
            }
            case "deop": {
                if (arg1 == null) { Log.warn("vanilla.need_player"); return true; }
                if (playerLists.isOp(arg1)) {
                    playerLists.removeOp(arg1);
                    Log.info("vanilla.op_remove", arg1);
                } else {
                    Log.info("vanilla.op_not", arg1);
                }
                return true;
            }
            case "whitelist": {
                String sub = arg1 == null ? "list" : arg1.toLowerCase();
                switch (sub) {
                    case "on" -> {
                        config.set("white-list", "true");
                        config.save();
                        Log.info("vanilla.whitelist_on");
                    }
                    case "off" -> {
                        config.set("white-list", "false");
                        config.save();
                        Log.info("vanilla.whitelist_off");
                    }
                    case "list" -> {
                        java.util.Set<String> wl = playerLists.getWhitelist();
                        Log.info("vanilla.whitelist_list", wl.size(),
                                wl.isEmpty() ? "-" : String.join(", ", wl));
                    }
                    case "add" -> {
                        if (parts.length > 2) {
                            playerLists.addWhitelist(parts[2]);
                            Log.info("vanilla.whitelist_add", parts[2]);
                        }
                    }
                    case "remove" -> {
                        if (parts.length > 2) {
                            playerLists.removeWhitelist(parts[2]);
                            Log.info("vanilla.whitelist_remove", parts[2]);
                        }
                    }
                    case "reload" -> Log.info("vanilla.whitelist_reloaded");
                    default -> Log.warn("vanilla.bad_value", sub, "on|off|list|add|remove|reload");
                }
                return true;
            }
            case "ban": {
                if (arg1 == null) { Log.warn("vanilla.need_player"); return true; }
                String reason = parts.length > 2
                        ? String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length))
                        : null;
                playerLists.ban(arg1, reason);
                Log.info("vanilla.ban_add", arg1);
                return true;
            }
            case "pardon": {
                if (arg1 == null) { Log.warn("vanilla.need_player"); return true; }
                playerLists.unban(arg1);
                Log.info("vanilla.ban_remove", arg1);
                return true;
            }
            case "banlist": {
                Log.info("vanilla.banlist", playerLists.getBannedPlayers().size(),
                        playerLists.getBannedPlayers().isEmpty() ? "-"
                                : String.join(", ", playerLists.getBannedPlayers()));
                return true;
            }
            case "ban-ip": {
                if (arg1 == null) { Log.warn("vanilla.need_ip"); return true; }
                playerLists.banIp(arg1, null);
                Log.info("vanilla.banip_add", arg1);
                return true;
            }
            case "pardon-ip": {
                if (arg1 == null) { Log.warn("vanilla.need_ip"); return true; }
                playerLists.unbanIp(arg1);
                Log.info("vanilla.banip_remove", arg1);
                return true;
            }
            case "kick": {
                if (arg1 == null) { Log.warn("vanilla.need_player"); return true; }
                Log.info("vanilla.kick", arg1);
                return true;
            }
            case "save-all": {
                config.save();
                Log.info("vanilla.saved");
                return true;
            }
            case "save-off":
            case "save-on": {
                Log.info("vanilla.save_toggle", cmd);
                return true;
            }
            case "reload": {
                reload();
                return true;
            }
            case "stop":
            case "exit":
            case "quit": {
                Log.info("server.shutdown");
                shutdown();
                return true;
            }
            case "status": {
                printStatus();
                return true;
            }
            case "me": {
                String msg = line.length() > 3 ? line.substring(3).trim() : "";
                Log.raw("* CONSOLE " + msg);
                return true;
            }
            case "tell":
            case "msg":
            case "w": {
                if (parts.length > 2) {
                    Log.info("vanilla.tell", parts[1],
                            String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length)));
                }
                return true;
            }
            case "title":
            case "playsound":
            case "particle":
            case "effect":
            case "summon":
            case "give":
            case "tp":
            case "teleport":
            case "weather": {
                Log.info("vanilla.needs_world", cmd);
                return true;
            }
            default:
                return false;
        }
    }

    /** 打印运行状态（/status）。 */
    private void printStatus() {
        long up = System.currentTimeMillis() - startedAt;
        Log.info("status.header");
        Log.raw("  " + I18n.get("status.version",
                VERSION, MINECRAFT_TARGET, System.getProperty("java.version")));
        Log.raw("  " + I18n.get("status.uptime", formatUptime(up)));
        Log.raw("  " + I18n.get("status.tick", tick, scheduler.getActiveTaskCount()));
        Log.raw("  " + I18n.get("status.events", pluginManager.getHandlerCount()));
        Log.raw("  " + I18n.get("status.plugins", pluginManager.getPlugins().size()));
        Log.raw("  " + I18n.get("status.motd", name));
        Log.raw("  " + I18n.get("status.port",
                config.getInt("server-port", 25565), config.getString("server-ip", "")));
        Log.raw("  " + I18n.get("status.mode",
                config.getBoolean("online-mode", true)
                        ? I18n.get("status.mode_online") : I18n.get("status.mode_offline")));
        Log.raw("  " + I18n.get("status.difficulty", config.getString("difficulty"),
                config.getString("gamemode")));
        Log.raw("  " + I18n.get("status.max_players", config.getInt("max-players", 20)));
        Log.raw("  " + I18n.get("status.lists",
                playerLists.getOps().size(), playerLists.getWhitelist().size(),
                playerLists.getBannedPlayers().size()));
        Log.info("status.footer");
    }

    private static String formatUptime(long ms) {
        long s = ms / 1000;
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return String.format("%02d:%02d:%02d", h, m, sec);
    }

    private void listPlugins() {
        Collection<LoadedPlugin> all = pluginManager.getAllLoaded();
        if (all.isEmpty()) {
            Log.info(I18n.get("cmd.plugins", 0));
            return;
        }
        Log.info("cmd.plugins", all.size());
        for (LoadedPlugin lp : all) {
            String state = lp.isEnabled() ? I18n.get("load.enabled_state") : I18n.get("load.disabled_state");
            if (!lp.isUsable()) state = I18n.get("load.failed_state");
            Log.raw("  - " + lp.getName() + " v"
                    + (lp.getDescription() == null ? "?" : lp.getDescription().getVersion())
                    + " [" + state + "]");
        }
    }

    private void printHelp() {
        Log.info("cmd.help");
        Log.raw("  " + I18n.get("help.info"));
        Log.raw("  " + I18n.get("help.plugins"));
        Log.raw("  " + I18n.get("help.world"));
        Log.raw("  " + I18n.get("help.players"));
        Log.raw("  " + I18n.get("help.server"));
        Log.raw("  " + I18n.get("help.chat"));
        Log.raw("  " + I18n.get("help.other"));
        List<String> cmds = pluginManager.getRegisteredCommands();
        if (!cmds.isEmpty()) {
            Log.raw("  " + I18n.get("cmd.help.plugin_cmds", String.join(", ", cmds)));
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
        Log.raw(I18n.get("broadcast.prefix") + msg);
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
        Log.info("server.shutdown");
        pluginManager.disableAll();
        scheduler.shutdown();
        Log.info("server.closed");
    }

    public void reload() {
        Log.info("log.reload");
        pluginManager.disableAll();
        pluginManager.loadPlugins();
        Log.info("log.reloaded");
    }

    // ---------- 附加访问器 ----------

    public File getRootDir() { return rootDir; }
    @Override public McScheduler getScheduler() { return scheduler; }
    public ConsoleSender getConsoleSender() { return consoleSender; }
    public PluginManagerImpl getPluginManagerImpl() { return pluginManager; }
}