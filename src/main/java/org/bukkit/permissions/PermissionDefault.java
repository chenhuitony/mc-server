package org.bukkit.permissions;

/** 权限默认值（Bukkit 的 PermissionDefault）。 */
public enum PermissionDefault {

    TRUE("true"),
    FALSE("false"),
    OP("op"),
    NOT_OP("!op");

    private final String value;

    PermissionDefault(String value) { this.value = value; }

    public String getValue() { return value; }

    public boolean getValue(boolean op) {
        switch (this) {
            case TRUE:    return true;
            case FALSE:   return false;
            case OP:      return op;
            case NOT_OP:  return !op;
            default:      return false;
        }
    }

    public static PermissionDefault getByName(String name) {
        if (name == null) return OP;
        for (PermissionDefault v : values()) {
            if (v.value.equalsIgnoreCase(name) || v.name().equalsIgnoreCase(name)) return v;
        }
        return OP;
    }

    public static PermissionDefault getByName(String name, PermissionDefault def) {
        if (name == null) return def;
        for (PermissionDefault v : values()) {
            if (v.value.equalsIgnoreCase(name) || v.name().equalsIgnoreCase(name)) return v;
        }
        return def;
    }
}