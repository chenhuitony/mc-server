package com.mc.plugin;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 插件类加载器。
 *
 * <p>关键设计：采用「父优先（parent-first）+ 共享 API」策略。
 *
 * <ul>
 *   <li>Bukkit API 包（org.bukkit.*）一律由宿主加载，保证所有插件看到同一份接口，
 *       否则会出现 ClassCastException（插件 A 的 Event 与宿主不是同一个类）。</li>
 *   <li>JDK 包（java.*, javax.*, jdk.*）交父加载器。</li>
 *   <li>其余包（插件自己的类、以及它内嵌的第三方库）由本加载器从插件 JAR 加载。</li>
 * </ul>
 *
 * <p>这样可以正确加载带内嵌依赖的插件（如 LuckPerms 会把 guava、jackson
 * 等打进自己的 jar），又不会破坏宿主 API 的类型一致性。
 */
public class PluginClassLoader extends URLClassLoader {

    /** 必须由宿主统一的包前缀。 */
    private static final String[] SHARED_PREFIXES = {
            "org.bukkit.",
            "org.bukkit",
            "io.papermc.",
            "net.kyori.",
            "com.destroystokyo."
    };

    static {
        registerAsParallelCapable();
    }

    private final File jarFile;
    private final String pluginName;
    private final List<String> classNames = new ArrayList<>();

    public PluginClassLoader(File jarFile, String pluginName, ClassLoader parent) throws Exception {
        super(new URL[]{jarFile.toURI().toURL()}, parent);
        this.jarFile = jarFile;
        this.pluginName = pluginName;
        indexClasses();
    }

    /** 预先索引 JAR 中的所有类名，便于诊断与统计。 */
    private void indexClasses() {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> es = jar.entries();
            while (es.hasMoreElements()) {
                JarEntry e = es.nextElement();
                String n = e.getName();
                if (n.endsWith(".class")) {
                    classNames.add(n.substring(0, n.length() - 6).replace('/', '.'));
                }
            }
        } catch (Exception ignore) { }
    }

    /** 插件 JAR 内的类总数。 */
    public int getClassCount() {
        return classNames.size();
    }

    public List<String> getClassNames() {
        return new ArrayList<>(classNames);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) return c;

            if (isShared(name)) {
                return super.loadClass(name, resolve);
            }

            // 优先从插件自己的 JAR 找，找不到再交父加载器（JDK 类等）
            try {
                c = findClass(name);
            } catch (ClassNotFoundException e) {
                c = super.loadClass(name, resolve);
            }
            if (resolve) resolveClass(c);
            return c;
        }
    }

    private boolean isShared(String name) {
        if (name.startsWith("java.") || name.startsWith("javax.")
                || name.startsWith("jdk.") || name.startsWith("sun.")
                || name.startsWith("com.sun.")) {
            return true;
        }
        for (String p : SHARED_PREFIXES) {
            if (name.startsWith(p)) return true;
        }
        return false;
    }

    /** 读取插件 JAR 内某个资源为文本。 */
    public String readResourceAsText(String path) throws Exception {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry e = jar.getJarEntry(path);
            if (e == null) return null;
            try (InputStream in = jar.getInputStream(e)) {
                return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
    }

    /** 插件 JAR 内是否存在某资源。 */
    public boolean hasResource(String path) {
        try (JarFile jar = new JarFile(jarFile)) {
            return jar.getJarEntry(path) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "PluginClassLoader[" + pluginName + ", " + classNames.size() + " classes]";
    }
}