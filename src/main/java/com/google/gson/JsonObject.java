package com.google.gson;

import java.util.LinkedHashMap;
import java.util.Map;

/** 极简 JsonObject（兼容层）。 */
public class JsonObject extends JsonElement {

    private final Map<String, JsonElement> members = new LinkedHashMap<>();

    public void add(String property, JsonElement value) {
        members.put(property, value == null ? new JsonNull() : value);
    }

    public JsonElement remove(String property) { return members.remove(property); }

    public JsonElement get(String memberName) { return members.get(memberName); }

    public boolean has(String memberName) { return members.containsKey(memberName); }

    public java.util.Set<Map.Entry<String, JsonElement>> entrySet() { return members.entrySet(); }

    public java.util.Set<String> keySet() { return members.keySet(); }

    public int size() { return members.size(); }
}