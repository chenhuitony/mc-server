package com.mc.loader;

import com.mc.plugin.LoadedPlugin;
import com.mc.util.IoUtil;
import com.mc.util.YamlParser;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fabric 模组加载器。
 *
 * <p>识别标志：JAR 内存在 {@code fabric.mod.json}。
 *
 * <p><b>限制说明（重要）</b>：本加载器只做识别与元数据解析，
 * 不能真正加载运行 Fabric 模组。原因是 Fabric 模组通过
 * {@code Mixin} 在字节码层直接注入 Minecraft 内部类，
 * 必须运行在真实的 Fabric 服务端之上；脱离它无法工作。
 * 这里实现它是为了演示「加载器抽象」的可扩展性，
 * 并让服务端能明确告知用户某个 mod 为什么不能用。
 */
public class FabricLoader implements PluginLoader {

    @Override
    public String getName() { return "FabricLoader"; }

    @Override
    public String getEcosystem() { return "Fabric"; }

    @Override
    public boolean canLoad(File jarFile) {
        return PaperLoader.hasEntry(jarFile, "fabric.mod.json");
    }

    @Override
    public LoadedPlugin readMetadata(File jarFile) {
        LoadedPlugin lp = new LoadedPlugin(jarFile);
        try {
            String json = readEntry(jarFile, "fabric.mod.json");
            if (json == null) {
                lp.getErrors().add("缺少 fabric.mod.json");
                return lp;
            }
            Map<String, Object> mod = com.mc.util.Json.parseObject(json);
            String id = str(mod, "id");
            String version = str(mod, "version");
            String name = str(mod, "name");
            if (name == null) name = id;

            // Fabric 的 mod 没有 Bukkit 意义上的主类，这里构造一个只含元数据的描述
            Map<String, Object> yml = new java.util.LinkedHashMap<>();
            yml.put("name", name);
            yml.put("version", version == null ? "?" : version);
            yml.put("main", "fabric.mod.json:entrypoints");
            yml.put("description", str(mod, "description"));
            PluginDescriptionFile desc = PluginDescriptionFile.parse(yml, jarFile);
            lp.setDescription(desc);
            lp.getWarnings().add("Fabric 模组无法在本内核运行（需要 Mixin 与真实 Fabric 服务端）");
        } catch (Exception e) {
            lp.getErrors().add("解析 fabric.mod.json 失败：" + e.getMessage());
        }
        return lp;
    }

    @Override
    public boolean isRunnable() { return false; }

    @Override
    public String getUnsupportedReason() {
        return "Fabric 模组依赖 Mixin 字节码注入和真实 Fabric 服务端，无法脱离运行";
    }

    static String readEntry(File jarFile, String entry) {
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            java.util.jar.JarEntry e = jar.getJarEntry(entry);
            if (e == null) return null;
            try (java.io.InputStream in = jar.getInputStream(e)) {
                return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String str(Map<String, Object> m, String k) {
        Object o = m.get(k);
        return o == null ? null : String.valueOf(o);
    }
}