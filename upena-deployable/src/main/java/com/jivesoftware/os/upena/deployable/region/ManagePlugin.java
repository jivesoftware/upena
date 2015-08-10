package com.jivesoftware.os.upena.deployable.region;

/**
 *
 */
public class ManagePlugin {

    public final String glyphicon;
    public final String icon;
    public final String name;
    public final String path;
    public final Class<?> endpointsClass;
    public final Region<?> region;

    public ManagePlugin(String glyphicon, String icon, String name, String path, Class<?> endpointsClass, Region<?> region) {
        this.glyphicon = glyphicon;
        this.icon = icon;
        this.name = name;
        this.path = path;
        this.endpointsClass = endpointsClass;
        this.region = region;
    }
}
