package org.bukkit.event;

/**
 * 所有事件的基类（Bukkit 的 Event）。
 *
 * <p>事件名默认取类名，用于监听器匹配。
 */
public abstract class Event {

    private final String name;
    private final boolean async;
    private boolean cancelled;

    protected Event() { this(false); }

    protected Event(boolean async) {
        this.name = getClass().getSimpleName();
        this.async = async;
    }

    /** 事件名。 */
    public String getEventName() { return name; }

    /** 是否异步执行。 */
    public boolean isAsynchronous() { return async; }

    /** 取消事件（仅 Cancellable 事件有效）。 */
    public void setCancelled(boolean cancel) {
        if (!(this instanceof Cancellable)) {
            throw new IllegalStateException("事件 " + name + " 不可取消");
        }
        this.cancelled = cancel;
    }

    public boolean isCancelled() {
        return this instanceof Cancellable && cancelled;
    }

    /** 可取消事件的标记接口。 */
    public interface Cancellable {
        boolean isCancelled();
        void setCancelled(boolean cancel);
    }
}