package org.bukkit.event;

/**
 * 事件监听器标记接口（Bukkit 的 Listener）。
 *
 * <p>插件实现本接口，并在方法上标注 {@link EventHandler}；
 * 宿主注册监听器时会扫描这些方法。
 */
public interface Listener {
}