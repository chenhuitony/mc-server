package com.mc.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 轻量 JSON 解析器（零依赖），用于 fabric.mod.json 等元数据。 */
public final class Json {

    private final String s;
    private int i;

    private Json(String s) { this.s = s; this.i = 0; }

    public static Object parse(String text) {
        Json p = new Json(text);
        p.ws();
        Object v = p.value();
        p.ws();
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object o = parse(text);
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>();
    }

    private Object value() {
        char c = s.charAt(i);
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': i += 4; return Boolean.TRUE;
            case 'f': i += 5; return Boolean.FALSE;
            case 'n': i += 4; return null;
            default:  return number();
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++;
        ws();
        if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
        while (i < s.length()) {
            ws();
            if (i >= s.length() || s.charAt(i) != '"') break;
            String key = string();
            ws();
            if (i < s.length() && s.charAt(i) == ':') i++;
            ws();
            m.put(key, value());
            ws();
            if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
            if (i < s.length() && s.charAt(i) == '}') { i++; break; }
            break;
        }
        return m;
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        i++;
        ws();
        if (i < s.length() && s.charAt(i) == ']') { i++; return list; }
        while (i < s.length()) {
            ws();
            list.add(value());
            ws();
            if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
            if (i < s.length() && s.charAt(i) == ']') { i++; break; }
            break;
        }
        return list;
    }

    private String string() {
        StringBuilder sb = new StringBuilder();
        i++;
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '"') break;
            if (c == '\\' && i < s.length()) {
                char e = s.charAt(i++);
                switch (e) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        if (i + 4 <= s.length()) {
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        break;
                    default: sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Object number() {
        int start = i;
        while (i < s.length() && "-+.eE0123456789".indexOf(s.charAt(i)) >= 0) i++;
        String n = s.substring(start, i);
        if (n.isEmpty()) return null;
        try {
            if (n.contains(".") || n.contains("e") || n.contains("E")) return Double.parseDouble(n);
            return Long.parseLong(n);
        } catch (NumberFormatException e) {
            return n;
        }
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }
}