/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.upena.uba.config.extractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

    public ConfigBinder(String[] args) throws IOException {
        this.propertyPrefix = new PropertyPrefix();
        this.properties = new Properties();
        for (String arg : args) {
            File f = new File(arg);
            if (f.exists()) {
                this.properties.load(new FileInputStream(f));
            }
        }
    }

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