package org.bukkit.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件元数据（对应 plugin.yml / paper-plugin.yml）。
 *
 * <p>Paper 的 paper-plugin.yml 与 Bukkit 的 plugin.yml 字段高度重合，
 * 主要差异是主类字段名为 {@code main} 与 {@code bootstrap}，
 * 以及依赖写法更结构化。这里统一解析成同一份模型。
 */
public class PluginDescriptionFile {

    private String name;
    private String version = "1.0.0";
    private String description = "";
    private String mainClass;
    private String apiVersion;
    private String website = "";
    private String authors = "";
    private final List<String> depend = new ArrayList<>();
    private final List<String> softDepend = new ArrayList<>();
    private final List<String> loadBefore = new ArrayList<>();
    private final Map<String, Map<String, Object>> commands = new LinkedHashMap<>();
    private final Map<String, Object> raw = new LinkedHashMap<>();
    private boolean foliaSupported;
    private File sourceFile;

    @SuppressWarnings("unchecked")
    public static PluginDescriptionFile parse(Map<String, Object> yml, File source) {
        PluginDescriptionFile d = new PluginDescriptionFile();
        d.sourceFile = source;
        d.raw.putAll(yml);

        d.name = str(yml, "name");
        if (d.name == null) d.name = source.getName().replaceAll("\\.jar$", "");
        d.version = orDefault(str(yml, "version"), "1.0.0");
        d.description = orDefault(str(yml, "description"), "");
        d.mainClass = str(yml, "main");
        if (d.mainClass == null) d.mainClass = str(yml, "bootstrap");
        d.apiVersion = str(yml, "api-version");
        d.website = orDefault(str(yml, "website"), "");

        Object authorsObj = yml.get("authors");
        if (authorsObj == null) authorsObj = yml.get("author");
        if (authorsObj instanceof List) {
            List<String> as = new ArrayList<>();
            for (Object o : (List<Object>) authorsObj) if (o != null) as.add(String.valueOf(o));
            d.authors = String.join(", ", as);
        } else if (authorsObj != null) {
            d.authors = String.valueOf(authorsObj);
        }

        d.depend.addAll(strList(yml, "depend"));
        d.softDepend.addAll(strList(yml, "softdepend"));
        if (d.softDepend.isEmpty()) d.softDepend.addAll(strList(yml, "soft-depend"));
        d.loadBefore.addAll(strList(yml, "loadbefore"));
        d.foliaSupported = bool(yml, "folia-supported", false);

        Object cmds = yml.get("commands");
        if (cmds instanceof Map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) cmds).entrySet()) {
                Map<String, Object> meta = e.getValue() instanceof Map
                        ? (Map<String, Object>) e.getValue()
                        : new LinkedHashMap<>();
                d.commands.put(e.getKey().toLowerCase(), meta);
            }
        }
        return d;
    }

    /** 校验元数据是否可用于加载。 */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) errors.add("缺少 name");
        if (mainClass == null || mainClass.trim().isEmpty()) errors.add("缺少 main（Bootstrap 主类）");
        return errors;
    }

    // ---------- getters ----------

    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public String getMainClass() { return mainClass; }
    public String getApiVersion() { return apiVersion; }
    public String getWebsite() { return website; }
    public String getAuthors() { return authors; }
    public List<String> getDepend() { return Collections.unmodifiableList(depend); }
    public List<String> getSoftDepend() { return Collections.unmodifiableList(softDepend); }
    public List<String> getLoadBefore() { return Collections.unmodifiableList(loadBefore); }
    public Map<String, Map<String, Object>> getCommands() { return Collections.unmodifiableMap(commands); }
    public boolean isFoliaSupported() { return foliaSupported; }
    public File getSourceFile() { return sourceFile; }
    public Map<String, Object> getRaw() { return Collections.unmodifiableMap(raw); }

    /** 展示用全名，如 LuckPerms v5.5.71。 */
    public String getFullName() {
        return name + " v" + version;
    }

    private static String str(Map<String, Object> m, String k) {
        Object o = m.get(k);
        return o == null ? null : String.valueOf(o);
    }

    private static String orDefault(String v, String def) {
        return v == null || v.isEmpty() ? def : v;
    }

    @SuppressWarnings("unchecked")
    private static List<String> strList(Map<String, Object> m, String k) {
        List<String> out = new ArrayList<>();
        Object o = m.get(k);
        if (o instanceof List) {
            for (Object x : (List<Object>) o) if (x != null) out.add(String.valueOf(x));
        } else if (o != null) {
            String s = String.valueOf(o).trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private static boolean bool(Map<String, Object> m, String k, boolean def) {
        Object o = m.get(k);
        if (o == null) return def;
        if (o instanceof Boolean) return (Boolean) o;
        return Boolean.parseBoolean(String.valueOf(o));
    }

    @Override
    public String toString() {
        return getFullName();
    }
}