package com.mc.server;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务管理器实现。
 *
 * <p>Vault 这类“服务提供者”插件会注册服务接口，
 * 其它插件通过 {@link #load} 取得实现。这里提供完整可用的实现。
 */
public class ServicesManagerImpl implements ServicesManager {

    private static final class Provider<T> {
        final T instance;
        final Plugin plugin;
        final ServicePriority priority;
        Provider(T i, Plugin p, ServicePriority pr) {
            this.instance = i; this.plugin = p; this.priority = pr;
        }
    }

    private final Map<Class<?>, List<Provider<?>>> services = new LinkedHashMap<>();

    @Override
    public <T> void register(Class<T> service, T provider, Plugin plugin, ServicePriority priority) {
        if (service == null || provider == null) return;
        List<Provider<?>> list = services.computeIfAbsent(service, k -> new ArrayList<>());
        list.add(new Provider<>(provider, plugin, priority == null ? ServicePriority.Normal : priority));
        list.sort((a, b) -> b.priority.ordinal() - a.priority.ordinal());
    }

    @Override
    public <T> void unregister(Class<T> service, T provider) {
        List<Provider<?>> list = services.get(service);
        if (list == null) return;
        list.removeIf(p -> p.instance == provider);
        if (list.isEmpty()) services.remove(service);
    }

    @Override
    public void unregisterAll(Plugin plugin) {
        for (List<Provider<?>> list : services.values()) {
            list.removeIf(p -> p.plugin == plugin);
        }
        services.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T load(Class<T> service) {
        List<Provider<?>> list = services.get(service);
        if (list == null || list.isEmpty()) return null;
        return (T) list.get(0).instance;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> org.bukkit.plugin.RegisteredServiceProvider<T> getRegistration(Class<T> service) {
        List<Provider<?>> list = services.get(service);
        if (list == null || list.isEmpty()) return null;
        Provider<?> p = list.get(0);
        return new org.bukkit.plugin.RegisteredServiceProvider<>(
                service, (T) p.instance, p.plugin, p.priority);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<org.bukkit.plugin.RegisteredServiceProvider<T>> getRegistrations(Class<T> service) {
        List<org.bukkit.plugin.RegisteredServiceProvider<T>> out = new ArrayList<>();
        List<Provider<?>> list = services.get(service);
        if (list == null) return out;
        for (Provider<?> p : list) {
            out.add(new org.bukkit.plugin.RegisteredServiceProvider<>(
                    service, (T) p.instance, p.plugin, p.priority));
        }
        return out;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<Class<? extends T>> getKnownServices(Class<T> service) {
        Collection<Class<? extends T>> out = new ArrayList<>();
        for (Class<?> c : services.keySet()) {
            if (service.isAssignableFrom(c)) out.add((Class<? extends T>) c);
        }
        return out;
    }

    /** 已注册服务总数。 */
    public int getServiceCount() {
        int n = 0;
        for (List<Provider<?>> l : services.values()) n += l.size();
        return n;
    }

    @Override
    public Collection<Class<?>> getKnownServices() {
        return new ArrayList<>(services.keySet());
    }
}