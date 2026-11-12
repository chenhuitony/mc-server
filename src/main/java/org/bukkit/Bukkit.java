package org.bukkit;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/**
 * Bukkit 静态工具类（Bukkit 的 Bukkit）。
 *
 * <p>插件常用它获取 Server、插件管理器、调度器、在线玩家等。
 * 本内核提供真实可用的实现，使依赖这些静态方法的插件能正常运行。
 */
public final class Bukkit {

    private static org.bukkit.Server server;

    private Bukkit() { }

    /** 由服务端启动时调用一次，注入 Server 实例。 */
    public static void setServer(org.bukkit.Server s) {
        server = s;
    }

    /** 获取服务端实例。 */
    public static org.bukkit.Server getServer() {
        return server;
    }

    /** 获取插件管理器。 */
    public static PluginManager getPluginManager() {
        return server == null ? null : server.getPluginManager();
    }

    /** 获取任务调度器。 */
    public static BukkitScheduler getScheduler() {
        return server == null ? null : server.getScheduler();
    }

    /** 获取服务管理器。 */
    public static ServicesManager getServicesManager() {
        return server == null ? null : server.getServicesManager();
    }

    /** 获取控制台发送者。 */
    public static CommandSender getConsoleSender() {
        return server == null ? null : server.getConsoleSender();
    }

    /** 获取在线玩家集合（本内核无网络，始终为空）。 */
    public static Collection<? extends Player> getOnlinePlayers() {
        return List.of();
    }

    /** 根据名字获取玩家（本内核无网络，始终返回 null）。 */
    public static Player getPlayer(String name) {
        return null;
    }

    /** 根据名字精确获取玩家。 */
    public static Player getPlayerExact(String name) {
        return null;
    }

    /** 获取插件。 */
    public static Plugin getPlugin(String name) {
        return server == null ? null : server.getPluginManager().getPlugin(name);
    }

    /** 服务端版本。 */
    public static String getVersion() {
        return server == null ? "unknown" : server.getVersion();
    }

    /** Bukkit 版本。 */
    public static String getBukkitVersion() {
        return server == null ? "unknown" : server.getBukkitVersion();
    }

    /** 服务端名字。 */
    public static String getName() {
        return server == null ? "unknown" : server.getName();
    }

    /** 广播消息。 */
    public static void broadcastMessage(String message) {
        if (server != null) server.broadcastMessage(message);
    }

    /** 重载插件。 */
    public static void reload() {
        if (server != null) server.reload();
    }
}