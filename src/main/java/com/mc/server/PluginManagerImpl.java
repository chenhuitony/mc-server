package com.mc.server;

import com.mc.plugin.LoadedPlugin;
import com.mc.util.Log;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.CommandExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 插件管理器实现。
 *
 * <p>加载流程：
 * <ol>
 *   <li>扫描 plugins/ 下的所有 .jar；</li>
 *   <li>解析元数据，记录错误与缺失依赖；</li>
 *   <li>按 depend / softdepend / loadbefore 做拓扑排序；</li>
 *   <li>逐个创建类加载器、实例化、注入宿主、调用 onLoad → onEnable；</li>
 *   <li>注册带 @EventHandler 的方法与 plugin.yml 声明的指令。</li>
 * </ol>
 */
public class PluginManagerImpl implements PluginManager {

    private final McServer server;
    private final File pluginsDir;
    private final Map<String, LoadedPlugin> loaded = new LinkedHashMap<>();
    private final List<Plugin> enabledOrder = new ArrayList<>();

    /** 事件订阅表：事件类名 → 处理器列表（已按优先级排序）。 */
    private final Map<Class<?>, List<Handler>> handlers = new LinkedHashMap<>();

    /** 指令表：指令名 → 执行器。 */
    private final Map<String, CommandEntry> commands = new LinkedHashMap<>();

    /** 事件处理器的绑定信息。 */
    private static final class Handler {
        final Plugin plugin;
        final Listener listener;
        final Method method;
        final EventPriority priority;
        final boolean ignoreCancelled;
        Handler(Plugin p, Listener l, Method m, EventPriority pr, boolean ic) {
            this.plugin = p; this.listener = l; this.method = m;
            this.priority = pr; this.ignoreCancelled = ic;
        }
    }

    /** 指令绑定。 */
    private static final class CommandEntry {
        final Plugin plugin;
        final String name;
        final CommandExecutor executor;
        final org.bukkit.command.Command command;
        CommandEntry(Plugin p, String n, CommandExecutor e) {
            this(p, n, e, null);
        }
        CommandEntry(Plugin p, String n, CommandExecutor e, org.bukkit.command.Command c) {
            this.plugin = p; this.name = n; this.executor = e; this.command = c;
        }
    }

    public PluginManagerImpl(McServer server) {
        this.server = server;
        this.pluginsDir = new File(server.getRootDir(), "plugins");
    }

    // =================== 加载 ===================

    @Override
    public void loadPlugins() {
        pluginsDir.mkdirs();
        File[] jars = pluginsDir.listFiles((d, n) ->
                n.toLowerCase().endsWith(".jar") && !n.startsWith("."));
        if (jars == null || jars.length == 0) {
            Log.info("plugins 目录中没有插件");
            return;
        }
        Log.info("发现 " + jars.length + " 个插件文件，开始扫描");

        // 1. 解析元数据
        List<LoadedPlugin> all = new ArrayList<>();
        for (File jar : jars) {
            LoadedPlugin lp = new LoadedPlugin(jar);
            if (!lp.loadMetadata()) {
                Log.error("插件 " + jar.getName() + " 元数据无效："
                        + String.join("；", lp.getErrors()));
                continue;
            }
            loaded.put(lp.getName().toLowerCase(), lp);
            all.add(lp);
            Log.info("识别插件 " + lp.getDescription().getFullName()
                    + "（主类 " + lp.getDescription().getMainClass() + "）");
        }

        // 2. 依赖排序
        List<LoadedPlugin> ordered = topologicalSort(all);

        // 3. 逐个加载并启用
        int ok = 0, failed = 0;
        for (LoadedPlugin lp : ordered) {
            if (!checkRequirements(lp)) { failed++; continue; }
            if (activate(lp)) ok++; else failed++;
        }

        Log.info("插件加载完成：成功 " + ok + " 个，失败 " + failed + " 个");
    }

    /** 检查硬依赖是否都已成功加载。 */
    private boolean checkRequirements(LoadedPlugin lp) {
        for (String dep : lp.getDescription().getDepend()) {
            LoadedPlugin d = loaded.get(dep.toLowerCase());
            if (d == null || !d.isEnabled()) {
                Log.error("插件 " + lp.getName() + " 缺少必需依赖 " + dep + "，跳过");
                return false;
            }
        }
        return true;
    }

    /** 拓扑排序：被依赖者先加载；缺失的软依赖只告警。 */
    private List<LoadedPlugin> topologicalSort(List<LoadedPlugin> all) {
        Map<String, LoadedPlugin> byName = new LinkedHashMap<>();
        for (LoadedPlugin lp : all) byName.put(lp.getName().toLowerCase(), lp);

        List<LoadedPlugin> result = new ArrayList<>();
        Set<String> done = new LinkedHashSet<>();
        Set<String> visiting = new LinkedHashSet<>();

        for (LoadedPlugin lp : all) {
            visit(lp, byName, done, visiting, result);
        }
        return result;
    }

