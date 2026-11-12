package org.bukkit.configuration.file;

import java.io.File;

/**
 * YAML 配置（Bukkit 的 YamlConfiguration）。
 *
 * <p>Vault 等插件用它读写 yml 配置。
 */
public class YamlConfiguration extends FileConfiguration {

    private final YamlConfigurationOptions yamlOptions = new YamlConfigurationOptions();

    public YamlConfiguration(File file) {
        super(file);
    }

    @Override
    public YamlConfigurationOptions options() {
        return yamlOptions;
    }

    /** 从字符串构建配置。 */
    public static YamlConfiguration loadConfiguration(String contents) {
        YamlConfiguration cfg = new YamlConfiguration(null);
        if (contents != null && !contents.isEmpty()) {
            cfg.applyMapInternal(com.mc.util.YamlParser.asMap(
                    com.mc.util.YamlParser.parse(
                            java.util.Arrays.asList(contents.split("\r?\n")))));
        }
        return cfg;
    }

    /** 从文件加载配置。 */
    public static YamlConfiguration loadConfiguration(File file) {
        YamlConfiguration cfg = new YamlConfiguration(file);
        try {
            cfg.load();
        } catch (Exception ignore) { }
        return cfg;
    }
}