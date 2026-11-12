package org.bukkit.plugin;

/**
 * 指令执行器。
 *
 * <p>现代 Bukkit 的路径，继承旧位置的同名接口，两种路径的插件都能链接。
 */
public interface CommandExecutor extends org.bukkit.command.CommandExecutor {
}