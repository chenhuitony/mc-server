package com.demo;

import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerStartedEvent;
import org.bukkit.plugin.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 演示插件：验证服务端内核的完整能力。
 *
 * <p>覆盖这些能力：
 * <ul>
 *   <li>继承 JavaPlugin 并拿到注入的 server / dataFolder / logger</li>
 *   <li>处理指令（/demo、/greet）</li>
 *   <li>监听事件（服务器启动、玩家加入）</li>
 *   <li>使用任务调度器（定时心跳）</li>
 *   <li>读写插件数据目录</li>
 * </ul>
 */
public class DemoPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private int heartbeats;
    private int demoCount;

    @Override
    public void onLoad() {
        getLogger().info("DemoPlugin 正在加载（onLoad）");
        getLogger().info("  数据目录：" + getDataFolder().getAbsolutePath());
        getLogger().info("  服务端：" + (getServer() == null ? "null" : getServer().getName()));
    }

    @Override
    public void onEnable() {
        getLogger().info("DemoPlugin 已启用（onEnable）");

        // 注册事件监听器
        getPluginManager().registerEvents(this, this);

        // 启动定时任务：每 20 tick（1 秒）心跳一次
        getScheduler().runTaskTimer(this, () -> {
            heartbeats++;
            if (heartbeats % 5 == 0) {
                getLogger().info("心跳 " + heartbeats + " 次，服务端已运行 "
                        + getServer().getTickCount() + " tick");
            }
        }, 20L, 20L);

        // 写一个文件证明数据目录可用
        try {
            java.io.File f = new java.io.File(getDataFolder(), "enabled.txt");
            com.mc.util.IoUtil.writeAll(f,
                    "DemoPlugin 由 chenhuitony 提供\n"
                            + "启用时间：" + new java.util.Date() + "\n");
            getLogger().info("已写入数据文件：" + f.getName());
        } catch (Exception e) {
            getLogger().warning("写数据文件失败：" + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("DemoPlugin 已禁用（onDisable），共处理 " + demoCount + " 次 /demo");
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command,
                             String label, String[] args) {
        if (label.equalsIgnoreCase("greet") || command.getName().equalsIgnoreCase("greet")) {
            String who = args.length > 0 ? args[0] : "世界";
            sender.sendMessage("[DemoPlugin] 你好，" + who + "！");
            return true;
        }

        demoCount++;
        String sub = args.length > 0 ? args[0].toLowerCase() : "help";

        switch (sub) {
            case "info":
                sender.sendMessage("[DemoPlugin] 版本 " + getDescription().getVersion()
                        + "，作者 " + getDescription().getAuthors());
                sender.sendMessage("[DemoPlugin] 服务端 tick: " + getServer().getTickCount());
                sender.sendMessage("[DemoPlugin] 在线玩家数: " + getServer().getOnlinePlayers().size());
                sender.sendMessage("[DemoPlugin] 数据目录: " + getDataFolder().getAbsolutePath());
                break;
            case "broadcast":
                String msg = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                        : "这是一条来自 DemoPlugin 的广播";
                getServer().broadcastMessage(msg);
                break;
            case "join":
                // 触发一个玩家加入事件，验证事件派发
                String player = args.length > 1 ? args[1] : "Steve";
                PlayerJoinEvent ev = new PlayerJoinEvent(player, player + " 加入了游戏");
                getPluginManager().callEvent(ev);
                sender.sendMessage("[DemoPlugin] 已触发 PlayerJoinEvent，广播消息："
                        + ev.getJoinMessage());
                break;
            case "count":
                sender.sendMessage("[DemoPlugin] /demo 已执行 " + demoCount + " 次，心跳 "
                        + heartbeats + " 次");
                break;
            default:
                sender.sendMessage("[DemoPlugin] 可用子命令：");
                sender.sendMessage("  /demo info       显示插件信息");
                sender.sendMessage("  /demo broadcast  广播消息");
                sender.sendMessage("  /demo join <名>  模拟玩家加入事件");
                sender.sendMessage("  /demo count      执行计数");
                sender.sendMessage("  /greet <名>      打招呼");
                break;
        }
        return true;
    }

    @EventHandler
    public void onServerStarted(ServerStartedEvent event) {
        getLogger().info("收到 ServerStartedEvent");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        getLogger().info("玩家加入：" + event.getPlayerName());
        event.setJoinMessage("[欢迎] " + event.getPlayerName() + " 来到了服务器");
    }
}