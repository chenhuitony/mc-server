# mc-server

**作者：chenhuitony**

> 一个从零手写的 Minecraft 服务端内核，实现 Bukkit API 子集，能真实加载并运行 Paper/Spigot/Bukkit 插件 JAR。
> 也可作为**原版服务端的外壳**：自动下载并运行官方 `server.jar`，把控制台输出翻译为中文。
> 零第三方依赖（纯 JDK），打包后 `build/mc-server.jar` 约 300KB。

---

## 两种运行模式

| 模式 | 启动方式 | 说明 |
|------|----------|------|
| **内核模式**（默认） | `start.bat` | 自研 Bukkit API 子集，直接加载 `plugins/` 里的插件 JAR，无网络/世界层 |
| **外壳模式** | `start-shell.bat` | 自动下载官方 `server.jar` 并运行，完整原版功能，控制台输出翻译为中文 |

两种模式都在同一个 JAR 里，用 `--shell vanilla` 切换。

---

## 特性

| 项目 | 说明 |
|------|------|
| **零依赖** | 仅需 JDK 21+，无 Maven/Gradle，无外部 JAR |
| **真实插件加载** | 实现 `JavaPlugin`、`PluginManager`、`BukkitScheduler`、`ServicesManager` 等核心 API，Vault 等插件可直接运行 |
| **原版外壳** | 从 Mojang 清单解析版本、下载 `server.jar`（SHA-1 校验）、启动并翻译控制台 |
| **中英双语** | `--lang zh_CN` / `--lang en_US`，覆盖日志、指令、扫描报告、状态 |
| **原版指令集** | `list`/`time`/`seed`/`difficulty`/`gamemode`/`gamerule`/`op`/`deop`/`ban`/`pardon`/`banlist`/`ban-ip`/`whitelist`/`kick`/`say`/`me`/`tell`/`save-all`/`status`/`stop` |
| **原版配置** | 生成与官方键名一致的 `server.properties`（含中文注释），`ops.json`/`whitelist.json`/`banned-*.json` 格式互通 |
| **多生态识别** | 自动识别 Paper/Spigot/Bukkit/Fabric/Forge，报告「可运行 / 仅识别 / 需依赖 / 失败」 |
| **20 TPS 主循环** | 同步/异步任务调度、事件总线、指令分发 |
| **配置系统** | `FileConfiguration` / `YamlConfiguration` 兼容插件读写 `config.yml` |
| **跨平台** | Windows / Linux / macOS |

---

## 快速开始

### 1. 环境
- **JDK 21+**（推荐 Eclipse Temurin 21/25）
- Windows CMD / PowerShell，或 Linux/macOS 终端

### 2. 编译打包
```cmd
build.bat
```
输出 `build/mc-server.jar`。

### 3. 运行

**内核模式**（自研内核加载插件）：
```cmd
start.bat
```
或
```cmd
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar build/mc-server.jar --dir run
```

**外壳模式**（真实原版服务端）：
```cmd
start-shell.bat
```
或指定版本：
```cmd
java -jar build/mc-server.jar --dir run --shell vanilla --version 1.21.8
```

### 4. 命令行参数

| 参数 | 说明 |
|------|------|
| `--dir <目录>` | 运行目录（默认当前目录） |
| `--lang zh_CN\|en_US` | 界面语言，默认 zh_CN |
| `--shell vanilla\|paper` | 启用外壳模式 |
| `--version <MC版本>` | 外壳模式下指定下载版本，默认最新正式版 |
| `--scan-only` | 只扫描插件并退出 |
| `--help` | 显示帮助 |

---

## 插件

放到 `run/plugins/`：

```text
run/plugins/
├── Vault-34315.jar
├── YourPlugin.jar
└── ...
```

---

## 原版指令集

内核模式下可用（语义与原版对齐）：

