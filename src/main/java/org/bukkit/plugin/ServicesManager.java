package org.bukkit.plugin;

/**
 * 服务注册（Bukkit 的 ServicesManager）。
 *
 * <p>插件用它注册/获取服务实现（如 Vault 的经济、权限服务）。
 * 本内核提供真实可用的实现，使依赖服务的插件能正常运行。
 */
public interface ServicesManager {

    /** 注册一个服务实现。 */
    <T> void register(Class<T> service, T provider, Plugin plugin, ServicePriority priority);

    /** 注销服务实现。 */
    <T> void unregister(Class<T> service, T provider);

    /** 注销某插件注册的全部服务。 */
    void unregisterAll(Plugin plugin);

    /** 获取优先级最高的服务实现，没有则返回 null。 */
    <T> T load(Class<T> service);

    /** 获取服务注册信息（含提供者插件与优先级）。 */
    <T> RegisteredServiceProvider<T> getRegistration(Class<T> service);

    /** 获取某服务的全部注册信息。 */
    <T> java.util.List<RegisteredServiceProvider<T>> getRegistrations(Class<T> service);

    /** 获取全部服务实现。 */
    <T> java.util.Collection<Class<? extends T>> getKnownServices(Class<T> service);

    /** 获取所有已注册的服务类（无参版本，供兼容）。 */
    java.util.Collection<Class<?>> getKnownServices();
}