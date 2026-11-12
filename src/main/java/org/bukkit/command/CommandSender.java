package org.bukkit.command;

/**
 * 指令发送者（玩家或控制台）。
 */
public interface CommandSender {

    /** 发送一条消息。 */
    void sendMessage(String message);

    void sendMessage(String[] messages);

    /** 发送者名称。 */
    String getName();

    /** 是否拥有某权限。 */
    boolean hasPermission(String permission);

    /** 是否是控制台（无实体）。 */
    boolean isOp();

    /** 是否是玩家。 */
    default boolean isPlayer() { return false; }

    /** 底层服务器。 */
    org.bukkit.Server getServer();
}