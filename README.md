# mc-server

**作者：chenhuitony**

> 一个从零手写的 Minecraft 服务端内核，实现 Bukkit API 子集，能真实加载并运行 Paper/Spigot/Bukkit 插件 JAR。
> 无任何第三方依赖（纯 JDK），单文件 60KB 级别，适合学习插件加载原理、二次开发或作为轻量级插件测试环境。

---

## 特性

| 项目 | 说明 |
|------|------|
| **零依赖** | 仅需 JDK 21+，无 Maven/Gradle，无外部 JAR |
| **真实插件加载** | 实现 `JavaPlugin`、`PluginManager`、`BukkitScheduler`、`ServicesManager` 等核心 API，Vault、LuckPerms 等主流插件可直接放入 `plugins/` 运行 |
| **多生态识别** | 自动识别 Paper/Spigot/Bukkit/Fabric/Forge 插件，扫描时报告「可运行 / 仅识别 / 需依赖 / 失败」 |
| **中文控制台** | 启动横幅、日志、报错全部中文，UTF-8 编码 |
| **20 TPS 主循环** | 同步/异步任务调度、事件总线、指令分发完整实现 |
| **配置系统** | `FileConfiguration` / `YamlConfiguration` 兼容插件读写 `config.yml` |
| **权限/经济服务** | `ServicesManager` 让 Vault 等服务插件注册/查找服务实现 |
| **跨平台** | Windows/Linux/macOS 均可运行 |

---

## 目录结构

```
mc-server/
├── src/main/java/           # 源码
│   ├── com/mc/              # 内核实现
│   │   ├── util/            # YAML/JSON/日志/JUL桥接
│   │   ├── plugin/          # 插件类加载器、已加载插件模型
│   │   ├── server/          # McServer、调度器、服务管理器、插件管理器
│   │   ├── loader/          # Paper/Fabric/Forge 加载器适配 + 扫描器
│   │   └── Main.java        # 入口（--dir、--scan-only、--help）
│   └── org/bukkit/          # Bukkit API 子集实现
│       ├── command/         # Command、PluginCommand、TabCompleter...
│       ├── configuration/   # ConfigurationSection、MemorySection...
│       ├── configuration/file/  # FileConfiguration、YamlConfiguration...
│       ├── entity/          # Player、OfflinePlayer
│       ├── event/           # Event、Listener、EventPriority...
│       ├── permissions/     # Permission、PermissionDefault
│       ├── plugin/          # Plugin、JavaPlugin、PluginManager...
│       ├── scheduler/       # BukkitScheduler、BukkitTask
│       ├── craftbukkit/     # CraftServer 标记类（供版本探测）
│       └── Bukkit.java      # 静态工具入口
├── test-plugin/             # 自带演示插件
├── run/                     # 运行目录
│   └── plugins/             # 放插件 JAR 的地方
├── build.bat                # 编译脚本（ASCII-only）
├── start.bat                # 启动脚本
└── README.md                # 本文件
```

---

## 快速开始

### 1. 环境要求
- **JDK 21+**（推荐 Eclipse Temurin 21/25）
- Windows PowerShell / CMD，或 Linux/macOS 终端

### 2. 编译
```cmd
build.bat
```
输出：`out/` 目录下生成所有 `.class` 文件。

### 3. 准备插件
将 Paper/Spigot/Bukkit 插件 JAR 放入 `run/plugins/`：
```text
run/plugins/
├── Vault-34315.jar
├── LuckPerms-Bukkit-5.5.71.jar
└── YourPlugin.jar
```
> ⚠️ LuckPerms 需要 Guava/Adventure 库，本内核不内置，会报「仅识别/加载失败」，属正常现象。

### 4. 启动
```cmd
start.bat
```
或直接：
```cmd
cmd /c "java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out com.mc.Main --dir run"
```

### 5. 控制台指令
| 指令 | 说明 |
|------|------|
| `help` / `?` | 显示帮助 |
| `plugins` / `pl` | 列出已加载插件 |
| `version` / `ver` | 显示版本 |
| `tps` | 显示 tick、任务数、事件处理器数 |
| `say <消息>` | 广播消息 |
| `stop` / `exit` | 关闭服务端 |
| `/插件指令` | 插件注册的指令（如 `/lp`、 `/vault`） |

---

## API 兼容性说明

本内核实现了 **Bukkit API 核心子集**，重点覆盖：
- `JavaPlugin` 生命周期：`onLoad` → `onEnable` → `onDisable`
- `PluginManager`：插件加载、指令注册、事件注册、权限节点
- `BukkitScheduler`：`runTask`、`runTaskLater`、`runTaskTimer`、异步对应方法，**返回 `BukkitTask`**
- `ServicesManager`：`register`/`load`/`getRegistration`/`getKnownServices`
- `Configuration`：`FileConfiguration`、`YamlConfiguration`、`MemorySection`
- `Command` 体系：`CommandExecutor`（真实签名 `onCommand(sender, command, label, args)`）、`TabCompleter`、`PluginCommand`
- 事件总线：`Event`、`Listener`、`EventPriority`、`@EventHandler`、同步分发
- `OfflinePlayer` / `Player` / `Permission` / `PermissionDefault`
- `Bukkit` 静态工具类：`getServer()`、`getPluginManager()`、`getScheduler()`、`getServicesManager()`、`getConsoleSender()`、`getOnlinePlayers()`、`broadcastMessage()` 等

