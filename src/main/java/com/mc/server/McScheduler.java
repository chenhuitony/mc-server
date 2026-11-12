package com.mc.server;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务调度器实现（20 TPS 主线程驱动 + 异步线程池）。
 */
public class McScheduler implements BukkitScheduler {

    private final class Task implements BukkitTask {
        final int id;
        final Plugin owner;
        final Runnable runnable;
        final boolean async;
        long nextTick;
        final long period;
        volatile boolean cancelled;

        Task(int id, Plugin owner, Runnable r, boolean async, long nextTick, long period) {
            this.id = id;
            this.owner = owner;
            this.runnable = r;
            this.async = async;
            this.nextTick = nextTick;
            this.period = period;
        }

        @Override public int getTaskId() { return id; }
        @Override public Plugin getOwner() { return owner; }
        @Override public boolean isSync() { return !async; }
        @Override public boolean isCancelled() { return cancelled; }
        @Override public void cancel() {
            cancelled = true;
            tasks.remove(id);
        }
    }

    private final Map<Integer, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicInteger idGen = new AtomicInteger(1);
    private final ExecutorService asyncPool;
    private volatile long currentTick;

    public McScheduler() {
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        this.asyncPool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "MC-Async");
            t.setDaemon(true);
            return t;
        });
    }

    /** 每 tick 由主循环调用。 */
    public void tick() {
        currentTick++;
        List<Task> due = new ArrayList<>();
        for (Task t : tasks.values()) {
            if (!t.cancelled && !t.async && t.nextTick <= currentTick) due.add(t);
        }
        for (Task t : due) {
            if (t.cancelled) continue;
            try {
                t.runnable.run();
            } catch (Throwable e) {
                com.mc.util.Log.error("任务 #" + t.id + "（插件 "
                        + (t.owner == null ? "?" : t.owner.getName()) + "）异常："
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            if (t.cancelled) continue;
            if (t.period > 0) t.nextTick = currentTick + t.period;
            else tasks.remove(t.id);
        }
    }

    // ---------- BukkitScheduler ----------

    @Override
    public BukkitTask runTask(Plugin plugin, Runnable task) {
        return schedule(plugin, task, false, 0, 0);
    }

    @Override
    public BukkitTask runTaskLater(Plugin plugin, Runnable task, long delay) {
        return schedule(plugin, task, false, Math.max(0, delay), 0);
    }

    @Override
    public BukkitTask runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
        return schedule(plugin, task, false, Math.max(0, delay), Math.max(1, period));
    }

    @Override
    public BukkitTask runTaskAsynchronously(Plugin plugin, Runnable task) {
        Task t = new Task(idGen.getAndIncrement(), plugin, task, true, currentTick, 0);
        asyncPool.submit(() -> safeRun(t));
        return t;
    }

    @Override
    public BukkitTask runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay) {
        Task t = new Task(idGen.getAndIncrement(), plugin, task, true, currentTick, 0);
        asyncPool.submit(() -> {
            try { Thread.sleep(Math.max(0, delay) * 50L); } catch (InterruptedException ignore) { }
            safeRun(t);
        });
        return t;
    }

    @Override
    public BukkitTask runTaskTimerAsynchronously(Plugin plugin, Runnable task,
                                                 long delay, long period) {
        Task t = new Task(idGen.getAndIncrement(), plugin, task, true, currentTick, period);
        tasks.put(t.id, t);
        asyncPool.submit(() -> {
            try { Thread.sleep(Math.max(0, delay) * 50L); } catch (InterruptedException ignore) { }
            while (!t.cancelled) {
                safeRun(t);
                if (period <= 0) break;
                try { Thread.sleep(period * 50L); } catch (InterruptedException ignore) { break; }
            }
            tasks.remove(t.id);
        });
        return t;
    }

    private void safeRun(Task t) {
        if (t.cancelled) return;
        try {
            t.runnable.run();
        } catch (Throwable e) {
            com.mc.util.Log.error("异步任务 #" + t.id + " 异常："
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private Task schedule(Plugin plugin, Runnable task, boolean async, long delay, long period) {
        Task t = new Task(idGen.getAndIncrement(), plugin, task, async, currentTick + delay, period);
        tasks.put(t.id, t);
        return t;
    }

    @Override
    public int scheduleSyncDelayedTask(Plugin plugin, Runnable task, long delay) {
        return runTaskLater(plugin, task, delay).getTaskId();
    }

    @Override
    public int scheduleSyncRepeatingTask(Plugin plugin, Runnable task, long delay, long period) {
        return runTaskTimer(plugin, task, delay, period).getTaskId();
    }

    @Override
    public void cancelTask(int taskId) {
        Task t = tasks.remove(taskId);
        if (t != null) t.cancelled = true;
    }

    @Override
    public void cancelTasks(Plugin plugin) {
        for (Task t : new ArrayList<>(tasks.values())) {
            if (t.owner == plugin) {
                t.cancelled = true;
                tasks.remove(t.id);
            }
        }
    }

    @Override public long getCurrentTick() { return currentTick; }

    @Override public boolean isQueued(int taskId) {
        Task t = tasks.get(taskId);
        return t != null && !t.cancelled;
    }

    public void shutdown() {
        for (Task t : tasks.values()) t.cancelled = true;
        tasks.clear();
        asyncPool.shutdownNow();
    }

    public int getActiveTaskCount() { return tasks.size(); }
}