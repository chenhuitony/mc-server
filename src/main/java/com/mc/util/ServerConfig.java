package com.mc.util;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * server.properties 配置（与原版服务端一致的键集合）。
 *
 * <p>首次启动时写出带中文注释的默认文件；之后按原版语义解析。
 * 未知键原样保留，避免旧配置被清空。
 */
public final class ServerConfig {

    private final Map<String, String> values = new LinkedHashMap<>();
    private final File file;

    public ServerConfig(File file) {
        this.file = file;
    }

    /** 默认值（键顺序即写文件顺序）。 */
    private static Map<String, String> defaults() {
        Map<String, String> d = new LinkedHashMap<>();
        d.put("server-port", "25565");
        d.put("server-ip", "");
        d.put("motd", "A Minecraft Server");
        d.put("max-players", "20");
        d.put("online-mode", "true");
        d.put("white-list", "false");
        d.put("enforce-whitelist", "false");
        d.put("difficulty", "easy");
        d.put("gamemode", "survival");
        d.put("force-gamemode", "false");
        d.put("hardcore", "false");
        d.put("pvp", "true");
        d.put("allow-flight", "false");
        d.put("allow-nether", "true");
        d.put("allow-cheats", "false");
        d.put("enable-command-block", "false");
        d.put("spawn-protection", "16");
        d.put("view-distance", "10");
        d.put("simulation-distance", "10");
        d.put("level-name", "world");
        d.put("level-seed", "");
        d.put("level-type", "minecraft:normal");
        d.put("max-world-size", "29999984");
        d.put("generate-structures", "true");
        d.put("spawn-monsters", "true");
        d.put("spawn-npcs", "true");
        d.put("spawn-animals", "true");
        d.put("entity-broadcast-range-percentage", "100");
        d.put("max-entity-collisions", "8");
        d.put("sync-chunk-writes", "true");
        d.put("network-compression-threshold", "256");
        d.put("rate-limit", "0");
        d.put("enable-status", "true");
        d.put("hide-online-players", "false");
        d.put("enable-rcon", "false");
        d.put("rcon.port", "25575");
        d.put("rcon.password", "");
        d.put("query.port", "25565");
        d.put("enable-query", "false");
        d.put("op-permission-level", "4");
        d.put("function-permission-level", "2");
        d.put("player-idle-timeout", "0");
        d.put("max-tick-time", "60000");
        d.put("pause-when-empty-seconds", "60");
        d.put("log-ips", "true");
        d.put("text-filtering-config", "");
        d.put("require-resource-pack", "false");
        d.put("resource-pack", "");
        d.put("resource-pack-prompt", "");
        d.put("resource-pack-sha1", "");
        d.put("enable-code-of-conduct", "false");
        d.put("bug-report-link", "");
        d.put("initial-enabled-packs", "vanilla");
        d.put("initial-disabled-packs", "");
        return d;
    }

    /** 键的中文/英文说明，用于生成注释。 */
    private static String commentFor(String key) {
        boolean zh = "zh_CN".equals(I18n.getLanguage());
        Map<String, String[]> m = new LinkedHashMap<>();
        m.put("server-port", new String[]{"服务端监听端口", "Server listen port"});
        m.put("server-ip", new String[]{"绑定 IP，留空表示全部", "Bind IP, empty for all"});
        m.put("motd", new String[]{"服务器列表显示的名称", "Message of the day"});
        m.put("max-players", new String[]{"最大玩家数", "Max players"});
        m.put("online-mode", new String[]{"正版验证", "Verify accounts with Mojang"});
        m.put("white-list", new String[]{"是否启用白名单", "Enable whitelist"});
        m.put("enforce-whitelist", new String[]{"踢出不在白名单的在线玩家", "Kick non-whitelisted players"});
        m.put("difficulty", new String[]{"难度：peaceful/easy/normal/hard", "Difficulty"});
        m.put("gamemode", new String[]{"默认游戏模式：survival/creative/adventure/spectator", "Default gamemode"});
        m.put("force-gamemode", new String[]{"强制玩家使用默认模式", "Force default gamemode"});
        m.put("hardcore", new String[]{"极限模式", "Hardcore mode"});
        m.put("pvp", new String[]{"允许玩家互相伤害", "Allow PvP"});
        m.put("allow-flight", new String[]{"允许飞行（防作弊关闭）", "Allow flight"});
        m.put("allow-nether", new String[]{"允许进入下界", "Allow Nether"});
        m.put("allow-cheats", new String[]{"允许作弊指令", "Allow cheats"});
        m.put("enable-command-block", new String[]{"启用命令方块", "Enable command blocks"});
        m.put("spawn-protection", new String[]{"出生点保护半径（格）", "Spawn protection radius"});
        m.put("view-distance", new String[]{"视距（区块）", "View distance in chunks"});
        m.put("simulation-distance", new String[]{"模拟距离（区块）", "Simulation distance"});
        m.put("level-name", new String[]{"世界目录名", "World directory name"});
        m.put("level-seed", new String[]{"世界种子，留空随机", "World seed, empty for random"});
        m.put("level-type", new String[]{"世界类型", "Level type"});
        m.put("generate-structures", new String[]{"生成结构（村庄等）", "Generate structures"});
        m.put("spawn-monsters", new String[]{"生成怪物", "Spawn monsters"});
        m.put("spawn-animals", new String[]{"生成动物", "Spawn animals"});
        m.put("spawn-npcs", new String[]{"生成村民", "Spawn NPCs"});
        m.put("enable-status", new String[]{"响应服务器列表 Ping", "Respond to server list ping"});
        m.put("hide-online-players", new String[]{"列表隐藏在线玩家", "Hide online players in list"});
        m.put("enable-rcon", new String[]{"启用 RCON 远程控制", "Enable RCON"});
        m.put("rcon.port", new String[]{"RCON 端口", "RCON port"});
        m.put("rcon.password", new String[]{"RCON 密码", "RCON password"});
        m.put("enable-query", new String[]{"启用 Query 协议", "Enable query protocol"});
        m.put("op-permission-level", new String[]{"OP 权限等级 1-4", "OP permission level 1-4"});
        m.put("player-idle-timeout", new String[]{"挂机踢出分钟数，0 关闭", "Idle kick minutes, 0 to disable"});
        m.put("max-tick-time", new String[]{"单 tick 超时毫秒，-1 关闭", "Max tick time ms"});
        m.put("require-resource-pack", new String[]{"强制使用资源包", "Require resource pack"});
        m.put("resource-pack", new String[]{"资源包 URL", "Resource pack URL"});
        m.put("log-ips", new String[]{"记录玩家 IP", "Log player IPs"});
        m.put("max-world-size", new String[]{"世界边界半径", "Max world size"});
        String[] c = m.get(key);
        if (c == null) return null;
        return zh ? c[0] : c[1];
    }

