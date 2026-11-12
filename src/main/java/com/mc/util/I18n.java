package com.mc.util;

/**
 * 国际化消息管理器。
 * 支持中文(zh_CN)和英文(en_US)，通过 --lang 参数或系统 Locale 切换。
 */
public final class I18n {

    private static final java.util.Map<String, java.util.Map<String, String>> BUNDLES = new java.util.LinkedHashMap<>();
    private static String currentLang = "zh_CN";

    static {
        // 中文（默认）
        java.util.Map<String, String> zh = new java.util.LinkedHashMap<>();
        zh.put("server.starting", "======================================");
        zh.put("server.version", "  Minecraft 服务端内核 v{0}");
        zh.put("server.target", "  目标版本 MC {0}");
        zh.put("server.dir", "  运行目录 {0}");
        zh.put("server.ending", "======================================");
        zh.put("scan.found", "发现 {0} 个插件文件，开始扫描");
        zh.put("scan.identified", "识别插件 {0} v{1}（主类 {2}）");
        zh.put("scan.report", "扫描报告：");
        zh.put("scan.runnable", "  可运行：{0}  仅识别：{1}  失败：{2}  未知：{3}");
        zh.put("scan.runnable_item", "  [可运行] {0} v{1}");
        zh.put("scan.identified_item", "  [仅识别] {0} v{1}");
        zh.put("scan.failed_item", "  [失败] {0} v{1}  原因: {2}");
        zh.put("scan.unknown_item", "  [未知] {0}");
        zh.put("scan.needs_deps", "  [需依赖] {0} v{1}  缺少类: {2}");
        zh.put("load.plugin", "加载插件 {0} v{1}");
        zh.put("load.onload", "  {0} 正在加载（onLoad）");
        zh.put("load.datadir", "  数据目录：{0}");
        zh.put("load.servername", "  服务端：{0}");
        zh.put("load.onenable", "  {0} 已启用（onEnable）");
        zh.put("load.events", "插件 {0} 注册了 {1} 个事件处理器");
        zh.put("load.enabled", "  [OK] 插件 {0} v{1} 已启用");
        zh.put("load.error_instantiate", "插件 {0} 实例化失败：{1}");
        zh.put("load.error_onload", "插件 {0} onLoad 出错：{1}");
        zh.put("load.error_onenable", "插件 {0} onEnable 出错：{1}");
        zh.put("load.summary", "插件加载完成：成功 {0} 个，失败 {1} 个");
        zh.put("server.started", "服务端已启动，输入 help 查看可用指令，stop 关闭");
        zh.put("server.shutdown", "收到关闭指令，正在停止服务端...");
        zh.put("server.disabled", "插件 {0} 已禁用");
        zh.put("server.closed", "服务端已关闭");
        zh.put("cmd.unknown", "未知指令：{0}（输入 help 查看可用指令）");
        zh.put("cmd.plugins", "已加载插件（{0} 个）：");
        zh.put("cmd.plugin_entry", "  - {0} v{1} [{2}]");
        zh.put("cmd.version", "服务端内核 v{0}，目标 MC {1}，Java {2}");
        zh.put("cmd.tps", "当前 tick {0}，活跃任务 {1}，事件处理器 {2}");
        zh.put("cmd.help", "可用指令：");
        zh.put("cmd.help.plugins", "  plugins  列出已加载插件");
        zh.put("cmd.help.version", "  version  显示版本信息");
        zh.put("cmd.help.tps", "  tps      运行状态统计");
        zh.put("cmd.help.say", "  say <消息> 广播");
        zh.put("cmd.help.help", "  help     显示本帮助");
        zh.put("cmd.help.stop", "  stop     关闭服务端");
        zh.put("cmd.help.plugin_cmds", "  插件指令：{0}");
        zh.put("load.enabled_state", "已启用");
        zh.put("load.disabled_state", "未启用");
        zh.put("load.failed_state", "加载失败");
        zh.put("error.class_not_found", "类链接失败：{0}（宿主未提供该类，且插件未打包该依赖）");
        zh.put("error.class_not_found_jar", "类未找到：{0}（插件未把该依赖打包进 JAR）");
        zh.put("error.init_failed", "初始化失败：{0}");
        zh.put("error.inject_failed", "依赖注入失败：{0}");
        zh.put("error.task_exception", "任务 #{0}（插件 {1}）异常：{2}");
        zh.put("error.cmd_exception", "执行指令 /{0} 时插件 {1} 抛出异常：{2}");
        zh.put("error.no_permission", "你没有权限执行此指令");
        zh.put("error.config_load", "加载配置失败：{0}");
        zh.put("error.config_save", "保存配置失败：{0}");
        zh.put("log.reload", "重载插件...");
        zh.put("log.reloaded", "插件重载完成");
        zh.put("broadcast.prefix", "[广播] ");
        zh.put("welcome.prefix", "[欢迎] ");
        zh.put("welcome.joined", "{0} 来到了服务器");
        zh.put("scanner.paper", "Paper");
        zh.put("scanner.spigot", "Spigot");
        zh.put("scanner.bukkit", "Bukkit");
        zh.put("scanner.fabric", "Fabric");
        zh.put("scanner.forge", "Forge");
        zh.put("scanner.unknown", "Unknown");
        zh.put("scanner.runnable", "可运行");
        zh.put("scanner.identified", "仅识别");
        zh.put("scanner.failed", "失败");
        zh.put("scanner.needs_deps", "需依赖");
        zh.put("scanner.header", "──────── 插件扫描报告 ────────");
        zh.put("scanner.ecosystem", "生态分布：");
        zh.put("scanner.summary", "可运行：{0}  仅识别：{1}  失败：{2}  未知：{3}");
        zh.put("scanner.runnable_item", "  [可运行] {0}");
        zh.put("scanner.identified_item", "  [仅识别] {0} — {1}");
        zh.put("scanner.failed_item", "  [失败]   {0} — {1}");
        zh.put("scanner.unknown_item", "  [未知]   {0}");
        zh.put("scanner.footer", "──────────────────────────────");
        zh.put("load.no_plugins", "plugins 目录中没有插件");
        zh.put("load.bad_metadata", "插件 {0} 元数据无效：{1}");
        zh.put("load.missing_depend", "插件 {0} 缺少必需依赖 {1}，已跳过");
        zh.put("load.depend_cycle", "检测到插件依赖循环，涉及 {0}，将按发现顺序加载");
        zh.put("load.depend_not_found", "插件 {0} 的硬依赖 {1} 未找到");
        zh.put("load.enable_fail", "启用插件 {0} 失败：{1}");
        zh.put("load.disable_fail", "禁用插件 {0} 时出错：{1}");
        zh.put("load.listener_bad_params", "监听器方法 {0} 参数数量不为 1，已忽略");
        zh.put("load.listener_not_event", "监听器方法 {0} 参数不是 Event，已忽略");
        zh.put("load.event_handler_error", "插件 {0} 的事件处理器 {1} 抛出异常：{2}");

        // --- 原版风格指令 ---
        zh.put("vanilla.list", "当前在线 0 名玩家，上限 {1} 人：无");
        zh.put("vanilla.seed", "世界种子：[{0}]");
        zh.put("vanilla.seed_random", "随机（未设置）");
        zh.put("vanilla.time", "世界时间 {0} tick（第 {1} 天）");
        zh.put("vanilla.difficulty_get", "当前难度：{0}");
        zh.put("vanilla.difficulty_set", "难度已设为 {0}");
        zh.put("vanilla.gamemode_get", "当前默认游戏模式：{0}");
        zh.put("vanilla.gamemode_set", "默认游戏模式已设为 {0}");
        zh.put("vanilla.bad_value", "无效参数 {0}，可选：{1}");
        zh.put("vanilla.need_player", "请指定玩家名");
        zh.put("vanilla.need_ip", "请指定 IP 地址");
        zh.put("vanilla.gamerule_list", "可用游戏规则：{0}");
        zh.put("vanilla.gamerule_get", "游戏规则 {0}");
        zh.put("vanilla.gamerule_set", "游戏规则 {0} 已设为 {1}");
        zh.put("vanilla.op_add", "已将 {0} 设为管理员");
        zh.put("vanilla.op_remove", "已取消 {0} 的管理员身份");
        zh.put("vanilla.op_already", "{0} 已经是管理员");
        zh.put("vanilla.op_not", "{0} 不是管理员");
        zh.put("vanilla.whitelist_on", "白名单已启用");
        zh.put("vanilla.whitelist_off", "白名单已关闭");
        zh.put("vanilla.whitelist_list", "白名单共 {0} 人：{1}");
        zh.put("vanilla.whitelist_add", "已将 {0} 加入白名单");
        zh.put("vanilla.whitelist_remove", "已将 {0} 移出白名单");
        zh.put("vanilla.whitelist_reloaded", "白名单已重新加载");
        zh.put("vanilla.ban_add", "已封禁玩家 {0}");
        zh.put("vanilla.ban_remove", "已解除对 {0} 的封禁");
        zh.put("vanilla.banlist", "封禁名单共 {0} 人：{1}");
        zh.put("vanilla.banip_add", "已封禁 IP {0}");
        zh.put("vanilla.banip_remove", "已解除对 IP {0} 的封禁");
        zh.put("vanilla.kick", "已踢出玩家 {0}（本内核无在线玩家）");
        zh.put("vanilla.tell", "对 {0} 说：{1}");
        zh.put("vanilla.saved", "已保存全部数据");
        zh.put("vanilla.save_toggle", "保存功能：{0}（本内核仅保存配置与名单）");
        zh.put("vanilla.unsupported_world", "指令 /{0} 需要世界数据，本内核不实现世界");
        zh.put("vanilla.needs_world", "指令 /{0} 需要游戏世界/实体，本内核不实现");
        zh.put("config.read_fail", "读取 {0} 失败：{1}");
        zh.put("config.write_fail", "写入 {0} 失败：{1}");
        zh.put("playerlist.read_fail", "读取玩家名单 {0} 失败：{1}");
        zh.put("playerlist.write_fail", "写入玩家名单 {0} 失败：{1}");

        // --- 帮助 ---
        zh.put("help.info", "信息：plugins(pl) / version(ver) / tps / status / help");
        zh.put("help.world", "世界：time / seed / difficulty / gamemode / gamerule / save-all");
        zh.put("help.players", "玩家：list / op / deop / ban / pardon / banlist / ban-ip / pardon-ip / kick / whitelist");
        zh.put("help.server", "服务端：stop / reload / say / me / tell");
        zh.put("help.chat", "聊天：say <消息> / me <动作> / tell <玩家> <消息>");
        zh.put("help.other", "提示：/give /tp /summon 等需要世界的指令会明确提示不支持");

        // --- 状态 ---
        zh.put("status.header", "──────── 服务端状态 ────────");
        zh.put("status.version", "内核 v{0}，目标 MC {1}，Java {2}");
        zh.put("status.uptime", "已运行 {0}");
        zh.put("status.tick", "tick {0}，活跃任务 {1}");
        zh.put("status.events", "事件处理器 {0} 个");
        zh.put("status.plugins", "已加载插件 {0} 个");
        zh.put("status.motd", "服务器名称：{0}");
        zh.put("status.port", "监听 {1}:{0}");
        zh.put("status.mode", "认证模式：{0}");
        zh.put("status.mode_online", "正版验证");
        zh.put("status.mode_offline", "离线模式");
        zh.put("status.difficulty", "难度 {0}，默认模式 {1}");
        zh.put("status.max_players", "最大玩家数 {0}");
        zh.put("status.lists", "OP {0} 人，白名单 {1} 人，封禁 {2} 人");
        zh.put("status.footer", "────────────────────────────");

        // --- 外壳模式 ---
        zh.put("shell.mode", "外壳模式：{0}");
        zh.put("shell.fetch_manifest", "正在获取 Mojang 版本清单...");
        zh.put("shell.resolve_version", "已确定版本：{0}");
        zh.put("shell.downloading", "正在下载服务端 {0}（{1}）");
        zh.put("shell.downloaded", "服务端下载完成：{0}（{1}）");
        zh.put("shell.already_cached", "已存在且校验通过：{0}（{1}）");
        zh.put("shell.download_skip", "无法下载新版本，使用本地已有 server.jar（{0}）");
        zh.put("shell.download_fail", "下载服务端失败：{0}");
        zh.put("shell.starting", "正在启动服务端：{0}");
        zh.put("shell.exited", "服务端已退出，退出码 {0}");
        zh.put("shell.fail", "外壳启动失败：{0}");
        BUNDLES.put("zh_CN", zh);

        // 英文
        java.util.Map<String, String> en = new java.util.LinkedHashMap<>();
        en.put("server.starting", "======================================");
        en.put("server.version", "  Minecraft Server Kernel v{0}");
        en.put("server.target", "  Target MC Version {0}");
        en.put("server.dir", "  Working Directory {0}");
        en.put("server.ending", "======================================");
        en.put("scan.found", "Found {0} plugin files, starting scan");
        en.put("scan.identified", "Identified plugin {0} v{1} (main class {2})");
        en.put("scan.report", "Scan Report:");
        en.put("scan.runnable", "  Runnable: {0}  Identified Only: {1}  Failed: {2}  Unknown: {3}");
        en.put("scan.runnable_item", "  [Runnable] {0} v{1}");
        en.put("scan.identified_item", "  [Identified Only] {0} v{1}");
        en.put("scan.failed_item", "  [Failed] {0} v{1}  Reason: {2}");
        en.put("scan.unknown_item", "  [Unknown] {0}");
        en.put("scan.needs_deps", "  [Missing Deps] {0} v{1}  Missing Classes: {2}");
        en.put("load.plugin", "Loading plugin {0} v{1}");
        en.put("load.onload", "  {0} loading (onLoad)");
        en.put("load.datadir", "  Data Folder: {0}");
        en.put("load.servername", "  Server: {0}");
        en.put("load.onenable", "  {0} enabled (onEnable)");
        en.put("load.events", "Plugin {0} registered {1} event handlers");
        en.put("load.enabled", "  [OK] Plugin {0} v{1} enabled");
        en.put("load.error_instantiate", "Plugin {0} instantiation failed: {1}");
        en.put("load.error_onload", "Plugin {0} onLoad error: {1}");
        en.put("load.error_onenable", "Plugin {0} onEnable error: {1}");
        en.put("load.summary", "Plugin loading complete: {0} succeeded, {1} failed");
        en.put("server.started", "Server started, type help for commands, stop to shutdown");
        en.put("server.shutdown", "Stop command received, shutting down...");
        en.put("server.disabled", "Plugin {0} disabled");
        en.put("server.closed", "Server closed");
        en.put("cmd.unknown", "Unknown command: {0} (type help for available commands)");
        en.put("cmd.plugins", "Loaded plugins ({0}):");
        en.put("cmd.plugin_entry", "  - {0} v{1} [{2}]");
        en.put("cmd.version", "Server Kernel v{0}, Target MC {1}, Java {2}");
        en.put("cmd.tps", "Current tick {0}, Active tasks {1}, Event handlers {2}");
        en.put("cmd.help", "Available Commands:");
        en.put("cmd.help.plugins", "  plugins  List loaded plugins");
        en.put("cmd.help.version", "  version  Show version info");
        en.put("cmd.help.tps", "  tps      Show runtime stats");
        en.put("cmd.help.say", "  say <msg> Broadcast message");
        en.put("cmd.help.help", "  help     Show this help");
        en.put("cmd.help.stop", "  stop     Shutdown server");
        en.put("cmd.help.plugin_cmds", "  Plugin Commands: {0}");
        en.put("load.enabled_state", "Enabled");
        en.put("load.disabled_state", "Disabled");
        en.put("load.failed_state", "Failed");
        en.put("error.class_not_found", "Class linkage failed: {0} (not provided by host, not bundled in plugin)");
        en.put("error.class_not_found_jar", "Class not found: {0} (plugin did not bundle this dependency)");
        en.put("error.init_failed", "Initialization failed: {0}");
        en.put("error.inject_failed", "Dependency injection failed: {0}");
        en.put("error.task_exception", "Task #{0} (plugin {1}) exception: {2}");
        en.put("error.cmd_exception", "Command /{0} from plugin {1} threw exception: {2}");
        en.put("error.no_permission", "You don't have permission to execute this command");
        en.put("error.config_load", "Failed to load config: {0}");
        en.put("error.config_save", "Failed to save config: {0}");
        en.put("log.reload", "Reloading plugins...");
        en.put("log.reloaded", "Plugins reloaded");
        en.put("broadcast.prefix", "[Broadcast] ");
        en.put("welcome.prefix", "[Welcome] ");
        en.put("welcome.joined", "{0} joined the server");
        en.put("scanner.paper", "Paper");
        en.put("scanner.spigot", "Spigot");
        en.put("scanner.bukkit", "Bukkit");
        en.put("scanner.fabric", "Fabric");
        en.put("scanner.forge", "Forge");
        en.put("scanner.unknown", "Unknown");
        en.put("scanner.runnable", "Runnable");
        en.put("scanner.identified", "Identified Only");
        en.put("scanner.failed", "Failed");
        en.put("scanner.needs_deps", "Missing Deps");
        en.put("scanner.header", "──────── Plugin Scan Report ────────");
        en.put("scanner.ecosystem", "Ecosystem Distribution:");
        en.put("scanner.summary", "Runnable: {0}  Identified Only: {1}  Failed: {2}  Unknown: {3}");
        en.put("scanner.runnable_item", "  [Runnable] {0}");
        en.put("scanner.identified_item", "  [Identified Only] {0} — {1}");
        en.put("scanner.failed_item", "  [Failed]   {0} — {1}");
        en.put("scanner.unknown_item", "  [Unknown]   {0}");
        en.put("scanner.footer", "────────────────────────────────────");
        en.put("load.no_plugins", "No plugins in plugins/ directory");
        en.put("load.bad_metadata", "Plugin {0} has invalid metadata: {1}");
        en.put("load.missing_depend", "Plugin {0} is missing required dependency {1}, skipped");
        en.put("load.depend_cycle", "Plugin dependency cycle detected involving {0}, loading in discovery order");
        en.put("load.depend_not_found", "Plugin {0} hard dependency {1} not found");
        en.put("load.enable_fail", "Failed to enable plugin {0}: {1}");
        en.put("load.disable_fail", "Error disabling plugin {0}: {1}");
        en.put("load.listener_bad_params", "Listener method {0} has wrong parameter count, ignored");
        en.put("load.listener_not_event", "Listener method {0} parameter is not an Event, ignored");
        en.put("load.event_handler_error", "Event handler {1} of plugin {0} threw exception: {2}");

        // --- Vanilla-style commands ---
        en.put("vanilla.list", "There are 0 of a max of {1} players online: none");
        en.put("vanilla.seed", "World seed: [{0}]");
        en.put("vanilla.seed_random", "random (not set)");
        en.put("vanilla.time", "World time {0} ticks (day {1})");
        en.put("vanilla.difficulty_get", "Current difficulty: {0}");
        en.put("vanilla.difficulty_set", "Difficulty set to {0}");
        en.put("vanilla.gamemode_get", "Default game mode: {0}");
        en.put("vanilla.gamemode_set", "Default game mode set to {0}");
        en.put("vanilla.bad_value", "Invalid argument {0}, expected: {1}");
        en.put("vanilla.need_player", "Please specify a player name");
        en.put("vanilla.need_ip", "Please specify an IP address");
        en.put("vanilla.gamerule_list", "Available gamerules: {0}");
        en.put("vanilla.gamerule_get", "Gamerule {0}");
        en.put("vanilla.gamerule_set", "Gamerule {0} set to {1}");
        en.put("vanilla.op_add", "Made {0} a server operator");
        en.put("vanilla.op_remove", "Removed {0} from server operators");
        en.put("vanilla.op_already", "{0} is already a server operator");
        en.put("vanilla.op_not", "{0} is not a server operator");
        en.put("vanilla.whitelist_on", "Whitelist enabled");
        en.put("vanilla.whitelist_off", "Whitelist disabled");
        en.put("vanilla.whitelist_list", "There are {0} whitelisted players: {1}");
        en.put("vanilla.whitelist_add", "Added {0} to the whitelist");
        en.put("vanilla.whitelist_remove", "Removed {0} from the whitelist");
        en.put("vanilla.whitelist_reloaded", "Whitelist reloaded");
        en.put("vanilla.ban_add", "Banned player {0}");
        en.put("vanilla.ban_remove", "Unbanned player {0}");
        en.put("vanilla.banlist", "There are {0} banned players: {1}");
        en.put("vanilla.banip_add", "Banned IP {0}");
        en.put("vanilla.banip_remove", "Unbanned IP {0}");
        en.put("vanilla.kick", "Kicked player {0} (no online players in this kernel)");
        en.put("vanilla.tell", "Told {0}: {1}");
        en.put("vanilla.saved", "Saved everything");
        en.put("vanilla.save_toggle", "Saving: {0} (config and player lists only)");
        en.put("vanilla.unsupported_world", "Command /{0} needs world data, not implemented");
        en.put("vanilla.needs_world", "Command /{0} needs world/entities, not implemented");
        en.put("config.read_fail", "Failed to read {0}: {1}");
        en.put("config.write_fail", "Failed to write {0}: {1}");
        en.put("playerlist.read_fail", "Failed to read player list {0}: {1}");
        en.put("playerlist.write_fail", "Failed to write player list {0}: {1}");

        // --- Help ---
        en.put("help.info", "Info: plugins(pl) / version(ver) / tps / status / help");
        en.put("help.world", "World: time / seed / difficulty / gamemode / gamerule / save-all");
        en.put("help.players", "Players: list / op / deop / ban / pardon / banlist / ban-ip / pardon-ip / kick / whitelist");
        en.put("help.server", "Server: stop / reload / say / me / tell");
        en.put("help.chat", "Chat: say <msg> / me <action> / tell <player> <msg>");
        en.put("help.other", "Note: world commands such as /give /tp /summon report unsupported");

        // --- Status ---
        en.put("status.header", "──────── Server Status ────────");
        en.put("status.version", "Kernel v{0}, target MC {1}, Java {2}");
        en.put("status.uptime", "Uptime {0}");
        en.put("status.tick", "tick {0}, active tasks {1}");
        en.put("status.events", "Event handlers: {0}");
        en.put("status.plugins", "Loaded plugins: {0}");
        en.put("status.motd", "MOTD: {0}");
        en.put("status.port", "Listening on {1}:{0}");
        en.put("status.mode", "Auth mode: {0}");
        en.put("status.mode_online", "online-mode");
        en.put("status.mode_offline", "offline-mode");
        en.put("status.difficulty", "Difficulty {0}, default mode {1}");
        en.put("status.max_players", "Max players {0}");
        en.put("status.lists", "OPs {0}, whitelist {1}, banned {2}");
        en.put("status.footer", "────────────────────────────");

        // --- Shell mode ---
        en.put("shell.mode", "Shell mode: {0}");
        en.put("shell.fetch_manifest", "Fetching Mojang version manifest...");
        en.put("shell.resolve_version", "Resolved version: {0}");
        en.put("shell.downloading", "Downloading server {0} ({1})");
        en.put("shell.downloaded", "Server downloaded: {0} ({1})");
        en.put("shell.already_cached", "Already cached and verified: {0} ({1})");
        en.put("shell.download_skip", "Cannot download new version, using local server.jar ({0})");
        en.put("shell.download_fail", "Failed to download server: {0}");
        en.put("shell.starting", "Starting server: {0}");
        en.put("shell.exited", "Server exited with code {0}");
        en.put("shell.fail", "Shell failed to start: {0}");
        BUNDLES.put("en_US", en);
    }

