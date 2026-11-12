package com.mc.loader;

import com.mc.plugin.LoadedPlugin;

import java.io.File;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Bukkit / Paper 插件加载器。
 *
 * <p>识别标志：JAR 内存在 {@code paper-plugin.yml} 或 {@code plugin.yml}。
 * 这是本项目唯一能「真正运行」的生态，因为我们实现了 Bukkit API 子集。
 */
public class PaperLoader implements PluginLoader {

    @Override
    public String getName() { return "PaperLoader"; }

    @Override
    public String getEcosystem() { return "Bukkit / Paper / Spigot"; }

    @Override
    public boolean canLoad(File jarFile) {
        return hasEntry(jarFile, "paper-plugin.yml") || hasEntry(jarFile, "plugin.yml");
    }

    @Override
    public LoadedPlugin readMetadata(File jarFile) {
        LoadedPlugin lp = new LoadedPlugin(jarFile);
        lp.loadMetadata();
        return lp;
    }

    @Override
    public boolean isRunnable() { return true; }

    @Override
    public String getUnsupportedReason() { return null; }

    static boolean hasEntry(File jarFile, String entry) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry e = jar.getJarEntry(entry);
            return e != null;
        } catch (Exception e) {
            return false;
        }
    }
}