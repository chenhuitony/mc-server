package org.bukkit;

import java.util.Collection;
import java.util.UUID;

/**
 * 服务器门面接口（Bukkit 的 Server）。
 *
 * <p>只暴露插件开发中最常用的能力：日志、插件管理、调度器、在线玩家、广播。
 * 具体实现由宿主（com.mc）提供。
 */
public interface Server {

    /** 服务器名称，来自 server.properties 的 motd 或默认值。 */
    String getName();

    /** 服务器版本字符串。 */
    String getVersion();

    /** 服务器实现版本（本项目中是我们的宿主）。 */
    String getBukkitVersion();

    /** 全局日志。 */
    java.util.logging.Logger getLogger();

    /** 插件管理器。 */
    org.bukkit.plugin.PluginManager getPluginManager();

    /** 服务管理器（Vault 等服务插件依赖）。 */
    org.bukkit.plugin.ServicesManager getServicesManager();

    /** 任务调度器。 */
    org.bukkit.scheduler.BukkitScheduler getScheduler();

    /** 获取控制台发送者。 */
    org.bukkit.command.ConsoleCommandSender getConsoleSender();

    /** 重载插件。 */
    void reload();

    /** 事件调度器（注册监听器）。 */
    org.bukkit.plugin.PluginManager getPluginManagerRef();

    /** 获取指定名字的插件，不存在返回 null。 */
    org.bukkit.plugin.Plugin getPlugin(String name);

    /** 所有已加载插件。 */
    Collection<? extends org.bukkit.plugin.Plugin> getPlugins();

    /** 广播消息给所有在线玩家与控制台。 */
    void broadcastMessage(String message, String... args);

    /** 在线玩家名列表（本内核为无实体服务器，通常为空）。 */
    Collection<String> getOnlinePlayers();

    /** 是否处于运行状态。 */
    boolean isRunning();

    /** 当前 tick（每秒 20 次）。 */
    long getTickCount();

    /** 服务器唯一 ID。 */
    UUID getServerId();

    /** 关闭服务器。 */
    void shutdown();
}