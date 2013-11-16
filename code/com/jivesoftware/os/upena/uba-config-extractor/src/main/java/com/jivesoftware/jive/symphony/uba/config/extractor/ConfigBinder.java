package com.jivesoftware.jive.symphony.uba.config.extractor;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.merlin.config.BindInterfaceToConfiguration;
import org.merlin.config.Config;
import org.merlin.config.MapBackConfiguration;

public class ConfigBinder {

    private final Properties properties;
    private final PropertyPrefix propertyPrefix;
    private final Map<Class, Config> bound = new ConcurrentHashMap<>();

    public ConfigBinder(Properties properties) {
        this.propertyPrefix = new PropertyPrefix();
        this.properties = properties;
    }

    public <T extends Config> T bind(Class<T> configInterface) {
        Config config = bound.get(configInterface);
        if (config != null) {
            return (T) config;
        }

        Map<String, String> required = new HashMap<>();
        config = new BindInterfaceToConfiguration<>(new MapBackConfiguration(required), configInterface)
                .bind();
        config.applyDefaults();

        final String prefix = propertyPrefix.propertyPrefix(configInterface);
        Map<String, String> available = new HashMap<>();

        for (Entry<Object, Object> e : properties.entrySet()) {
            if (e.getKey().toString().startsWith(prefix)) {
                available.put(e.getKey().toString().substring(prefix.length()), e.getValue().toString());
            }
        }
        Set<String> requiredKeys = required.keySet();
        requiredKeys.removeAll(available.keySet());
        if (!requiredKeys.isEmpty()) {
            throw new IllegalStateException("The provided properties lacks the following properties: " + requiredKeys);
        }

        T t = new BindInterfaceToConfiguration<>(new MapBackConfiguration(available), configInterface)
                .bind();
        bound.put(configInterface, t);
        return t;
    }
}