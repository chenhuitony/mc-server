package org.bukkit.event.player;

import org.bukkit.event.Event;

/** 玩家加入事件（本内核无真实连接，可由指令或 API 触发）。 */
public class PlayerJoinEvent extends Event {

    private final String playerName;
    private String joinMessage;

    public PlayerJoinEvent(String playerName, String joinMessage) {
        super(false);
        this.playerName = playerName;
        this.joinMessage = joinMessage;
    }

    public String getPlayerName() { return playerName; }

    public String getJoinMessage() { return joinMessage; }

    public void setJoinMessage(String msg) { this.joinMessage = msg; }
}