    /** 加载配置；文件不存在则写默认值。 */
    public void load() {
        if (!file.exists()) {
            save();
            return;
        }
        try {
            for (String line : IoUtil.readLines(file)) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#") || t.startsWith("!")) continue;
                int eq = t.indexOf('=');
                if (eq <= 0) continue;
                values.put(t.substring(0, eq).trim(), t.substring(eq + 1).trim());
            }
        } catch (Exception e) {
            Log.warn("config.read_fail", file.getName(), e.getMessage());
        }
        // 补齐缺失键（不覆盖已有值）
        for (Map.Entry<String, String> e : defaults().entrySet()) {
            values.putIfAbsent(e.getKey(), e.getValue());
        }
    }

    /** 写回文件（仅当内容有变化）。 */
    public void save() {
        StringBuilder sb = new StringBuilder();
        boolean zh = "zh_CN".equals(I18n.getLanguage());
        sb.append("#Minecraft server properties\n");
        sb.append(zh ? "#由 mc-server 生成，键名与原版服务端一致\n" : "#Generated by mc-server, keys match vanilla\n");
        Map<String, String> all = new LinkedHashMap<>(defaults());
        all.putAll(values);
        for (Map.Entry<String, String> e : all.entrySet()) {
            String c = commentFor(e.getKey());
            if (c != null) sb.append("# ").append(c).append("\n");
            sb.append(e.getKey()).append("=").append(e.getValue()).append("\n");
        }
        try {
            IoUtil.writeAll(file, sb.toString());
        } catch (Exception e) {
            Log.warn("config.write_fail", file.getName(), e.getMessage());
        }
    }

    // ---------- 取值 ----------

    public String getString(String key) {
        return values.getOrDefault(key, defaults().getOrDefault(key, ""));
    }

    public String getString(String key, String def) {
        String v = values.get(key);
        return v == null || v.isEmpty() ? def : v;
    }

    public int getInt(String key, int def) {
        try {
            return Integer.parseInt(getString(key).trim());
        } catch (Exception e) {
            return def;
        }
    }

    public boolean getBoolean(String key, boolean def) {
        String v = values.get(key);
        if (v == null) return def;
        return "true".equalsIgnoreCase(v.trim());
    }

    public void set(String key, String value) {
        values.put(key, value == null ? "" : value);
    }

    public void toggle(String key) {
        set(key, getBoolean(key, false) ? "false" : "true");
    }

    public Map<String, String> asMap() {
        return new LinkedHashMap<>(values);
    }

    public File getFile() {
        return file;
    }

    /** 难度合法值。 */
    public static final Set<String> DIFFICULTIES = new LinkedHashSet<>(
            java.util.Arrays.asList("peaceful", "easy", "normal", "hard"));

    /** 游戏模式合法值。 */
    public static final Set<String> GAMEMODES = new LinkedHashSet<>(
            java.util.Arrays.asList("survival", "creative", "adventure", "spectator"));
}