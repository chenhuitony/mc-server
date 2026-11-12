package org.bukkit.permissions;

/**
 * 权限节点（Bukkit 的 Permission）。
 */
public class Permission {

    public static final String DEFAULT_PERMISSION = "op";

    private final String name;
    private String description = "";
    private String defaultValue = DEFAULT_PERMISSION;
    private final java.util.Map<String, Boolean> children = new java.util.LinkedHashMap<>();

    public Permission(String name) {
        this.name = name == null ? "" : name.toLowerCase();
    }

    public Permission(String name, String description, String defaultValue) {
        this(name);
        this.description = description == null ? "" : description;
        this.defaultValue = defaultValue == null ? DEFAULT_PERMISSION : defaultValue;
    }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public void setDescription(String d) { this.description = d; }

    public String getDefault() { return defaultValue; }

    public void setDefault(String d) { this.defaultValue = d; }

    /** 用枚举设置默认值。 */
    public void setDefault(org.bukkit.permissions.PermissionDefault d) {
        this.defaultValue = d == null ? DEFAULT_PERMISSION : d.getValue();
    }

    public java.util.Map<String, Boolean> getChildren() { return children; }

    public void addParent(Permission p, boolean value) {
        if (p != null) children.put(p.getName(), value);
    }
}