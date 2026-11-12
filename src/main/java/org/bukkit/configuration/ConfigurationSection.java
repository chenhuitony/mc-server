package org.bukkit.configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 配置节点（Bukkit 的 ConfigurationSection）。
 *
 * <p>以 YAML 风格的层级结构存储键值，支持 "a.b.c" 路径访问。
 */
public interface ConfigurationSection {

    Set<String> getKeys(boolean deep);

    Map<String, Object> getValues(boolean deep);

    boolean contains(String path);

    boolean isSet(String path);

    String getCurrentPath();

    String getName();

    ConfigurationSection getParent();

    Object get(String path);

    Object get(String path, Object def);

    void set(String path, Object value);

    ConfigurationSection createSection(String path);

    ConfigurationSection createSection(String path, Map<?, ?> map);

    String getString(String path);

    String getString(String path, String def);

    boolean isString(String path);

    int getInt(String path);

    int getInt(String path, int def);

    boolean isInt(String path);

    boolean getBoolean(String path);

    boolean getBoolean(String path, boolean def);

    boolean isBoolean(String path);

    double getDouble(String path);

    double getDouble(String path, double def);

    boolean isDouble(String path);

    long getLong(String path);

    long getLong(String path, long def);

    boolean isLong(String path);

    List<?> getList(String path);

    List<?> getList(String path, List<?> def);

    boolean isList(String path);

    List<String> getStringList(String path);

    ConfigurationSection getConfigurationSection(String path);

    boolean isConfigurationSection(String path);

    void addDefault(String path, Object value);
}