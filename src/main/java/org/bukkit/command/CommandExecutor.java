package org.bukkit.command;

/**
 * 指令执行器（Bukkit 原始位置）。
 *
 * <p>签名与真实 Bukkit 一致：第二个参数是被执行的 Command 对象。
 */
public interface CommandExecutor {

    /**
     * @param sender  指令发送者
     * @param command 被执行的指令对象
     * @param label   实际输入的别名
     * @param args    参数
     * @return 是否成功处理
     */
    boolean onCommand(CommandSender sender, Command command, String label, String[] args);
}