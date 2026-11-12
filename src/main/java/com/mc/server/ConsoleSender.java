package com.mc.server;

import org.bukkit.command.ConsoleCommandSender;

/**
 * 控制台指令发送者。
 */
public class ConsoleSender implements ConsoleCommandSender {

    private final McServer server;

    public ConsoleSender(McServer server) {
        this.server = server;
    }

    @Override
    public void sendMessage(String message) {
        com.mc.util.Log.raw(message);
    }

    @Override
    public void sendMessage(String[] messages) {
        if (messages == null) return;
        for (String m : messages) sendMessage(m);
    }

    @Override
    public String getName() { return "CONSOLE"; }

    @Override
    public boolean hasPermission(String permission) { return true; }

    @Override
    public boolean isOp() { return true; }

    @Override
    public boolean isPlayer() { return false; }

    @Override
    public org.bukkit.Server getServer() { return server; }
}