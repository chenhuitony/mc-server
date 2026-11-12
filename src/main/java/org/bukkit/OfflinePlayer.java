package org.bukkit;

import java.util.UUID;

/**
 * 离线玩家（Bukkit 的 OfflinePlayer）。
 */
public interface OfflinePlayer {

    /** 玩家名。 */
    String getName();

    /** 唯一 ID。 */
    UUID getUniqueId();

    /** 是否在线。 */
    boolean isOnline();

    /** 是否被封禁。 */
    boolean isBanned();

    /** 是否是 OP。 */
    boolean isOp();

    /** 设置 OP。 */
    void setOp(boolean value);

    /** 首次加入时间（毫秒）。 */
    long getFirstPlayed();

    /** 最后在线时间（毫秒）。 */
    long getLastPlayed();

    /** 是否玩过。 */
    boolean hasPlayedBefore();
}