> **不支持**：任何依赖 NMS (`net.minecraft.*`)、CraftBukkit 内部实现、Mixin、Forge/Fabric 模组加载器核心逻辑的插件/模组。这些会在加载时报出明确的 `NoClassDefFoundError`/`ClassNotFoundException`，并在控制台给出「宿主未提供该类 / 插件未把该依赖打包进 JAR」等中文提示。

---

## 扫描报告示例

```
======================================
  Minecraft 服务端内核 v1.0.0
  目标版本 MC 1.21.8
  运行目录 E:\mc-server\run
======================================
发现 3 个插件文件，开始扫描
识别插件 DemoPlugin v1.0.0（主类 com.demo.DemoPlugin）
识别插件 LuckPerms v5.5.71（主类 me.lucko.luckperms.bukkit.loader.BukkitLoaderPlugin）
识别插件 Vault v1.7.3-b131（主类 net.milkbowl.vault.Vault）
扫描报告：
  可运行：2  仅识别：0  失败：1  未知：0
  [可运行] DemoPlugin v1.0.0
  [可运行] Vault v1.7.3-b131
  [需依赖] LuckPerms v5.5.71  缺少类: com/google/common/collect/ImmutableList, me/lucko/luckperms/lib/adventure/...
```

---

## 自带演示插件

`test-plugin/` 是一个最小 `JavaPlugin`，演示：
- `onLoad` / `onEnable` / `onDisable` 生命周期
- `Listener` 监听 `PlayerJoinEvent`（重写加入消息为 `[欢迎] xxx 来到了服务器`）
- `CommandExecutor` 实现 `/greet [名字]`、`/demo info|join|count|broadcast`
- `BukkitScheduler` 周期任务（每 20 tick 打印心跳、在线人数）
- `getDataFolder()` 写入 `enabled.txt`

编译并打包：
```cmd
cd test-plugin
javac -d out -encoding UTF-8 -cp ..\out src\main\java\com\demo\DemoPlugin.java
copy src\main\resources\plugin.yml out\plugin.yml
jar --create --file ..\run\plugins\DemoPlugin.jar -C out .
```

---

## 设计要点

| 模块 | 关键点 |
|------|--------|
| **PluginClassLoader** | 父优先加载 `org.bukkit.*`、`io.papermc.*`、`net.kyori.*`、`com.destroystokyo.*`；其余类插件内部优先，实现插件间隔离 |
| **LoadedPlugin** | 反射注入 `server/description/dataFolder/logger/file/enabled`，兜底写私有字段，兼容老旧插件 |
| **JulBridge** | 拦截 `java.util.logging`，统一重定向到内核 `Log`（中文、带时间戳、去重） |
| **PluginManagerImpl.registerEvents** | 实例+方法去重，防止手动注册+注解扫描双重注册 |
| **McScheduler** | 20 TPS 主线程驱动同步任务，异步任务交给守护线程池，返回 `BukkitTask` 可取消 |
| **ServicesManagerImpl** | 优先级队列，`load`/`getRegistration`/`getRegistrations`/`getKnownServices()` 全实现 |

---

## 为什么不用 Paper/Spigot？

- **学习价值**：从零写内核能彻底理解插件加载机制、类隔离、事件总线、调度器
- **极简体积**：编译后 `out/` 不到 500KB，启动 < 1s，适合 CI/单元测试/插件原型验证
- **可控性**：每一行代码可读、可改，遇到插件报错能精确定位到缺哪个 API
- **扩展性**：`PluginLoader` 接口 + `PluginScanner` 已预留 Paper/Fabric/Forge 适配槽位，按同模式继续实现即可

---

## 常见问题

**Q: 插件报 `NoClassDefFoundError: org/bukkit/...` 怎么办？**  
A: 说明内核缺该 API 类，请提 Issue 或自行在 `org.bukkit.*` 下补全。

**Q: LuckPerms / 其它复杂插件加载失败？**  
A: 它们依赖 Guava、Adventure、HikariCP 等第三方库，本内核不打包这些库。你可以：
1. 把依赖 JAR 放入插件自身（Shade 打包）
2. 或在内核 classpath 加入这些库（不推荐，违背零依赖初衷）

**Q: 能跑模组？**  
A: 不能。Fabric/Forge 需要 Mixin + 修补过的 Minecraft 类，本内核无网络/世界/实体实现。

**Q: 如何自定义加载器？**  
A: 实现 `com.mc.loader.PluginLoader` 接口，在 `PluginScanner` 注册即可。

---

## 许可证

MIT License — 详见 [LICENSE](LICENSE)

---

## 作者

**chenhuitony**  
GitHub: https://github.com/chenhuitony  
如有建议或发现 Bug，欢迎提 Issue 或 PR！