package org.bukkit.scheduler;

import org.bukkit.plugin.Plugin;

/**
 * 任务调度器（Bukkit 的 BukkitScheduler）。
 *
 * <p>签名与真实 Bukkit 一致，返回 {@link BukkitTask}。
 * 宿主用 20 TPS 主线程循环驱动同步任务，异步任务交给线程池。
 */
public interface BukkitScheduler {

    BukkitTask runTask(Plugin plugin, Runnable task);

    BukkitTask runTaskLater(Plugin plugin, Runnable task, long delayTicks);

    BukkitTask runTaskTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks);

    BukkitTask runTaskAsynchronously(Plugin plugin, Runnable task);

    BukkitTask runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delayTicks);

    BukkitTask runTaskTimerAsynchronously(Plugin plugin, Runnable task,
                                          long delayTicks, long periodTicks);

    /** 计数器式接口（老 API 兼容，返回 int 任务 ID）。 */
    int scheduleSyncDelayedTask(Plugin plugin, Runnable task, long delay);

    int scheduleSyncRepeatingTask(Plugin plugin, Runnable task, long delay, long period);

    void cancelTask(int taskId);

    void cancelTasks(Plugin plugin);

    long getCurrentTick();

    boolean isQueued(int taskId);
}