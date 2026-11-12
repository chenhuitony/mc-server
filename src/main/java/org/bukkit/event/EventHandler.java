package org.bukkit.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为事件处理器（Bukkit 的 @EventHandler）。
 *
 * <p>宿主在注册监听器时扫描带本注解的方法，按参数类型订阅事件。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {

    /** 优先级，数值小的先执行。 */
    EventPriority priority() default EventPriority.NORMAL;

    /** 是否忽略被取消的事件。 */
    boolean ignoreCancelled() default false;
}