| 分类 | 指令 |
|------|------|
| 信息 | `plugins`/`pl`、`version`/`ver`、`tps`、`status`、`help` |
| 世界 | `time`、`seed`、`difficulty`、`gamemode`、`gamerule`、`save-all` |
| 玩家 | `list`、`op`、`deop`、`ban`、`pardon`、`banlist`、`ban-ip`、`pardon-ip`、`kick`、`whitelist on\|off\|list\|add\|remove` |
| 服务端 | `stop`/`exit`/`quit`、`reload`、`say`、`me`、`tell` |
| 需要世界 | `give`、`tp`、`summon`、`effect`、`playsound`、`weather` 等会明确提示「本内核不实现世界」 |

> 外壳模式下，以上指令由真实原版服务端处理，功能完整。

---

## 配置文件

首次启动自动生成：

| 文件 | 说明 |
|------|------|
| `server.properties` | 与原版键名一致，含中英文注释 |
| `ops.json` | OP 列表（UUID v3 离线 UUID，与原版一致） |
| `whitelist.json` | 白名单 |
| `banned-players.json` | 玩家封禁 |
| `banned-ips.json` | IP 封禁 |
| `eula.txt` | 外壳模式下自动写 `eula=true` |

这些文件与官方服务端**格式互通**，可直接复制已有文件过来。

---

## API 兼容性

已实现：
- `JavaPlugin` 生命周期：`onLoad` → `onEnable` → `onDisable`
- `PluginManager`：加载、指令、事件、权限节点
- `BukkitScheduler`：`runTask`/`runTaskLater`/`runTaskTimer` + 异步版本，返回 `BukkitTask`
- `ServicesManager`：`register`/`load`/`getRegistration`/`getKnownServices`
- `Configuration`：`FileConfiguration`/`YamlConfiguration`/`MemorySection`
- `Command` 体系：`CommandExecutor`（真实签名）、`TabCompleter`、`PluginCommand`
- 事件总线：`Event`/`Listener`/`EventPriority`/`@EventHandler`
- `Bukkit` 静态入口：`getServer()`/`getPluginManager()`/`getScheduler()`/`getServicesManager()`/`getConsoleSender()`/`broadcastMessage()`
- `OfflinePlayer`/`Player`/`Permission`/`PermissionDefault`
- 精简 Gson 兼容层（`com.google.gson.*`），让引用 Gson 的插件能链接

**不支持**：依赖 NMS (`net.minecraft.*`)、CraftBukkit 内部实现、Mixin、Fabric/Forge 加载器的插件或模组。这类会在加载时明确报错，并提示缺哪个类。

---

## 实测结果

| 插件 | 结果 |
|------|------|
| 内置 DemoPlugin | ✅ 完整运行（生命周期、事件、指令、调度器、数据目录） |
| Vault 1.7.3 | ✅ 加载并启用（注册服务、权限系统、事件） |
| LuckPerms 5.5.71 | ⚠️ 识别成功，缺 Guava/Adventure（其自身未 shade），报错清晰 |

---

## 目录结构

```
mc-server/
├── build/
│   └── mc-server.jar        # 打包产物
├── src/main/java/
│   ├── com/mc/
│   │   ├── util/            # YAML/JSON/日志/i18n/server.properties/玩家名单
│   │   ├── plugin/          # 类加载器、已加载插件模型
│   │   ├── server/          # McServer、调度器、服务管理器、插件管理器
│   │   ├── loader/          # Paper/Fabric/Forge 适配 + 扫描器
│   │   ├── shell/           # 外壳模式：下载器、启动器、控制台翻译
│   │   └── Main.java        # 入口
│   └── org/bukkit/          # Bukkit API 子集
├── test-plugin/             # 演示插件
├── run/                     # 运行目录
├── build.bat                # 编译打包
├── start.bat                # 内核模式启动
├── start-shell.bat          # 外壳模式启动
├── README.md
└── LICENSE (MIT)
```

---

## 作者

**chenhuitony** — https://github.com/chenhuitony
