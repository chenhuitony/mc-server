package com.mc.plugin;

import com.mc.util.IoUtil;
import com.mc.util.YamlParser;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 插件容器：一个 JAR 对应一个 LoadedPlugin。
 *
 * <p>负责从插件 JAR 读取元数据、创建类加载器、实例化入口类，
 * 并保留加载过程中的所有诊断信息（错误、缺失依赖、不支持的 API）。
 */
public class LoadedPlugin {

    private final File jarFile;
    private PluginDescriptionFile description;
    private PluginClassLoader classLoader;
    private Plugin instance;
    private File dataFolder;
    private Logger logger;
    private boolean enabled;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private boolean metadataLoaded;

    public LoadedPlugin(File jarFile) {
        this.jarFile = jarFile;
    }

    /**
     * 解析 plugin.yml / paper-plugin.yml。
     * @return 是否解析成功
     */
    public boolean loadMetadata() {
        if (metadataLoaded) return description != null;
        metadataLoaded = true;

        try {
            // 两种描述文件都尝试，paper-plugin.yml 优先（Paper 生态更常见）
            String yaml = readDescriptor("paper-plugin.yml");
            if (yaml == null) yaml = readDescriptor("plugin.yml");
            if (yaml == null) {
                yaml = readDescriptor("bukkit-plugin.yml");
            }
            if (yaml == null) {
                errors.add("插件 JAR 内缺少 plugin.yml 或 paper-plugin.yml");
                return false;
            }

            Object parsed = YamlParser.parse(splitLines(yaml));
            Map<String, Object> map = YamlParser.asMap(parsed);
            if (map.isEmpty()) {
                errors.add("描述文件为空或格式无法解析");
                return false;
            }

            description = PluginDescriptionFile.parse(map, jarFile);
            List<String> v = description.validate();
            if (!v.isEmpty()) {
                errors.addAll(v);
                return false;
            }
            return true;
        } catch (Exception e) {
            errors.add("解析描述文件失败：" + e.getMessage());
            return false;
        }
    }

    private String readDescriptor(String name) {
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            java.util.jar.JarEntry e = jar.getJarEntry(name);
            if (e == null) return null;
            try (java.io.InputStream in = jar.getInputStream(e)) {
                return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            return null;
        }
    }

    private static List<String> splitLines(String s) {
        List<String> out = new ArrayList<>();
        for (String line : s.split("\r?\n", -1)) out.add(line);
        return out;
    }

    /**
     * 创建类加载器并实例化插件入口类。
     * @param parent 宿主类加载器
     */
    public void instantiate(ClassLoader parent) {
        if (description == null) {
            errors.add("元数据未加载，无法实例化");
            return;
        }
        try {
            classLoader = new PluginClassLoader(jarFile, description.getName(), parent);
        } catch (Exception e) {
            errors.add("创建类加载器失败：" + e.getMessage());
            return;
        }

        try {
            Class<?> main = classLoader.loadClass(description.getMainClass());
            Object obj = main.getDeclaredConstructor().newInstance();
            if (!(obj instanceof Plugin)) {
                errors.add("入口类 " + description.getMainClass()
                        + " 未实现 org.bukkit.plugin.Plugin");
                return;
            }
            instance = (Plugin) obj;
        } catch (ClassNotFoundException e) {
            errors.add("找不到入口类 " + description.getMainClass());
        } catch (NoSuchMethodException e) {
            errors.add("入口类缺少无参构造函数");
        } catch (LinkageError e) {
            errors.add("类链接失败（依赖缺失的 API）：" + rootCauseMessage(e));
        } catch (java.lang.reflect.InvocationTargetException e) {
            errors.add("构造函数抛出异常：" + rootCauseMessage(e));
        } catch (Throwable e) {
            errors.add("实例化失败：" + rootCauseMessage(e));
        }
    }

    /** 递归取出最内层异常，给出可读的根因。 */
    private static String rootCauseMessage(Throwable t) {
        Throwable c = t;
        int guard = 0;
        while (c.getCause() != null && c.getCause() != c && guard++ < 10) {
            c = c.getCause();
        }
        String msg = c.getMessage();
        if (msg == null || msg.isEmpty()) {
            msg = c.getClass().getSimpleName();
        } else {
            msg = c.getClass().getSimpleName() + ": " + msg;
        }
        // 常见缺失类型的友好提示
        if (c instanceof NoClassDefFoundError) {
            msg += "（宿主未提供该类，通常说明插件依赖了本内核尚未实现的 API）";
        } else if (c instanceof ClassNotFoundException) {
            msg += "（插件未把该依赖打包进 JAR）";
        }
        return msg;
    }

