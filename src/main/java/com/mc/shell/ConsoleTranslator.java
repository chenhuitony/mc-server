package com.mc.shell;

import com.mc.util.I18n;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 原版服务端控制台输出翻译器。
 *
 * <p>把官方服务端的英文日志转换为当前语言，保留原始时间戳与日志级别。
 * 翻译采用规则匹配（正则），不依赖完整翻译表，覆盖最常见的启动/关闭/玩家事件。
 */
public class ConsoleTranslator {

    /** 一条翻译规则。 */
    private static final class Rule {
        final Pattern pattern;
        final String template;   // 用 $1..$9 引用捕获组
        Rule(String regex, String template) {
            this.pattern = Pattern.compile(regex);
            this.template = template;
        }
    }

    private final Map<String, Rule[]> rules = new LinkedHashMap<>();

    public ConsoleTranslator() {
        boolean zh = "zh_CN".equals(I18n.getLanguage());

        // ---------- 中文规则 ----------
        rules.put("zh_CN", new Rule[]{
                new Rule("^Starting minecraft server version (.+)$",
                        "正在启动 Minecraft 服务端，版本 $1"),
                new Rule("^Loading properties$", "正在读取服务端配置"),
                new Rule("^Default game type: (.+)$", "默认游戏模式：$1"),
                new Rule("^Generating keypair$", "正在生成密钥对"),
                new Rule("^Starting Minecraft server on (.+)$", "正在启动服务端，监听 $1"),
                new Rule("^Using epoll channel type$", "使用 epoll 通道"),
                new Rule("^Using default channel type$", "使用默认通道"),
                new Rule("^Preparing level \"(.+)\"$", "正在准备世界「$1」"),
                new Rule("^Preparing start region for dimension (.+)$", "正在准备出生区域：$1"),
                new Rule("^Time elapsed: (\\d+) ms$", "耗时：$1 毫秒"),
                new Rule("^Done \\(([0-9.]+)s\\)! For help, type \"help\"$",
                        "启动完成（用时 $1 秒）！输入 help 查看可用指令"),
                new Rule("^Player (\\S+) joined the game$", "玩家 $1 加入了游戏"),
                new Rule("^Player (\\S+) left the game$", "玩家 $1 离开了游戏"),
                new Rule("^(\\S+) joined the game$", "玩家 $1 加入了游戏"),
                new Rule("^(\\S+) left the game$", "玩家 $1 离开了游戏"),
                new Rule("^(\\S+) lost connection: (.+)$", "$1 断开连接：$2"),
                new Rule("^Stopping server$", "正在关闭服务端"),
                new Rule("^Stopping the server$", "正在关闭服务端"),
                new Rule("^Saving worlds$", "正在保存世界"),
                new Rule("^Saving chunks for level '(.+)'/minecraft:(.+)$", "正在保存区块：$1/$2"),
                new Rule("^All chunks are saved$", "所有区块已保存"),
                new Rule("^ThreadedAnvilChunkStorage.*$", "区块存储：$0"),
                new Rule("^Server thread dump.*$", "服务端线程转储"),
                new Rule("^You need to agree to the EULA.*$",
                        "需要先同意 EULA（编辑 eula.txt 把 eula 改为 true）"),
                new Rule("^Failed to load properties.*$", "读取配置失败"),
                new Rule("^Preparing spawn area: (\\d+)%$", "正在准备出生区域：$1%"),
                new Rule("^Running server$", "服务端运行中"),
                new Rule("^Unknown or incomplete command, see below for error$",
                        "未知或不完整的指令，详见下方错误"),
                new Rule("^<--\\[HERE\\]$", "<--[此处]"),
                new Rule("^That player does not exist$", "该玩家不存在"),
                new Rule("^No player was found$", "未找到该玩家"),
                new Rule("^You do not have permission to use this command$",
                        "你没有权限使用此指令"),
                new Rule("^Set the time to (\\d+)$", "时间已设为 $1"),
                new Rule("^Set the difficulty to (.+)$", "难度已设为 $1"),
                new Rule("^Set own game mode to (.+)$", "游戏模式已设为 $1"),
                new Rule("^Matching server not found$", "未找到匹配的服务端"),
                new Rule("^Reloading!$", "正在重载"),
                new Rule("^Reload complete.$", "重载完成"),
                new Rule("^Saving is (\\w+)$", "保存状态：$1"),
                new Rule("^Saved the game$", "游戏已保存"),
                new Rule("^Saving the game \\(this may take a moment\\)$", "正在保存游戏（可能需要一点时间）"),
                new Rule("^Server does not exist$", "服务端不存在"),
                new Rule("^Usage: (.+)$", "用法：$1"),
                new Rule("^Tip: (.+)$", "提示：$1"),
                new Rule("^Failed to start the minecraft server$", "启动 Minecraft 服务端失败"),
                new Rule("^Exception in server tick loop$", "服务端 tick 循环出现异常"),
                new Rule("^This server is not running the EULA$", "此服务端尚未同意 EULA"),
        });

        // ---------- 英文规则（保持原文，仅规范化） ----------
        if (!zh) {
            rules.put("en_US", new Rule[0]);
        }
    }

    /** 翻译一行控制台输出。 */
    public String translate(String line) {
        if (line == null || line.isEmpty()) return line;

        // 保留 [时间] [线程/级别] 前缀，只翻译消息部分
        String prefix = "";
        String body = line;
        Matcher m = Pattern.compile("^(\\[[^\\]]*\\]\\s*(?:\\[[^\\]]*\\]\\s*)*)(.*)$")
                .matcher(line);
        if (m.matches()) {
            prefix = m.group(1);
            body = m.group(2);
        }

        String lang = I18n.getLanguage();
        Rule[] list = rules.get(lang);
        if (list == null) return line;

        for (Rule r : list) {
            Matcher rm = r.pattern.matcher(body);
            if (rm.matches()) {
                String out = r.template;
                for (int i = rm.groupCount(); i >= 1; i--) {
                    out = out.replace("$" + i, rm.group(i) == null ? "" : rm.group(i));
                }
                return prefix + out;
            }
        }
        return line;
    }
}