package com.mc.loader;

import com.mc.plugin.LoadedPlugin;
import com.mc.util.I18n;
import com.mc.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件/模组扫描器。
 *
 * <p>把 plugins/ 目录下的所有 JAR 按生态分派给对应加载器。
 * 每个加载器自己判断 {@code canLoad}，扫描器只负责调度与汇总。
 * 要支持新的生态，实现 {@link PluginLoader} 并在此注册即可。
 */
public class PluginScanner {

    private final List<PluginLoader> loaders = new ArrayList<>();

    public PluginScanner() {
        // 注册顺序即匹配优先级
        loaders.add(new PaperLoader());
        loaders.add(new FabricLoader());
        loaders.add(new ForgeLoader());
    }

    public void registerLoader(PluginLoader loader) {
        if (loader != null) loaders.add(loader);
    }

    public List<PluginLoader> getLoaders() {
        return new ArrayList<>(loaders);
    }

    /** 扫描结果。 */
    public static class ScanResult {
        public final List<LoadedPlugin> runnable = new ArrayList<>();
        public final List<LoadedPlugin> recognizedOnly = new ArrayList<>();
        public final List<LoadedPlugin> failed = new ArrayList<>();
        public final Map<String, Integer> byEcosystem = new LinkedHashMap<>();
        public final List<File> unknown = new ArrayList<>();
    }

    /**
     * 扫描目录，识别每个 JAR 的生态并解析元数据。
     */
    public ScanResult scan(File pluginsDir) {
        ScanResult result = new ScanResult();
        pluginsDir.mkdirs();

        File[] jars = pluginsDir.listFiles((d, n) ->
                n.toLowerCase().endsWith(".jar") && !n.startsWith("."));
        if (jars == null || jars.length == 0) {
            return result;
        }

        for (File jar : jars) {
            PluginLoader loader = findLoader(jar);
            if (loader == null) {
                result.unknown.add(jar);
                Log.warn("scanner.unknown", jar.getName());
                continue;
            }

            LoadedPlugin lp = loader.readMetadata(jar);
            result.byEcosystem.merge(loader.getEcosystem(), 1, Integer::sum);

            if (!lp.getErrors().isEmpty()) {
                result.failed.add(lp);
                continue;
            }
            if (loader.isRunnable()) {
                result.runnable.add(lp);
            } else {
                result.recognizedOnly.add(lp);
            }
        }
        return result;
    }

    private PluginLoader findLoader(File jar) {
        for (PluginLoader l : loaders) {
            try {
                if (l.canLoad(jar)) return l;
            } catch (Exception ignore) { }
        }
        return null;
    }

    /** 打印扫描报告。 */
    public void printReport(ScanResult r) {
        Log.info("scanner.header");
        if (!r.byEcosystem.isEmpty()) {
            StringBuilder sb = new StringBuilder(I18n.get("scanner.ecosystem"));
            for (Map.Entry<String, Integer> e : r.byEcosystem.entrySet()) {
                sb.append(e.getKey()).append(" ×").append(e.getValue()).append("  ");
            }
            Log.info(sb.toString().trim());
        }
        Log.info("scanner.summary", r.runnable.size(), r.recognizedOnly.size(), r.failed.size(), r.unknown.size());

        for (LoadedPlugin lp : r.runnable) {
            Log.ok("scanner.runnable_item", lp.getDescription().getFullName());
        }
        for (LoadedPlugin lp : r.recognizedOnly) {
            Log.warn("scanner.identified_item", lp.getDescription().getFullName(),
                    String.join("；", lp.getWarnings()));
        }
        for (LoadedPlugin lp : r.failed) {
            Log.error("scanner.failed_item", lp.getName(), String.join("；", lp.getErrors()));
        }
        for (File f : r.unknown) {
            Log.warn("scanner.unknown_item", f.getName());
        }
        Log.info("scanner.footer");
    }
}