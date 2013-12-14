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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.jive.utils.http.client.HttpClient;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfig;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfiguration;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactory;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.jive.utils.http.client.rest.RequestHelper;
import com.jivesoftware.os.upena.config.shared.UpenaConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.io.FileUtils;
import org.merlin.config.BindInterfaceToConfiguration;
import org.merlin.config.Config;
import org.merlin.config.MapBackConfiguration;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

public class ConfigExtractor {

    public static void main(String[] args) {

        try {
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .setUrls(ClasspathHelper.forPackage("com.jivesoftware")) // GRRR
                    .setScanners(new SubTypesScanner(), new TypesScanner()));
            Set<Class<? extends Config>> subTypesOf = reflections.getSubTypesOf(Config.class);
            File configDir = new File("./config");
            configDir.mkdirs();
            ConfigExtractor configExtractor = new ConfigExtractor(new PropertyPrefix(), subTypesOf);
            File defaultConfigFile = new File(configDir, "default-config.properties");
            configExtractor.writeDefaultsToFile(defaultConfigFile);

            RequestHelper buildRequestHelper = buildRequestHelper(args[0], Integer.parseInt(args[1]));

            Properties defaultProperties = createKeySortedProperties();
            defaultProperties.load(new FileInputStream(defaultConfigFile));
            Map<String, String> config = new HashMap<>();
            for (Map.Entry<Object, Object> entry : defaultProperties.entrySet()) {
                config.put(entry.getKey().toString(), entry.getValue().toString());
            }

            UpenaConfig setDefaults = new UpenaConfig("default", args[2], config);
            UpenaConfig setConfig = buildRequestHelper.executeRequest(setDefaults, "/upenaConfig/set", UpenaConfig.class, null);
            if (setConfig == null) {
                System.out.println("Failed to publish default config for " + Arrays.deepToString(args));
            }

            UpenaConfig getOverrides = new UpenaConfig("override", args[2], config);
            UpenaConfig gotConfig = buildRequestHelper.executeRequest(getOverrides, "/upenaConfig/get", UpenaConfig.class, null);
            if (gotConfig == null) {
                System.out.println("Failed to publish default config for " + Arrays.deepToString(args));
            } else {
                Properties override = createKeySortedProperties();
                override.putAll(gotConfig.properties);
                override.store(new FileOutputStream("config/override-config.properties"), "");
            }

            Properties instanceProperties = createKeySortedProperties();
            File configFile = new File("config/instance.properties");
            if (configFile.exists()) {
                instanceProperties.load(new FileInputStream(configFile));
            }

            Properties overrideProperties = createKeySortedProperties();
            configFile = new File("config/override-config.properties");
            if (configFile.exists()) {
                overrideProperties.load(new FileInputStream(configFile));
            }

            Properties properties = createKeySortedProperties();
            properties.putAll(defaultProperties);
            properties.putAll(overrideProperties);
            properties.putAll(instanceProperties);
            properties.store(new FileOutputStream("config/config.properties"), "");

            System.exit(0);
        } catch (Exception x) {
            x.printStackTrace();
            System.exit(1);
        }

    }

    static Properties createKeySortedProperties() {
        return new Properties() {
            @Override
            public synchronized Enumeration<Object> keys() {
                return Collections.enumeration(new TreeSet<>(super.keySet()));
            }
        };
    }

    static RequestHelper buildRequestHelper(String host, int port) {
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider().createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(host, port);
        RequestHelper requestHelper = new RequestHelper(httpClient, new ObjectMapper());
        return requestHelper;
    }

    private final Collection<Class<? extends Config>> configClasses;
    private final PropertyPrefix propertyPrefix;

    public ConfigExtractor(PropertyPrefix propertyPrefix, Collection<Class<? extends Config>> configClasses) {
        this.propertyPrefix = propertyPrefix;
        this.configClasses = configClasses;

    }

    public void writeDefaultsToFile(File outputFile) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Class c : configClasses) {
            if (!c.isInterface()) {
                System.out.println("WARNING: class " + c + " somehow made it into the list of config classes. It is being skipped.");
                continue;
            } else {
                System.out.println("Building defaults for class:" + c.getName());
            }
            String classPrefix = propertyPrefix.propertyPrefix(c);
            Map<String, String> expected = new HashMap<>();
            Config config = new BindInterfaceToConfiguration<>(new MapBackConfiguration(expected), c).bind();
            config.applyDefaults();
            for (Map.Entry<String, String> entry : expected.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                String property = classPrefix + key + "=" + value;
                lines.add(property);
                System.out.println("\t"+property);
            }
        }
        FileUtils.writeLines(outputFile, "utf-8", lines, "\n", false);
    }
}
