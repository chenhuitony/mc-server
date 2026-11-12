package org.bukkit.event.server;

import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

/** 插件禁用事件。 */
public class PluginDisableEvent extends Event {

    private final Plugin plugin;

    public PluginDisableEvent(Plugin plugin) {
        super(false);
        this.plugin = plugin;
    }

    public Plugin getPlugin() { return plugin; }
}