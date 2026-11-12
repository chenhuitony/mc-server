package org.bukkit.configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ConfigurationSection 的内存实现。
 *
 * <p>用嵌套 Map 表示层级，{@code set("a.b.c", v)} 会自动创建中间层。
 */
public class MemorySection implements ConfigurationSection {

    protected final Map<String, Object> map = new LinkedHashMap<>();
    private final String path;
    private final ConfigurationSection parent;
    private final Map<String, Object> defaults = new LinkedHashMap<>();

    public MemorySection() {
        this("", null);
    }

    public MemorySection(String path, ConfigurationSection parent) {
        this.path = path;
        this.parent = parent;
    }

    // ---------- 基础 ----------

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getKeys(boolean deep) {
        Set<String> out = new LinkedHashSet<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            out.add(e.getKey());
            if (deep && e.getValue() instanceof MemorySection) {
                for (String sub : ((MemorySection) e.getValue()).getKeys(true)) {
                    out.add(e.getKey() + "." + sub);
                }
            }
        }
        return out;
    }

    @Override
    public Map<String, Object> getValues(boolean deep) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            Object v = e.getValue();
            if (deep && v instanceof MemorySection) {
                out.putAll(prefix(e.getKey(), ((MemorySection) v).getValues(true)));
            } else {
                out.put(e.getKey(), simplify(v));
            }
        }
        return out;
    }

    private Map<String, Object> prefix(String p, Map<String, Object> m) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : m.entrySet()) out.put(p + "." + e.getKey(), e.getValue());
        return out;
    }

    private Object simplify(Object v) {
        return v instanceof MemorySection ? ((MemorySection) v).getValues(true) : v;
    }

    @Override
    public boolean contains(String path) {
        return get(path) != null || isSet(path);
    }

    @Override
    public boolean isSet(String path) {
        return get(path) != null;
    }

    @Override
    public String getCurrentPath() { return path; }

    @Override
    public String getName() {
        if (path.isEmpty()) return "";
        int i = path.lastIndexOf('.');
        return i < 0 ? path : path.substring(i + 1);
    }

    @Override
    public ConfigurationSection getParent() { return parent; }

    // ---------- 读写 ----------

    @Override
    public Object get(String path) {
        String[] parts = path.split("\\.");
        Object cur = this;
        for (String p : parts) {
            if (!(cur instanceof MemorySection)) return defaults.get(path);
            Object next = ((MemorySection) cur).map.get(p);
            if (next == null) return defaults.get(path);
            cur = next;
        }
        return cur;
    }

    @Override
    public Object get(String path, Object def) {
        Object v = get(path);
        return v == null ? def : v;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(String path, Object value) {
        String[] parts = path.split("\\.");
        ConfigurationSection cur = this;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = cur.get(parts[i]);
            if (!(next instanceof ConfigurationSection)) {
                next = cur.createSection(parts[i]);
            }
            cur = (ConfigurationSection) next;
        }
        ((MemorySection) cur).map.put(parts[parts.length - 1], value);
    }

    @Override
    public ConfigurationSection createSection(String path) {
        String[] parts = path.split("\\.");
        ConfigurationSection cur = this;
        for (String p : parts) {
            Object next = cur.get(p);
            if (!(next instanceof ConfigurationSection)) {
                String childPath = cur.getCurrentPath().isEmpty()
                        ? p : cur.getCurrentPath() + "." + p;
                MemorySection child = new MemorySection(childPath, cur);
                ((MemorySection) cur).map.put(p, child);
                next = child;
            }
            cur = (ConfigurationSection) next;
        }
        return cur;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationSection createSection(String path, Map<?, ?> values) {
        ConfigurationSection s = createSection(path);
        for (Map.Entry<?, ?> e : values.entrySet()) {
            s.set(String.valueOf(e.getKey()), e.getValue());
        }
        return s;
    }

    // ---------- 类型化访问 ----------

    @Override
    public String getString(String path) {
        Object v = get(path);
        return v == null ? null : String.valueOf(v);
    }

    @Override
    public String getString(String path, String def) {
        String v = getString(path);
        return v == null ? def : v;
    }

    @Override
    public boolean isString(String path) {
        return get(path) instanceof String;
    }

    @Override
    public int getInt(String path) {
        return getInt(path, 0);
    }

    @Override
    public int getInt(String path, int def) {
        Object v = get(path);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v != null) {
            try { return Integer.parseInt(String.valueOf(v).trim()); } catch (Exception ignore) { }
        }
        return def;
    }

    @Override
    public boolean isInt(String path) {
        Object v = get(path);
        return v instanceof Number || (v != null && String.valueOf(v).matches("[-+]?\\d+"));
    }

    @Override
    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        Object v = get(path);
        if (v instanceof Boolean) return (Boolean) v;
        if (v != null) return Boolean.parseBoolean(String.valueOf(v).trim());
        return def;
    }

    @Override
    public boolean isBoolean(String path) {
        Object v = get(path);
        if (v instanceof Boolean) return true;
        if (v == null) return false;
        String s = String.valueOf(v).trim();
        return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false");
    }

    @Override
    public double getDouble(String path) {
        return getDouble(path, 0);
    }

    @Override
    public double getDouble(String path, double def) {
        Object v = get(path);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v != null) {
            try { return Double.parseDouble(String.valueOf(v).trim()); } catch (Exception ignore) { }
        }
        return def;
    }

    @Override
    public boolean isDouble(String path) {
        Object v = get(path);
        return v instanceof Number
                || (v != null && String.valueOf(v).matches("[-+]?\\d*\\.\\d+([eE][-+]?\\d+)?"));
    }

    @Override
    public long getLong(String path) {
        return getLong(path, 0);
    }

    @Override
    public long getLong(String path, long def) {
        Object v = get(path);
        if (v instanceof Number) return ((Number) v).longValue();
        if (v != null) {
            try { return Long.parseLong(String.valueOf(v).trim()); } catch (Exception ignore) { }
        }
        return def;
    }

    @Override
    public boolean isLong(String path) {
        Object v = get(path);
        return v instanceof Number || (v != null && String.valueOf(v).matches("[-+]?\\d+"));
    }

    @Override
    public List<?> getList(String path) {
        return getList(path, new ArrayList<>());
    }

    @Override
    public List<?> getList(String path, List<?> def) {
        Object v = get(path);
        return v instanceof List ? (List<?>) v : def;
    }

    @Override
    public boolean isList(String path) {
        return get(path) instanceof List;
    }

    @Override
    public List<String> getStringList(String path) {
        List<String> out = new ArrayList<>();
        List<?> raw = getList(path);
        for (Object o : raw) if (o != null) out.add(String.valueOf(o));
        return out;
    }

    @Override
    public ConfigurationSection getConfigurationSection(String path) {
        Object v = get(path);
        return v instanceof ConfigurationSection ? (ConfigurationSection) v : null;
    }

    @Override
    public boolean isConfigurationSection(String path) {
        return get(path) instanceof ConfigurationSection;
    }

    @Override
    public void addDefault(String path, Object value) {
        defaults.put(path, value);
    }

    /** 全部直接子节点（供序列化使用）。 */
    public Map<String, Object> getRawMap() {
        return map;
    }

    /** 从解析好的嵌套 Map 批量填充（供 FileConfiguration 使用）。 */
    @SuppressWarnings("unchecked")
    public void applyMapInternal(Map<String, Object> m) {
        for (Map.Entry<String, Object> e : m.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Map) {
                ConfigurationSection s = createSection(e.getKey());
                ((MemorySection) s).applyMapInternal((Map<String, Object>) v);
            } else {
                map.put(e.getKey(), v);
            }
        }
    }
}