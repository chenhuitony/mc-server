package com.google.gson;

/** JSON null（兼容层）。 */
public class JsonNull extends JsonElement {

    public static final JsonNull INSTANCE = new JsonNull();

    @Override
    public boolean isJsonNull() { return true; }

    @Override
    public String getAsString() { return "null"; }
}