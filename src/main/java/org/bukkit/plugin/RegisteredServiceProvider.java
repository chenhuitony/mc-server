package org.bukkit.plugin;

/**
 * 已注册的服务提供者（Bukkit 的 RegisteredServiceProvider）。
 */
public class RegisteredServiceProvider<T> {

    private final Class<T> service;
    private final T provider;
    private final Plugin plugin;
    private final ServicePriority priority;

    public RegisteredServiceProvider(Class<T> service, T provider,
                                     Plugin plugin, ServicePriority priority) {
        this.service = service;
        this.provider = provider;
        this.plugin = plugin;
        this.priority = priority == null ? ServicePriority.Normal : priority;
    }

    public Class<T> getService() { return service; }

    public T getProvider() { return provider; }

    public Plugin getPlugin() { return plugin; }

    public ServicePriority getPriority() { return priority; }
}