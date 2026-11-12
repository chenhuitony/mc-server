package org.bukkit.configuration.file;

/** YAML 配置选项（Bukkit 的 YamlConfigurationOptions）。 */
public class YamlConfigurationOptions extends FileConfigurationOptions {

    private int width = 80;

    public int width() { return width; }

    public YamlConfigurationOptions width(int value) {
        this.width = value;
        return this;
    }

    @Override
    public YamlConfigurationOptions header(String value) {
        this.header = value;
        return this;
    }

    @Override
    public YamlConfigurationOptions copyDefaults(boolean value) {
        this.copyDefaults = value;
        return this;
    }

    @Override
    public YamlConfigurationOptions parseComments(boolean value) {
        this.parseComments = value;
        return this;
    }

    @Override
    public YamlConfigurationOptions indent(int value) {
        this.indent = value;
        return this;
    }
}