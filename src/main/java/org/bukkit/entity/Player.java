package org.bukkit.entity;

import org.bukkit.OfflinePlayer;
import java.util.UUID;

/** 玩家实体（Bukkit 的 Player extends OfflinePlayer）。 */
public interface Player extends OfflinePlayer {

    /** 玩家当前所在世界名。 */
    String getWorldName();

    /** 玩家坐标。 */
    double getX();
    double getY();
    double getZ();

    /** 发送消息。 */
    void sendRawMessage(String message);

    /** 踢出玩家。 */
    void kickPlayer(String message);

    /** 获取 UUID。 */
    UUID getUniqueId();
}