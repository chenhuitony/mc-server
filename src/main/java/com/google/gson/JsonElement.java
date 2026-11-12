package com.google.gson;

/**
 * 极简 Gson 兼容层。
 *
 * <p>真实服务端会把 Gson 作为共享库提供（Bukkit 依赖它），
 * 很多插件（Vault、LuckPerms 等）直接 import com.google.gson。
 * 这里提供满足「能被链接、不抛 ClassNotFound」的最小实现。
 *
 * <p>注意：这不是完整 Gson，复杂序列化请把 gson 打进插件 JAR。
 */
public class JsonElement {

    private final Object value;

    public JsonElement() { this.value = null; }

    protected JsonElement(Object value) { this.value = value; }

    public boolean isJsonObject() { return value instanceof JsonObject; }

    public boolean isJsonArray() { return value instanceof JsonArray; }

    public boolean isJsonPrimitive() { return value instanceof JsonPrimitive; }

    public boolean isJsonNull() { return this instanceof JsonNull; }

    public JsonObject getAsJsonObject() {
        return value instanceof JsonObject ? (JsonObject) value : new JsonObject();
    }

    public JsonArray getAsJsonArray() {
        return value instanceof JsonArray ? (JsonArray) value : new JsonArray();
    }

    public JsonPrimitive getAsJsonPrimitive() {
        return value instanceof JsonPrimitive ? (JsonPrimitive) value : new JsonPrimitive((String) null);
    }

    public String getAsString() {
        return value == null ? "null" : String.valueOf(value);
    }

    public int getAsInt() {
        try { return Integer.parseInt(getAsString()); } catch (Exception e) { return 0; }
    }

    public long getAsLong() {
        try { return Long.parseLong(getAsString()); } catch (Exception e) { return 0; }
    }

    public double getAsDouble() {
        try { return Double.parseDouble(getAsString()); } catch (Exception e) { return 0; }
    }

    public boolean getAsBoolean() {
        return Boolean.parseBoolean(getAsString());
    }
}