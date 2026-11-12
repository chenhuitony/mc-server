package org.bukkit.event;

/**
 * 可取消事件的标记接口（Bukkit 的 Cancellable）。
 */
public interface Cancellable {

    boolean isCancelled();

    void setCancelled(boolean cancel);
}