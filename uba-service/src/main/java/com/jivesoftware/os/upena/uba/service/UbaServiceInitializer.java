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
package com.jivesoftware.os.upena.uba.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jivesoftware.os.routing.bird.http.client.HttpClient;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfig;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfiguration;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import java.io.File;
import java.util.Arrays;

public class UbaServiceInitializer {

    public UbaService initialize(String hostKey,
        String workingDir,
        String datacenter,
        String rack,
        String publicHostName,
        String host,
        int port,
        UbaLog ubaLog) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        UpenaClient client = new UpenaClient(buildRequestHelper(host, port, mapper));
        File root = new File(new File(workingDir), "services/");
        if (!root.exists() && !root.mkdirs()) {
            throw new RuntimeException("Failed trying to mkdirs for " + root);
        }
        UbaTree tree = new UbaTree(root, new String[]{"cluster", "service", "release", "instance"});
        Uba uba = new Uba(datacenter, rack, publicHostName, host, host, port, tree, ubaLog);
        UbaService conductorService = new UbaService(client, uba, hostKey);
        return conductorService;
    }

    HttpRequestHelper buildRequestHelper(String host, int port, ObjectMapper mapper) {
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider()
            .createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(host, port);
        HttpRequestHelper requestHelper = new HttpRequestHelper(httpClient, mapper);
        return requestHelper;
    }
}
