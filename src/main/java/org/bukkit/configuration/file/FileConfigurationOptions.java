package org.bukkit.configuration.file;

/**
 * 配置文件选项（Bukkit 的 FileConfigurationOptions）。
 *
 * <p>插件常用它设置 copyDefaults、header 等。
 */
public class FileConfigurationOptions {

    protected boolean copyDefaults;
    protected String header;
    protected boolean parseComments = true;
    protected int indent = 2;

    public FileConfigurationOptions() { }

    public boolean copyDefaults() { return copyDefaults; }

    public FileConfigurationOptions copyDefaults(boolean value) {
        this.copyDefaults = value;
        return this;
    }

    public String header() { return header; }

    public FileConfigurationOptions header(String value) {
        this.header = value;
        return this;
    }

    public boolean parseComments() { return parseComments; }

    public FileConfigurationOptions parseComments(boolean value) {
        this.parseComments = value;
        return this;
    }

    public int indent() { return indent; }

    public FileConfigurationOptions indent(int value) {
        this.indent = value;
        return this;
    }
}