package org.bukkit.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一条已注册的指令（Bukkit 的 Command）。
 *
 * <p>插件可以在 onEnable 中创建 Command 子类并注册，以便自定义 tab 补全、
 * 权限、用法提示等。
 */
public abstract class Command {

    private final String name;
    private String description = "";
    private String usage = "/<command>";
    private final List<String> aliases = new ArrayList<>();
    private String permission;
    private String permissionMessage = "你没有权限执行此指令";

    protected Command(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getUsage() { return usage; }
    public void setUsage(String u) { this.usage = u; }
    public List<String> getAliases() { return Collections.unmodifiableList(aliases); }
    public String getPermission() { return permission; }
    public void setPermission(String p) { this.permission = p; }
    public String getPermissionMessage() { return permissionMessage; }
    public void setPermissionMessage(String m) { this.permissionMessage = m; }

    /** 执行指令。 */
    public abstract boolean execute(CommandSender sender, String commandLabel, String[] args);

    /** Tab 补全。 */
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return Collections.emptyList();
    }
}