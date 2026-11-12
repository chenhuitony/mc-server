package org.bukkit;

import java.util.UUID;

/**
 * 在线玩家（Bukkit 的 Player）。
 *
 * <p>本内核没有网络连接，Player 主要用于满足插件 API 的类型引用。
 */
public interface Player extends OfflinePlayer {

    /** 玩家是否在线（对本内核通常为 false）。 */
    @Override
    default boolean isOnline() { return false; }

    /** 玩家当前所在世界名。 */
    String getWorldName();

    /** 玩家坐标。 */
    double getX();
    double getY();
    double getZ();

    /** 发送消息（等同 sendMessage）。 */
    void sendRawMessage(String message);

    /** 踢出玩家。 */
    void kickPlayer(String message);
}