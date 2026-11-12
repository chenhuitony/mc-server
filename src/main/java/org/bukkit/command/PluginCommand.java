package org.bukkit.command;

import org.bukkit.plugin.Plugin;

/**
 * 插件指令（Bukkit 的 PluginCommand）。
 *
 * <p>插件用 {@code getCommand("名字")} 获取，然后设置执行器、描述、权限等。
 */
public class PluginCommand extends Command {

    private final Plugin owner;
    private CommandExecutor executor;
    private TabCompleter completer;

    public PluginCommand(String name, Plugin owner) {
        super(name);
        this.owner = owner;
    }

    public Plugin getPlugin() { return owner; }

    public CommandExecutor getExecutor() { return executor; }

    public void setExecutor(CommandExecutor executor) { this.executor = executor; }

    public void setTabCompleter(TabCompleter completer) { this.completer = completer; }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (executor == null) {
            sender.sendMessage("指令 /" + getName() + " 尚未设置执行器");
            return false;
        }
        return executor.onCommand(sender, this, commandLabel, args);
    }

    @Override
    public java.util.List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (completer != null) {
            java.util.List<String> r = completer.onTabComplete(sender, this, alias, args);
            return r == null ? java.util.Collections.emptyList() : r;
        }
        return java.util.Collections.emptyList();
    }
}