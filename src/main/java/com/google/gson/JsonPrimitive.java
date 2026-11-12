package com.google.gson;

/** 基本类型 JSON 值（兼容层）。 */
public class JsonPrimitive extends JsonElement {

    private final Object value;

    public JsonPrimitive(String s) { super(s); this.value = s; }

    public JsonPrimitive(Number n) { super(n); this.value = n; }

    public JsonPrimitive(Boolean b) { super(b); this.value = b; }

    public Number getAsNumber() {
        if (value instanceof Number) return (Number) value;
        try { return Double.parseDouble(String.valueOf(value)); } catch (Exception e) { return 0; }
    }

    @Override
    public String getAsString() {
        // 字符串原样返回；数字/布尔转字符串
        return value == null ? "null" : String.valueOf(value);
    }
}