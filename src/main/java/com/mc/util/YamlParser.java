package com.mc.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量 YAML 解析器，把 YAML 文本解析成 Map / List / String / Number / Boolean。
 *
 * <p>为什么自己写：本项目要“零第三方依赖”，而插件元数据（plugin.yml /
 * paper-plugin.yml）和配置（config.yml）都必须是 YAML。完整 YAML 规范过于庞大，
 * 这里实现插件生态实际会用到的子集：
 *
 * <ul>
 *   <li>缩进嵌套的映射与列表</li>
 *   <li>{@code key: value} 与 {@code key:} 换行嵌套</li>
 *   <li>{@code - item} 列表，支持列表项本身是映射</li>
 *   <li>行内列表 {@code [a, b, c]}</li>
 *   <li>单引号 / 双引号字符串</li>
 *   <li>块标量 {@code |} 与 {@code >}</li>
 *   <li>{@code #} 注释（引号内的不算）</li>
 *   <li>布尔、整数、浮点、null 自动识别</li>
 * </ul>
 */
public final class YamlParser {

    private YamlParser() {}

    /** 解析结果根节点。YAML 顶层通常映射；纯列表或标量也支持。 */
    public static Object parse(List<String> lines) {
        List<Line> cleaned = new ArrayList<>();
        for (String raw : lines) {
            if (raw == null) continue;
            String expanded = raw.replace("\t", "    ");
            int indent = leadingSpaces(expanded);
            String content = expanded.substring(indent);
            if (content.isEmpty()) { cleaned.add(new Line(indent, "", true)); continue; }
            if (content.startsWith("#")) { cleaned.add(new Line(indent, "", true)); continue; }
            cleaned.add(new Line(indent, stripComment(content), false));
        }
        int[] cursor = {0};
        return parseBlock(cleaned, cursor, 0);
    }

    // ---------- 解析核心 ----------

    private static Object parseBlock(List<Line> lines, int[] cursor, int minIndent) {
        skipBlank(lines, cursor);
        if (cursor[0] >= lines.size()) return new LinkedHashMap<String, Object>();

        Line first = lines.get(cursor[0]);
        if (first.indent < minIndent) return new LinkedHashMap<String, Object>();

        if (first.content.startsWith("- ") || first.content.equals("-")) {
            return parseList(lines, cursor, first.indent);
        }
        return parseMap(lines, cursor, first.indent);
    }

    private static List<Object> parseList(List<Line> lines, int[] cursor, int indent) {
        List<Object> out = new ArrayList<>();
        while (cursor[0] < lines.size()) {
            skipBlank(lines, cursor);
            if (cursor[0] >= lines.size()) break;

            Line line = lines.get(cursor[0]);
            if (line.indent != indent) break;
            if (!line.content.startsWith("-")) break;

            String rest = line.content.length() > 1
                    ? line.content.substring(1).trim()
                    : "";

            if (rest.isEmpty()) {
                cursor[0]++;
                Object child = parseBlock(lines, cursor, indent + 1);
                out.add(child);
            } else if (isKeyValue(rest)) {
                // 列表项本身是映射：把它当作缩进为 indent+2 的映射起始
                Line synthetic = new Line(indent + 2, rest, false);
                lines.set(cursor[0], synthetic);
                Map<String, Object> item = parseMap(lines, cursor, indent + 2);
                out.add(item);
            } else {
                cursor[0]++;
                out.add(scalar(rest));
            }
        }
        return out;
    }

    private static Map<String, Object> parseMap(List<Line> lines, int[] cursor, int indent) {
        Map<String, Object> out = new LinkedHashMap<>();
        while (cursor[0] < lines.size()) {
            skipBlank(lines, cursor);
            if (cursor[0] >= lines.size()) break;

            Line line = lines.get(cursor[0]);
            if (line.indent < indent) break;
            if (line.indent > indent) break;

            int c = colon(line.content);
            if (c < 0) { cursor[0]++; continue; }

            String key = unquote(line.content.substring(0, c).trim());
            String val = line.content.substring(c + 1).trim();
            cursor[0]++;

            if (val.startsWith("|") || val.startsWith(">")) {
                out.put(key, blockScalar(lines, cursor, indent, val.startsWith(">")));
            } else if (val.isEmpty()) {
                skipBlank(lines, cursor);
                if (cursor[0] < lines.size() && lines.get(cursor[0]).indent > indent) {
                    out.put(key, parseBlock(lines, cursor, lines.get(cursor[0]).indent));
                } else {
                    out.put(key, null);
                }
            } else {
                out.put(key, scalar(val));
            }
        }
        return out;
    }

    /** 收集块标量（| 保留换行，> 折叠为空格）。 */
    private static String blockScalar(List<Line> lines, int[] cursor, int indent, boolean fold) {
        StringBuilder sb = new StringBuilder();
        while (cursor[0] < lines.size()) {
            Line line = lines.get(cursor[0]);
            if (line.indent <= indent) break;
            if (sb.length() > 0) sb.append(fold ? " " : "\n");
            sb.append(line.content);
            cursor[0]++;
        }
        return sb.toString();
    }

    // ---------- 标量处理 ----------

    private static Object scalar(String s) {
        String v = s.trim();
        if (v.startsWith("[") && v.endsWith("]")) {
            List<Object> list = new ArrayList<>();
            String inner = v.substring(1, v.length() - 1).trim();
            if (!inner.isEmpty()) {
                for (String part : splitTopLevel(inner)) {
                    String p = part.trim();
                    if (!p.isEmpty()) list.add(scalar(p));
                }
            }
            return list;
        }
        if (v.startsWith("{") && v.endsWith("}")) {
            Map<String, Object> m = new LinkedHashMap<>();
            String inner = v.substring(1, v.length() - 1).trim();
            if (!inner.isEmpty()) {
                for (String part : splitTopLevel(inner)) {
                    int c = colon(part);
                    if (c < 0) continue;
                    m.put(unquote(part.substring(0, c).trim()), scalar(part.substring(c + 1).trim()));
                }
            }
            return m;
        }
        if (v.length() >= 2) {
            char a = v.charAt(0), b = v.charAt(v.length() - 1);
            if ((a == '"' && b == '"') || (a == '\'' && b == '\'')) {
                return unquote(v);
            }
        }
        if (v.equalsIgnoreCase("null") || v.equals("~") || v.isEmpty()) return null;
        if (v.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (v.equalsIgnoreCase("false")) return Boolean.FALSE;
        try {
            if (v.matches("[-+]?\\d+")) return Long.parseLong(v);
            if (v.matches("[-+]?\\d*\\.\\d+([eE][-+]?\\d+)?")) return Double.parseDouble(v);
        } catch (NumberFormatException ignore) { }
        return v;
    }

    /** 按逗号拆分，但忽略引号和括号内部的逗号。 */
    private static List<String> splitTopLevel(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inS = false, inD = false;
        int depth = 0;
        for (char c : s.toCharArray()) {
            if (c == '\'' && !inD) { inS = !inS; cur.append(c); continue; }
            if (c == '"' && !inS) { inD = !inD; cur.append(c); continue; }
            if (!inS && !inD) {
                if (c == '[' || c == '{') depth++;
                else if (c == ']' || c == '}') depth--;
                else if (c == ',' && depth == 0) { out.add(cur.toString()); cur.setLength(0); continue; }
            }
            cur.append(c);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private static boolean isKeyValue(String s) {
        int c = colon(s);
        if (c <= 0) return false;
        // “- 1.0” 这类不是键值对
        String key = s.substring(0, c).trim();
        return !key.isEmpty() && !key.matches("[-+]?\\d+(\\.\\d+)?");
    }

    private static int colon(String s) {
        boolean inS = false, inD = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !inD) inS = !inS;
            else if (c == '"' && !inS) inD = !inD;
            else if (c == ':' && !inS && !inD) {
                if (i + 1 >= s.length() || Character.isWhitespace(s.charAt(i + 1))) return i;
            }
        }
        return -1;
    }

    private static String stripComment(String s) {
        boolean inS = false, inD = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !inD) inS = !inS;
            else if (c == '"' && !inS) inD = !inD;
            else if (c == '#' && !inS && !inD) {
                if (i == 0 || Character.isWhitespace(s.charAt(i - 1))) {
                    return s.substring(0, i).trim();
                }
            }
        }
        return s.trim();
    }

    private static String unquote(String s) {
        String v = s.trim();
        if (v.length() >= 2) {
            char a = v.charAt(0), b = v.charAt(v.length() - 1);
            if ((a == '"' && b == '"') || (a == '\'' && b == '\'')) {
                String inner = v.substring(1, v.length() - 1);
                if (a == '"') {
                    return inner.replace("\\n", "\n").replace("\\t", "\t")
                            .replace("\\\"", "\"").replace("\\\\", "\\");
                }
                return inner.replace("''", "'");
            }
        }
        return v;
    }

    private static int leadingSpaces(String s) {
        int n = 0;
        while (n < s.length() && s.charAt(n) == ' ') n++;
        return n;
    }

    private static void skipBlank(List<Line> lines, int[] cursor) {
        while (cursor[0] < lines.size() && lines.get(cursor[0]).blank) cursor[0]++;
    }

    // ---------- 内部结构 ----------

    private static final class Line {
        final int indent;
        String content;
        final boolean blank;
        Line(int indent, String content, boolean blank) {
            this.indent = indent;
            this.content = content;
            this.blank = blank;
        }
    }

    // ---------- 对外便捷方法 ----------

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object o) {
        return o instanceof List ? (List<Object>) o : new ArrayList<>();
    }

    public static String str(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o == null ? null : String.valueOf(o);
    }

    public static List<String> strList(Map<String, Object> m, String key) {
        List<String> out = new ArrayList<>();
        Object o = m.get(key);
        if (o == null) return out;
        if (o instanceof List) {
            for (Object x : asList(o)) if (x != null) out.add(String.valueOf(x));
        } else {
            String s = String.valueOf(o).trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    public static boolean bool(Map<String, Object> m, String key, boolean def) {
        Object o = m.get(key);
        if (o == null) return def;
        if (o instanceof Boolean) return (Boolean) o;
        return Boolean.parseBoolean(String.valueOf(o));
    }
}