package org.bukkit.event.server;

import org.bukkit.event.Event;

/** 服务器启动完成事件。 */
public class ServerStartedEvent extends Event {

    public ServerStartedEvent() {
        super(false);
    }
}