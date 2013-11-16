package com.jivesoftware.jive.symphony.uba.config.extractor;

import org.merlin.config.Config;

public class PropertyPrefix {

    public String propertyPrefix(Class c) {
        String name = c.getSimpleName();
        String instance = "default";
        Class[] implemented = c.getInterfaces();
        if (implemented != null && implemented.length == 1 && implemented[0] != Config.class) {
            name = implemented[0].getSimpleName();
            instance = c.getSimpleName();
        }

        return name + "/" + instance + "/";
    }
}
