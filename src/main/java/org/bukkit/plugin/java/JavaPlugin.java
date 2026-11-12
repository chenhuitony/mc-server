package org.bukkit.plugin.java;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * 插件基类（Bukkit 的 JavaPlugin）。
 *
 * <p>绝大多数 Bukkit/Paper 插件都继承本类。宿主的 {@code PluginManagerImpl}
 * 会在实例化后调用上面这些 setter 把宿主对象注入进来。
 */
public abstract class JavaPlugin implements Plugin {

    private Server server;
    private PluginDescriptionFile description;
    private File dataFolder;
    private Logger logger;
    private boolean enabled;
    private File file;

    // ---------- 宿主注入 ----------

    public void setServer(Server server) { this.server = server; }
    public void setDescription(PluginDescriptionFile d) { this.description = d; }
    public void setDataFolder(File f) { this.dataFolder = f; if (f != null) f.mkdirs(); }
    public void setLogger(Logger l) { this.logger = l; }
    public void setEnabled(boolean b) { this.enabled = b; }
    public void setFile(File f) { this.file = f; }

    // ---------- Plugin 接口 ----------

    @Override
    public PluginDescriptionFile getDescription() { return description; }

    @Override
    public File getDataFolder() {
        if (dataFolder == null) {
            dataFolder = new File("plugins", getName());
            dataFolder.mkdirs();
        }
        return dataFolder;
    }

    @Override
    public Server getServer() { return server; }

    @Override
    public Logger getLogger() {
        if (logger == null) logger = Logger.getLogger("Minecraft." + getName());
        return logger;
    }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public String getName() {
        return description == null ? getClass().getSimpleName() : description.getName();
    }

    public File getFile() { return file; }

    // ---------- 便捷方法 ----------

    public PluginManager getPluginManager() {
        return server == null ? null : server.getPluginManager();
    }

    public BukkitScheduler getScheduler() {
        return server == null ? null : ((com.mc.server.McServer) server).getScheduler();
    }

    /** 服务管理器（Vault 等服务插件常用）。 */
    public org.bukkit.plugin.ServicesManager getServicesManager() {
        return server == null ? null : server.getServicesManager();
    }

    /** 服务器版本。 */
    public String getServerVersion() {
        return server == null ? "unknown" : server.getVersion();
    }

    /** 服务器名称。 */
    public String getServerName() {
        return server == null ? "unknown" : server.getName();
    }

    /** 插件是否支持 Folia（本内核按普通 Bukkit 处理）。 */
    public boolean isFolia() {
        return false;
    }

    /** 从插件 JAR 读取内嵌资源。 */
    public InputStream getResource(String filename) {
        ClassLoader cl = getClass().getClassLoader();
        return cl == null ? null : cl.getResourceAsStream(filename);
    }

    /** 生成默认配置（若不存在）。 */
    public void saveDefaultConfig() {
        File cfg = new File(getDataFolder(), "config.yml");
        if (cfg.exists()) return;
        try (InputStream in = getResource("config.yml")) {
            if (in != null) {
                com.mc.util.IoUtil.copyResource(in, cfg);
            } else {
                // 插件没有内嵌默认配置时创建一个空文件
                com.mc.util.IoUtil.writeAll(cfg, "# " + getName() + " 配置\n");
            }
        } catch (Exception e) {
            getLogger().warning("保存默认配置失败：" + e.getMessage());
        }
    }

    private org.bukkit.configuration.file.FileConfiguration config;

    /** 获取插件配置（自动加载 config.yml）。 */
    public org.bukkit.configuration.file.FileConfiguration getConfig() {
        if (config == null) {
            File cfgFile = new File(getDataFolder(), "config.yml");
            config = new org.bukkit.configuration.file.FileConfiguration(cfgFile);
            try {
                config.load();
            } catch (Exception e) {
                getLogger().warning("加载配置失败：" + e.getMessage());
            }
        }
        return config;
    }

    /** 重新加载配置。 */
    public void reloadConfig() {
        File cfgFile = new File(getDataFolder(), "config.yml");
        config = new org.bukkit.configuration.file.FileConfiguration(cfgFile);
        try {
            config.load();
        } catch (Exception e) {
            getLogger().warning("重新加载配置失败：" + e.getMessage());
        }
    }

    /** 保存配置到 config.yml。 */
    public void saveConfig() {
        try {
            getConfig().save();
        } catch (Exception e) {
            getLogger().warning("保存配置失败：" + e.getMessage());
        }
    }

    /**
     * 获取 plugin.yml 中声明的指令。
     *
     * <p>返回的 PluginCommand 已绑定本插件；插件可在 onEnable 中
     * {@code setExecutor(this)} 设置执行器。
     */
    public org.bukkit.command.PluginCommand getCommand(String name) {
        if (description == null) return null;
        String key = name.toLowerCase();
        for (String declared : description.getCommands().keySet()) {
            if (declared.equalsIgnoreCase(key)) {
                return commandRegistry.computeIfAbsent(key,
                        k -> new org.bukkit.command.PluginCommand(k, this));
            }
        }
        // 即使未在 plugin.yml 声明也允许获取，方便动态指令
        return commandRegistry.computeIfAbsent(key,
                k -> new org.bukkit.command.PluginCommand(k, this));
    }

    private final java.util.Map<String, org.bukkit.command.PluginCommand> commandRegistry
            = new java.util.LinkedHashMap<>();

    /** 控制台发送者。 */
    public CommandSender getConsoleSender() {
        return server == null ? null : ((com.mc.server.McServer) server).getConsoleSender();
    }

    /** 便捷日志。 */
    public void info(String msg) { getLogger().info(msg); }
    public void warn(String msg) { getLogger().warning(msg); }
}