    /** 设置当前语言（zh_CN / en_US）。 */
    public static void setLanguage(String lang) {
        if (BUNDLES.containsKey(lang)) {
            currentLang = lang;
        } else {
            System.err.println("[I18n] Unknown language: " + lang + ", fallback to zh_CN");
            currentLang = "zh_CN";
        }
    }

    /** 获取当前语言。 */
    public static String getLanguage() {
        return currentLang;
    }

    /** 当前语言是否有该键。 */
    public static boolean has(String key) {
        return BUNDLES.get(currentLang).containsKey(key)
                || BUNDLES.get("zh_CN").containsKey(key);
    }

    /** 获取消息（无参数）。 */
    public static String get(String key) {
        java.util.Map<String, String> bundle = BUNDLES.get(currentLang);
        String msg = bundle.get(key);
        if (msg == null) {
            // 回退到中文
            msg = BUNDLES.get("zh_CN").get(key);
        }
        return msg == null ? "!" + key + "!" : msg;
    }

    /** 获取消息（单参数）。 */
    public static String get(String key, Object arg1) {
        return format(get(key), arg1);
    }

    /** 获取消息（双参数）。 */
    public static String get(String key, Object arg1, Object arg2) {
        return format(get(key), arg1, arg2);
    }

    /** 获取消息（三参数）。 */
    public static String get(String key, Object arg1, Object arg2, Object arg3) {
        return format(get(key), arg1, arg2, arg3);
    }

    /** 获取消息（可变参数）。 */
    public static String get(String key, Object... args) {
        return format(get(key), args);
    }

    private static String format(String template, Object... args) {
        if (args == null || args.length == 0) return template;
        String result = template;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", args[i] == null ? "null" : args[i].toString());
        }
        return result;
    }
}