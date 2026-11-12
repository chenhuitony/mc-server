package org.bukkit.configuration.file;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 文件配置（Bukkit 的 FileConfiguration）。
 *
 * <p>在 MemorySection 之上增加文件读写能力。宿主的插件
 * {@code getConfig()} 返回的就是本类实例。
 */
public class FileConfiguration extends MemorySection {

    private File file;
    private String header;
    private final FileConfigurationOptions options = new FileConfigurationOptions();

    public FileConfiguration(File file) {
        super();
        this.file = file;
    }

    /** 获取配置选项。 */
    public FileConfigurationOptions options() {
        return options;
    }

    /** 设置默认值并让后续保存包含它们。 */
    public void setDefaults(FileConfiguration defaults) {
        if (defaults != null) {
            for (java.util.Map.Entry<String, Object> e : defaults.getValues(true).entrySet()) {
                addDefault(e.getKey(), e.getValue());
            }
        }
    }

    public File getFile() { return file; }

    public void setFile(File f) { this.file = f; }

    public String getHeader() { return header; }

    public void setHeader(String h) { this.header = h; }

    /** 从文件加载。 */
    public void load() throws Exception {
        if (file == null || !file.exists()) return;
        Map<String, Object> parsed = com.mc.util.YamlParser.asMap(
                com.mc.util.YamlParser.parse(com.mc.util.IoUtil.readLines(file)));
        applyMap(parsed);
    }

    @SuppressWarnings("unchecked")
    private void applyMap(Map<String, Object> m) {
        applyMapInternal(m);
    }

    /** 保存到文件。 */
    public void save() throws Exception {
        StringBuilder sb = new StringBuilder();
        if (header != null && !header.isEmpty()) {
            for (String line : header.split("\n")) sb.append("# ").append(line).append("\n");
        }
        writeMap(sb, getRawMap(), 0);
        com.mc.util.IoUtil.writeAll(file, sb.toString());
    }

    /** 保存到指定文件。 */
    public void save(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        if (header != null && !header.isEmpty()) {
            for (String line : header.split("\n")) sb.append("# ").append(line).append("\n");
        }
        writeMap(sb, getRawMap(), 0);
        com.mc.util.IoUtil.writeAll(file, sb.toString());
    }

    @SuppressWarnings("unchecked")
    private void writeMap(StringBuilder sb, Map<String, Object> m, int indent) {
        String pad = "  ".repeat(indent);
        for (Map.Entry<String, Object> e : m.entrySet()) {
            Object v = e.getValue();
            if (v instanceof MemorySection) {
                sb.append(pad).append(e.getKey()).append(":\n");
                writeMap(sb, ((MemorySection) v).getRawMap(), indent + 1);
            } else if (v instanceof Map) {
                sb.append(pad).append(e.getKey()).append(":\n");
                writeMap(sb, (Map<String, Object>) v, indent + 1);
            } else if (v instanceof List) {
                sb.append(pad).append(e.getKey()).append(":\n");
                for (Object o : (List<Object>) v) {
                    sb.append(pad).append("  - ").append(formatScalar(o)).append("\n");
                }
            } else {
                sb.append(pad).append(e.getKey()).append(": ").append(formatScalar(v)).append("\n");
            }
        }
    }

    private String formatScalar(Object o) {
        if (o == null) return "null";
        String s = String.valueOf(o);
        boolean needQuote = s.isEmpty() || s.contains(": ") || s.contains("#")
                || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")
                || s.matches("[-+]?\\d+(\\.\\d+)?");
        return needQuote ? "\"" + s.replace("\"", "\\\"") + "\"" : s;
    }

    /** 深度合并默认值。 */
    public void addDefaults(Map<String, Object> defaults) {
        for (Map.Entry<String, Object> e : defaults.entrySet()) {
            addDefault(e.getKey(), e.getValue());
        }
    }
}