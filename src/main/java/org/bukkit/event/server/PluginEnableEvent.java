package org.bukkit.event.server;

import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

/** 插件启用事件。 */
public class PluginEnableEvent extends Event {

    private final Plugin plugin;

    public PluginEnableEvent(Plugin plugin) {
        super(false);
        this.plugin = plugin;
    }

    public Plugin getPlugin() { return plugin; }
}