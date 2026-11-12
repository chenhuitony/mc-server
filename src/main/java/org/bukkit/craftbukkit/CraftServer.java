package org.bukkit.craftbukkit;

/**
 * CraftBukkit 存在性标记。
 *
 * <p>许多插件（LuckPerms、Vault、Essentials 等）通过探测
 * {@code org.bukkit.craftbukkit.CraftServer} 是否存在及其包路径
 * 来识别服务端实现与版本。本内核提供这个标记类，
 * 让这类探测能正常通过。
 *
 * <p>注意：本类**不提供**真实的 Minecraft 服务端实现，
 * 依赖 NMS（net.minecraft.*）的深度功能仍然不可用。
 */
public final class CraftServer {

    /** 服务端实现名。 */
    public static final String IMPLEMENTATION_NAME = "MCServer";

    private CraftServer() { }
}