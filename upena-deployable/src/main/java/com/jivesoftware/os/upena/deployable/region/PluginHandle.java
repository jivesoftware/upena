package com.jivesoftware.os.upena.deployable.region;

/**
 *
 */
public class PluginHandle {

    public final String glyphicon;
    public final String icon;
    public final String name;
    public final String path;
    public final Class<?> endpointsClass;
    public final Region<?> region;
    public final String separator;
    public final String[] permissions;

    public PluginHandle(String glyphicon, String icon, String name, String path, Class<?> endpointsClass, Region<?> region, String separator, String... permissions) {
        this.glyphicon = glyphicon;
        this.icon = icon;
        this.name = name;
        this.path = path;
        this.endpointsClass = endpointsClass;
        this.region = region;
        this.separator = separator;
        this.permissions = permissions;
    }

}
