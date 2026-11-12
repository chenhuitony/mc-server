package org.bukkit.event.player;

import org.bukkit.event.Event;

/** 玩家离开事件。 */
public class PlayerQuitEvent extends Event {

    private final String playerName;
    private String quitMessage;

    public PlayerQuitEvent(String playerName, String quitMessage) {
        super(false);
        this.playerName = playerName;
        this.quitMessage = quitMessage;
    }

    public String getPlayerName() { return playerName; }

    public String getQuitMessage() { return quitMessage; }

    public void setQuitMessage(String msg) { this.quitMessage = msg; }
}