    /**
     * 让宿主把自身能力注入插件实例。
     * 用反射调用 Bukkit 插件常见的 setServer/setDescription/setDataFolder/setLogger，
     * 插件若有这些方法即可拿到宿主对象。
     */
    public void inject(org.bukkit.Server server, File pluginsRoot) {
        if (instance == null) return;
        dataFolder = new File(pluginsRoot, description.getName());
        dataFolder.mkdirs();
        logger = Logger.getLogger("Minecraft." + description.getName());

        // 逐个注入，任一失败不影响其余（插件可能有部分 setter 签名不同）
        invokeIfPresent("setServer", org.bukkit.Server.class, server);
        invokeIfPresent("setDescription", PluginDescriptionFile.class, description);
        invokeIfPresent("setDataFolder", File.class, dataFolder);
        invokeIfPresent("setLogger", Logger.class, logger);
        invokeIfPresent("setEnabled", boolean.class, Boolean.TRUE);
        invokeIfPresent("setFile", File.class, jarFile);

        // 兜底：若插件自己没保存 description（老旧插件很常见），
        // 直接通过反射写 JavaPlugin 的私有字段，保证 getDescription() 可用。
        if (instance.getDescription() == null) {
            forceField(instance, "description", description);
        }
        if (instance.getDataFolder() == null) {
            forceField(instance, "dataFolder", dataFolder);
        }
    }

    /** 直接写对象（含父类）中的私有字段。 */
    private static void forceField(Object target, String fieldName, Object value) {
        Class<?> c = target.getClass();
        while (c != null && c != Object.class) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Throwable ignore) {
                return;
            }
        }
    }

    private void invokeIfPresent(String method, Class<?> type, Object value) {
        try {
            java.lang.reflect.Method m = findMethod(instance.getClass(), method, type);
            if (m != null) {
                m.setAccessible(true);
                m.invoke(instance, value);
            }
        } catch (Throwable ignore) {
            // 插件没有该方法或签名不同，属于正常情况
        }
    }

    /** 在类层次中查找方法。 */
    private static java.lang.reflect.Method findMethod(Class<?> cls, String name, Class<?> param) {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isAssignableFrom(param)) {
                    return m;
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    /** 调用插件生命周期方法。 */
    public void callLifecycle(String method) {
        if (instance == null) return;
        try {
            java.lang.reflect.Method m = null;
            for (java.lang.reflect.Method cand : instance.getClass().getMethods()) {
                if (cand.getName().equals(method) && cand.getParameterCount() == 0) {
                    m = cand;
                    break;
                }
            }
            if (m == null) {
                // 可能在父类私有实现里
                Class<?> c = instance.getClass();
                while (c != null && m == null) {
                    try {
                        m = c.getDeclaredMethod(method);
                        m.setAccessible(true);
                    } catch (NoSuchMethodException ignore) { }
                    c = c.getSuperclass();
                }
            }
            if (m != null) {
                m.setAccessible(true);
                m.invoke(instance);
            }
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            errors.add(method + "() 抛出异常：" + cause.getClass().getSimpleName()
                    + ": " + cause.getMessage());
        } catch (Throwable e) {
            errors.add(method + "() 调用失败：" + e.getMessage());
        }
    }

    // ---------- 状态 ----------

    public File getJarFile() { return jarFile; }
    public PluginDescriptionFile getDescription() { return description; }

    /** 供外部加载器直接设置已解析的元数据（如 Fabric/Forge 适配器）。 */
    public void setDescription(PluginDescriptionFile description) {
        this.description = description;
        this.metadataLoaded = true;
    }

    public PluginClassLoader getClassLoader() { return classLoader; }
    public Plugin getInstance() { return instance; }
    public File getDataFolder() { return dataFolder; }
    public Logger getLogger() { return logger; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    public String getName() {
        return description == null ? jarFile.getName() : description.getName();
    }
    public boolean isUsable() { return instance != null && errors.isEmpty(); }
}