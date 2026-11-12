package com.google.gson;

import java.util.ArrayList;
import java.util.List;

/** 极简 JsonArray（兼容层）。 */
public class JsonArray extends JsonElement {

    private final List<JsonElement> elements = new ArrayList<>();

    public void add(JsonElement element) {
        elements.add(element == null ? new JsonNull() : element);
    }

    public JsonElement get(int i) { return elements.get(i); }

    public int size() { return elements.size(); }

    public boolean isEmpty() { return elements.isEmpty(); }

    public java.util.Iterator<JsonElement> iterator() { return elements.iterator(); }
}