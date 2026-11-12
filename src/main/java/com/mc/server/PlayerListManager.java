package com.mc.server;

import com.mc.util.IoUtil;
import com.mc.util.Json;
import com.mc.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家名单管理（与原版服务端一致的 JSON 文件格式）。
 *
 * <p>维护 ops.json / whitelist.json / banned-players.json / banned-ips.json，
 * 格式与官方服务端互通，可以直接把已有文件放进来。
 */
public class PlayerListManager {

    private final File rootDir;

    private final Set<String> ops = new LinkedHashSet<>();
    private final Set<String> whitelist = new LinkedHashSet<>();
    private final Set<String> bannedPlayers = new LinkedHashSet<>();
    private final Set<String> bannedIps = new LinkedHashSet<>();

    public PlayerListManager(File rootDir) {
        this.rootDir = rootDir;
    }

    public void load() {
        ops.addAll(readNames(new File(rootDir, "ops.json")));
        whitelist.addAll(readNames(new File(rootDir, "whitelist.json")));
        bannedPlayers.addAll(readNames(new File(rootDir, "banned-players.json")));
        bannedIps.addAll(readStrings(new File(rootDir, "banned-ips.json"), "ip"));
    }

    private List<String> readNames(File f) {
        return readStrings(f, "name");
    }

    private List<String> readStrings(File f, String field) {
        List<String> out = new ArrayList<>();
        if (!f.exists()) return out;
        try {
            Object parsed = Json.parse(IoUtil.readAll(f));
            if (parsed instanceof List) {
                for (Object o : (List<?>) parsed) {
                    if (o instanceof Map) {
                        Object v = ((Map<?, ?>) o).get(field);
                        if (v instanceof String) out.add((String) v);
                    }
                }
            }
        } catch (Exception e) {
            Log.warn("playerlist.read_fail", f.getName(), e.getMessage());
        }
        return out;
    }

    // ---------- ops ----------

    public boolean isOp(String name) {
        return contains(ops, name);
    }

    public void addOp(String name) {
        if (ops.add(name)) saveOps();
    }

    public void removeOp(String name) {
        if (ops.removeIf(n -> n.equalsIgnoreCase(name))) saveOps();
    }

    private void saveOps() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String n : ops) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("uuid", offlineUuid(n));
            e.put("name", n);
            e.put("level", 4);
            e.put("bypassesPlayerLimit", false);
            list.add(e);
        }
        writeJson(new File(rootDir, "ops.json"), list);
    }

    // ---------- whitelist ----------

    public boolean isWhitelisted(String name) {
        return contains(whitelist, name);
    }

    public void addWhitelist(String name) {
        if (whitelist.add(name)) saveWhitelist();
    }

    public void removeWhitelist(String name) {
        if (whitelist.removeIf(n -> n.equalsIgnoreCase(name))) saveWhitelist();
    }

    public Set<String> getWhitelist() {
        return new LinkedHashSet<>(whitelist);
    }

    private void saveWhitelist() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String n : whitelist) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("uuid", offlineUuid(n));
            e.put("name", n);
            list.add(e);
        }
        writeJson(new File(rootDir, "whitelist.json"), list);
    }

    // ---------- bans ----------

    public boolean isBanned(String name) {
        return contains(bannedPlayers, name);
    }

    public void ban(String name, String reason) {
        if (!bannedPlayers.add(name)) return;
        List<Map<String, Object>> list = new ArrayList<>();
        for (String n : bannedPlayers) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("uuid", offlineUuid(n));
            e.put("name", n);
            e.put("created", now());
            e.put("source", "Server");
            e.put("expires", "forever");
            e.put("reason", n.equals(name) && reason != null ? reason : "Banned by an operator");
            list.add(e);
        }
        writeJson(new File(rootDir, "banned-players.json"), list);
    }

    public void unban(String name) {
        if (!bannedPlayers.removeIf(n -> n.equalsIgnoreCase(name))) return;
        List<Map<String, Object>> list = new ArrayList<>();
        for (String n : bannedPlayers) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("uuid", offlineUuid(n));
            e.put("name", n);
            e.put("created", now());
            e.put("source", "Server");
            e.put("expires", "forever");
            e.put("reason", "Banned by an operator");
            list.add(e);
        }
        writeJson(new File(rootDir, "banned-players.json"), list);
    }

    public boolean isIpBanned(String ip) {
        return contains(bannedIps, ip);
    }

    public void banIp(String ip, String reason) {
        if (!bannedIps.add(ip)) return;
        saveIps(reason);
    }

    public void unbanIp(String ip) {
        if (bannedIps.removeIf(s -> s.equalsIgnoreCase(ip))) saveIps(null);
    }

    private void saveIps(String reason) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String ip : bannedIps) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("ip", ip);
            e.put("created", now());
            e.put("source", "Server");
            e.put("expires", "forever");
            e.put("reason", reason == null ? "Banned by an operator" : reason);
            list.add(e);
        }
        writeJson(new File(rootDir, "banned-ips.json"), list);
    }

    // ---------- 查询 ----------

    public Set<String> getOps() { return new LinkedHashSet<>(ops); }
    public Set<String> getBannedPlayers() { return new LinkedHashSet<>(bannedPlayers); }
    public Set<String> getBannedIps() { return new LinkedHashSet<>(bannedIps); }

    public Collection<String> allLists() {
        List<String> out = new ArrayList<>();
        out.add("ops=" + ops.size());
        out.add("whitelist=" + whitelist.size());
        out.add("banned=" + bannedPlayers.size());
        out.add("banned-ips=" + bannedIps.size());
        return out;
    }

    // ---------- 工具 ----------

    private static boolean contains(Set<String> set, String name) {
        if (name == null) return false;
        for (String s : set) {
            if (s.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /** 与原版一致：离线模式的 UUID v3（nameUUIDFromBytes）。 */
    public static String offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(
                java.nio.charset.StandardCharsets.UTF_8)).toString();
    }

    private static String now() {
        return java.time.OffsetDateTime.now()
                .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /** 极简 JSON 写出（只支持本类用到的结构）。 */
    private void writeJson(File f, List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < list.size(); i++) {
            sb.append("  {");
            int j = 0;
            for (Map.Entry<String, Object> e : list.get(i).entrySet()) {
                if (j++ > 0) sb.append(",");
                sb.append("\n    \"").append(escape(e.getKey())).append("\": ");
                Object v = e.getValue();
                if (v instanceof Boolean || v instanceof Number) {
                    sb.append(v);
                } else {
                    sb.append("\"").append(escape(String.valueOf(v))).append("\"");
                }
            }
            sb.append("\n  }");
            if (i < list.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]\n");
        try {
            IoUtil.writeAll(f, sb.toString());
        } catch (Exception e) {
            Log.warn("playerlist.write_fail", f.getName(), e.getMessage());
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}