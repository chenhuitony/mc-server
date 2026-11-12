package org.bukkit.plugin;

import java.util.Collection;
import java.util.List;

/**
 * 插件管理器（Bukkit 的 PluginManager）。
 *
 * <p>职责：加载/启用/禁用插件、注册与派发事件监听器、按插件隔离权限。
 */
public interface PluginManager {

    /** 加载并启用已放入 plugins 目录的全部插件。 */
    void loadPlugins();

    /** 启用指定插件。 */
    void enablePlugin(Plugin plugin);

    /** 禁用指定插件。 */
    void disablePlugin(Plugin plugin);

    /** 按名字取插件，不存在返回 null。 */
    Plugin getPlugin(String name);

    /** 所有已加载插件。 */
    Collection<? extends Plugin> getPlugins();

    /** 插件是否已启用。 */
    boolean isPluginEnabled(String name);

    boolean isPluginEnabled(Plugin plugin);

    /** 注册事件监听器。 */
    void registerEvents(org.bukkit.event.Listener listener, Plugin plugin);

    /** 直接派发一个事件。 */
    void callEvent(org.bukkit.event.Event event);

    /** 注册一条指令。 */
    void registerCommand(Plugin plugin, String name, CommandExecutor executor);

    /** 执行一条服务端指令（含插件注册的指令）。 */
    boolean dispatchCommand(String commandLine);

    /** 已注册的全部指令名。 */
    List<String> getRegisteredCommands();

    /** 按名获取权限节点，不存在返回 null。 */
    org.bukkit.permissions.Permission getPermission(String name);

    /** 注册一个权限节点（兼容旧版 Vault 等插件，返回 void）。 */
    void addPermission(org.bukkit.permissions.Permission perm);

    /** 移除权限节点。 */
    void removePermission(org.bukkit.permissions.Permission perm);

    /** 全部已注册权限。 */
    java.util.Set<org.bukkit.permissions.Permission> getPermissions();
}