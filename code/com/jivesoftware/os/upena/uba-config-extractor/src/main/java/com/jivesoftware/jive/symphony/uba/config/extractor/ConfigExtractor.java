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
package com.jivesoftware.jive.symphony.uba.config.extractor;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.merlin.config.BindInterfaceToConfiguration;
import org.merlin.config.Config;
import org.merlin.config.MapBackConfiguration;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.scanners.TypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

public class ConfigExtractor {

    public static void main(String[] args) {

        try {
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .setUrls(ClasspathHelper.forPackage("com.jivesoftware"))
                    .setScanners(new SubTypesScanner(), new TypesScanner(), new MethodAnnotationsScanner(), new TypeAnnotationsScanner()));
            Set<Class<? extends Config>> subTypesOf = reflections.getSubTypesOf(Config.class);

            ConfigExtractor configExtractor = new ConfigExtractor(new PropertyPrefix(), subTypesOf);
            configExtractor.writeDefaultsToFile(new File("config.properties"));

            RequestHelper buildRequestHelper = buildRequestHelper(args[0], Integer.parseInt(args[1]));

            Properties prop = new Properties();
            prop.load(new FileInputStream("config.properties"));
            Map<String, String> config = new HashMap<>();
            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                config.put(entry.getKey().toString(), entry.getValue().toString());
            }

            UpenaConfig upenaConfig = new UpenaConfig("default", args[2], config);
            UpenaConfig gotConfig = buildRequestHelper.executeRequest(upenaConfig, "/upenaConfig/set", UpenaConfig.class, null);
            if (gotConfig == null) {
                throw new RuntimeException("Failed to publish default config for " + Arrays.deepToString(args));
            }

            System.exit(0);
        } catch (Exception x) {
            x.printStackTrace();
            System.exit(1);
        }

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
            String classPrefix = propertyPrefix.propertyPrefix(c);
            Map<String, String> expected = new HashMap<>();
            Config config = new BindInterfaceToConfiguration<>(new MapBackConfiguration(expected), c).bind();
            config.applyDefaults();
            for (Map.Entry<String, String> entry : expected.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                String property = classPrefix + key + "=" + value;
                lines.add(property);
                System.out.println(property);
            }
        }
        FileUtils.writeLines(outputFile, "utf-8", lines, "\n", false);
    }
}