package org.bukkit.command;

import java.util.List;

/** Tab 补全接口（Bukkit 的 TabCompleter）。 */
public interface TabCompleter {

    List<String> onTabComplete(CommandSender sender, Command command,
                               String alias, String[] args);
}