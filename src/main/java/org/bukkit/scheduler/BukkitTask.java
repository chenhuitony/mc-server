package org.bukkit.scheduler;

import org.bukkit.plugin.Plugin;

/** 一个已调度的任务（Bukkit 的 BukkitTask）。 */
public interface BukkitTask {

    /** 任务 ID。 */
    int getTaskId();

    /** 拥有者插件。 */
    Plugin getOwner();

    /** 是否异步。 */
    boolean isSync();

    /** 是否已取消。 */
    boolean isCancelled();

    /** 取消任务。 */
    void cancel();
}