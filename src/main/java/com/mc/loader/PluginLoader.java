package com.mc.loader;

import com.mc.plugin.LoadedPlugin;

import java.io.File;
import java.util.List;

/**
 * 插件加载器适配器。
 *
 * <p>不同生态的插件元数据与 API 不同，但对宿主来说流程一致：
 * <b>识别 → 校验 → 排序 → 加载 → 启用</b>。本接口把这套流程抽象出来，
 * 新增一个生态（Fabric / Forge / Sponge）只需实现本接口，无需改动宿主。
 *
 * <p>内置实现：
 * <ul>
 *   <li>{@code PaperLoader} — Bukkit/Paper 插件（plugin.yml / paper-plugin.yml）</li>
 *   <li>{@code FabricLoader} — Fabric 模组（fabric.mod.json）</li>
 *   <li>{@code ForgeLoader} — Forge 模组（META-INF/mods.toml）</li>
 * </ul>
 */
public interface PluginLoader {

    /** 加载器名称，用于日志与诊断。 */
    String getName();

    /** 该加载器对应的插件生态描述，如 "Bukkit/Paper"。 */
    String getEcosystem();

    /**
     * 判断这个 JAR 是否属于本加载器能处理的类型。
     * 通过检查 JAR 内是否存在标志性元数据文件来判断。
     */
    boolean canLoad(File jarFile);

    /**
     * 解析元数据但不实例化。失败时返回的 LoadedPlugin 内含错误信息。
     */
    LoadedPlugin readMetadata(File jarFile);

    /**
     * 该生态的插件能否在本宿主上真正运行。
     * Paper 插件依赖 Bukkit API，我们实现了子集，可部分运行；
     * Fabric/Forge 模组依赖游戏客户端/服务端内部实现，只能识别不能运行。
     */
    boolean isRunnable();

    /** 不能运行时给出的原因说明。 */
    String getUnsupportedReason();

    /** 本加载器已处理的插件数量。 */
    default int getHandledCount(List<LoadedPlugin> plugins) {
        return plugins.size();
    }
}