    private void visit(LoadedPlugin lp, Map<String, LoadedPlugin> byName,
                       Set<String> done, Set<String> visiting, List<LoadedPlugin> out) {
        String key = lp.getName().toLowerCase();
        if (done.contains(key)) return;
        if (!visiting.add(key)) {
            Log.warn("检测到插件依赖循环，涉及 " + lp.getName() + "，将按发现顺序加载");
            return;
        }
        for (String dep : lp.getDescription().getDepend()) {
            LoadedPlugin d = byName.get(dep.toLowerCase());
            if (d != null) visit(d, byName, done, visiting, out);
            else Log.warn("插件 " + lp.getName() + " 的硬依赖 " + dep + " 未找到");
        }
        for (String dep : lp.getDescription().getSoftDepend()) {
            LoadedPlugin d = byName.get(dep.toLowerCase());
            if (d != null) visit(d, byName, done, visiting, out);
        }
        visiting.remove(key);
        if (done.add(key)) out.add(lp);
    }

    /** 实例化 + 注入 + 生命周期。 */
    private boolean activate(LoadedPlugin lp) {
        Log.info("加载插件 " + lp.getDescription().getFullName());
        lp.instantiate(getClass().getClassLoader());
        if (lp.getInstance() == null) {
            Log.error("插件 " + lp.getName() + " 实例化失败："
                    + String.join("；", lp.getErrors()));
            return false;
        }
        lp.inject(server, pluginsDir);

        lp.callLifecycle("onLoad");
        if (!lp.getErrors().isEmpty()) {
            Log.error("插件 " + lp.getName() + " onLoad 出错："
                    + String.join("；", lp.getErrors()));
            lp.getErrors().clear();
        }

        // 注册根据 plugin.yml 声明的指令
        registerDeclaredCommands(lp);

        lp.callLifecycle("onEnable");
        if (!lp.getErrors().isEmpty()) {
            Log.error("插件 " + lp.getName() + " onEnable 出错："
                    + String.join("；", lp.getErrors()));
            lp.getErrors().clear();
            return false;
        }

        lp.setEnabled(true);
        enabledOrder.add(lp.getInstance());
        Log.ok("插件 " + lp.getDescription().getFullName() + " 已启用");

        // 若插件入口类是 Listener 但没在 onEnable 里手动注册，这里补注册。
        // 已被手动注册过的监听器实例会被 registerEvents 去重。
        scanEventHandlers(lp);
        return true;
    }

    /** 对实现了 Listener 的入口类自动扫描 @EventHandler（带实例去重）。 */
    private void scanEventHandlers(LoadedPlugin lp) {
        Plugin p = lp.getInstance();
        if (p instanceof Listener) {
            try {
                registerEvents((Listener) p, p);
            } catch (Throwable ignore) { }
        }
    }

    /** 把 plugin.yml 中 commands 声明的指令绑到插件（若插件实现了 CommandExecutor）。 */
    private void registerDeclaredCommands(LoadedPlugin lp) {
        Plugin p = lp.getInstance();
        if (!(p instanceof CommandExecutor)) return;
        for (String cmd : lp.getDescription().getCommands().keySet()) {
            commands.putIfAbsent(cmd.toLowerCase(),
                    new CommandEntry(p, cmd, (CommandExecutor) p));
        }
    }

    // =================== 启用/禁用 ===================

    @Override
    public void enablePlugin(Plugin plugin) {
        if (plugin == null || plugin.isEnabled()) return;
        try {
            plugin.onEnable();
        } catch (Throwable e) {
            Log.error("启用插件 " + plugin.getName() + " 失败：" + e.getMessage());
        }
    }

    @Override
    public void disablePlugin(Plugin plugin) {
        if (plugin == null) return;
        try {
            plugin.onDisable();
        } catch (Throwable e) {
            Log.error("禁用插件 " + plugin.getName() + " 时出错：" + e.getMessage());
        }
        server.getScheduler().cancelTasks(plugin);
        enabledOrder.remove(plugin);
        for (Map.Entry<String, LoadedPlugin> e : loaded.entrySet()) {
            if (e.getValue().getInstance() == plugin) {
                e.getValue().setEnabled(false);
            }
        }
        Log.info("插件 " + plugin.getName() + " 已禁用");
    }

    /** 按加载的逆序禁用全部插件。 */
    public void disableAll() {
        List<Plugin> copy = new ArrayList<>(enabledOrder);
        java.util.Collections.reverse(copy);
        for (Plugin p : copy) {
            try {
                disablePlugin(p);
            } catch (Throwable ignore) { }
        }
    }

    // =================== 查询 ===================

    @Override
    public Plugin getPlugin(String name) {
        LoadedPlugin lp = loaded.get(name.toLowerCase());
        return lp == null ? null : lp.getInstance();
    }

    @Override
    public Collection<? extends Plugin> getPlugins() {
        return new ArrayList<>(enabledOrder);
    }

    @Override
    public boolean isPluginEnabled(String name) {
        LoadedPlugin lp = loaded.get(name.toLowerCase());
        return lp != null && lp.isEnabled();
    }

    @Override
    public boolean isPluginEnabled(Plugin plugin) {
        return plugin != null && plugin.isEnabled();
    }

