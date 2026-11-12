package org.bukkit.command;

/**
 * 指令执行器 + Tab 补全（Bukkit 的 TabExecutor）。
 */
public interface TabExecutor extends CommandExecutor, TabCompleter {
}