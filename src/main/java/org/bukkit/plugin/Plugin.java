package org.bukkit.plugin;

import java.io.File;

/**
 * 所有插件的顶层接口（与 Bukkit 的 Plugin 一致）。
 *
 * <p>插件 JAR 的入口类必须实现本接口。核心生命周期：
 * {@link #onLoad()} → {@link #onEnable()} → 运行 → {@link #onDisable()}。
 */
public interface Plugin {

    /** 加载阶段调用，早于其它插件的 onEnable。 */
    default void onLoad() { }

    /** 启用阶段调用，此时可以注册监听器和指令。 */
    default void onEnable() { }

    /** 禁用阶段调用，用于释放资源。 */
    default void onDisable() { }

    /** 插件元数据。 */
    PluginDescriptionFile getDescription();

    /** 插件数据目录（plugins/&lt;插件名&gt;/）。 */
    File getDataFolder();

    /** 获取宿主提供的服务器实例。 */
    org.bukkit.Server getServer();

    /** 插件日志（自动加插件名前缀）。 */
    java.util.logging.Logger getLogger();

    /** 插件是否已启用。 */
    boolean isEnabled();

    /** 插件名称。 */
    default String getName() {
        return getDescription() == null ? "unknown" : getDescription().getName();
    }
}