    /** 所有已识别插件的详细信息（含失败原因），用于诊断报告。 */
    public Collection<LoadedPlugin> getAllLoaded() {
        return new ArrayList<>(loaded.values());
    }

    // =================== 事件 ===================

    @Override
    public void registerEvents(Listener listener, Plugin plugin) {
        if (listener == null || plugin == null) return;

        // 去重：同一个监听器实例 + 同一方法 只注册一次，
        // 避免插件在 onEnable 手动注册后又被自动扫描重复注册。
        List<Handler> existing = new ArrayList<>();
        for (List<Handler> l : handlers.values()) existing.addAll(l);

        int count = 0;
        Class<?> c = listener.getClass();
        while (c != null && c != Object.class) {
            for (Method m : c.getDeclaredMethods()) {
                EventHandler ann = m.getAnnotation(EventHandler.class);
                if (ann == null) continue;
                if (m.getParameterCount() != 1) {
                    Log.warn("监听器方法 " + m.getName() + " 参数数量不为 1，已忽略");
                    continue;
                }
                Class<?> param = m.getParameterTypes()[0];
                if (!Event.class.isAssignableFrom(param)) {
                    Log.warn("监听器方法 " + m.getName() + " 参数不是 Event，已忽略");
                    continue;
                }
                boolean dup = false;
                for (Handler h : existing) {
                    if (h.listener == listener && h.method.equals(m)) { dup = true; break; }
                }
                if (dup) continue;

                m.setAccessible(true);
                handlers.computeIfAbsent(param, k -> new ArrayList<>())
                        .add(new Handler(plugin, listener, m, ann.priority(), ann.ignoreCancelled()));
                count++;
            }
            c = c.getSuperclass();
        }
        // 按优先级排序，MONITOR 最后
        for (List<Handler> list : handlers.values()) {
            list.sort(Comparator.comparingInt(h -> h.priority.ordinal()));
        }
        if (count > 0) {
            Log.info("插件 " + plugin.getName() + " 注册了 " + count + " 个事件处理器");
        }
    }

    @Override
    public void callEvent(Event event) {
        List<Handler> list = handlers.get(event.getClass());
        if (list == null) return;
        for (Handler h : new ArrayList<>(list)) {
            if (!h.plugin.isEnabled()) continue;
            if (h.ignoreCancelled && event.isCancelled()) continue;
            try {
                h.method.invoke(h.listener, event);
            } catch (Throwable e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                Log.error("插件 " + h.plugin.getName() + " 的事件处理器 "
                        + h.method.getName() + " 抛出异常："
                        + cause.getClass().getSimpleName() + ": " + cause.getMessage());
            }
        }
    }

    /** 已注册的监听器总数。 */
    public int getHandlerCount() {
        int n = 0;
        for (List<Handler> l : handlers.values()) n += l.size();
        return n;
    }

    // =================== 指令 ===================

    @Override
    public void registerCommand(Plugin plugin, String name, CommandExecutor executor) {
        if (name == null || executor == null) return;
        commands.put(name.toLowerCase(), new CommandEntry(plugin, name, executor));
    }

    @Override
    public boolean dispatchCommand(String commandLine) {
        if (commandLine == null) return false;
        String line = commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
        line = line.trim();
        if (line.isEmpty()) return false;

        String[] parts = line.split("\\s+");
        String label = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        CommandEntry entry = commands.get(label);
        if (entry == null) return false;

        CommandSender sender = server.getConsoleSender();
        // 构造一个 Command 对象传给执行器（符合真实 Bukkit 签名）
        org.bukkit.command.Command command = entry.command != null
                ? entry.command
                : new org.bukkit.command.PluginCommand(entry.name, entry.plugin);
        try {
            return entry.executor.onCommand(sender, command, label, args);
        } catch (Throwable e) {
            Log.error("执行指令 /" + label + " 时插件 " + entry.plugin.getName()
                    + " 抛出异常：" + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> getRegisteredCommands() {
        return new ArrayList<>(commands.keySet());
    }

    /** 某指令属于哪个插件。 */
    public String getCommandOwner(String cmd) {
        CommandEntry e = commands.get(cmd.toLowerCase());
        return e == null ? null : e.plugin.getName();
    }

    // =================== 权限 ===================

    private final java.util.Map<String, org.bukkit.permissions.Permission> permissions
            = new java.util.LinkedHashMap<>();

    @Override
    public org.bukkit.permissions.Permission getPermission(String name) {
        return name == null ? null : permissions.get(name.toLowerCase());
    }

    @Override
    public void addPermission(org.bukkit.permissions.Permission perm) {
        if (perm == null) return;
        permissions.computeIfAbsent(perm.getName().toLowerCase(), k -> perm);
    }

    @Override
    public void removePermission(org.bukkit.permissions.Permission perm) {
        if (perm != null) permissions.remove(perm.getName().toLowerCase());
    }

    @Override
    public java.util.Set<org.bukkit.permissions.Permission> getPermissions() {
        return new java.util.LinkedHashSet<>(permissions.values());
    }
}