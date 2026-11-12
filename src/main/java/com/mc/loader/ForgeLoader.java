package com.mc.loader;

import com.mc.plugin.LoadedPlugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Forge 模组加载器。
 *
 * <p>识别标志：JAR 内存在 {@code META-INF/mods.toml} 或 {@code META-INF/neoforge.mods.toml}。
 *
 * <p><b>限制说明（重要）</b>：与 Fabric 相同，本加载器只做识别与元数据解析。
 * Forge 模组依赖 Forge 的补丁化 Minecraft 和事件总线，
 * 脱离真实 Forge 服务端无法运行。
 */
public class ForgeLoader implements PluginLoader {

    private static final String[] TOML_PATHS = {
            "META-INF/neoforge.mods.toml",
            "META-INF/mods.toml"
    };

    @Override
    public String getName() { return "ForgeLoader"; }

    @Override
    public String getEcosystem() { return "Forge / NeoForge"; }

    @Override
    public boolean canLoad(File jarFile) {
        for (String p : TOML_PATHS) {
            if (PaperLoader.hasEntry(jarFile, p)) return true;
        }
        return false;
    }

    @Override
    public LoadedPlugin readMetadata(File jarFile) {
        LoadedPlugin lp = new LoadedPlugin(jarFile);
        try {
            String toml = null;
            for (String p : TOML_PATHS) {
                toml = FabricLoader.readEntry(jarFile, p);
                if (toml != null) break;
            }
            if (toml == null) {
                lp.getErrors().add("缺少 mods.toml");
                return lp;
            }

            Map<String, String> kv = parseToml(toml);
            String modId = kv.getOrDefault("modId", jarFile.getName().replaceAll("\\.jar$", ""));
            String version = kv.getOrDefault("version", "?");
            String displayName = kv.getOrDefault("displayName", modId);

            Map<String, Object> yml = new LinkedHashMap<>();
            yml.put("name", displayName);
            yml.put("version", version);
            yml.put("main", "mods.toml:modId=" + modId);
            yml.put("description", kv.getOrDefault("description", ""));
            PluginDescriptionFile desc = PluginDescriptionFile.parse(yml, jarFile);
            lp.setDescription(desc);
            lp.getWarnings().add("Forge 模组无法在本内核运行（需要补丁化 Minecraft 与 Forge 事件总线）");
        } catch (Exception e) {
            lp.getErrors().add("解析 mods.toml 失败：" + e.getMessage());
        }
        return lp;
    }

    @Override
    public boolean isRunnable() { return false; }

    @Override
    public String getUnsupportedReason() {
        return "Forge/NeoForge 模组依赖补丁化 Minecraft 与 Forge 事件总线，无法脱离运行";
    }

    /** 极简 TOML 解析：只取 [[mods]] 段里的 key = "value"。 */
    private static Map<String, String> parseToml(String toml) {
        Map<String, String> out = new LinkedHashMap<>();
        boolean inMods = false;
        for (String raw : toml.split("\r?\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("[[")) {
                inMods = line.contains("mods");
                continue;
            }
            if (line.startsWith("[")) { inMods = false; continue; }
            if (!inMods) continue;

            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq).trim();
            String val = line.substring(eq + 1).trim();
            if (val.length() >= 2 && val.startsWith("\"") && val.endsWith("\"")) {
                val = val.substring(1, val.length() - 1);
            }
            out.putIfAbsent(key, val);
        }
        return out;
    }
}