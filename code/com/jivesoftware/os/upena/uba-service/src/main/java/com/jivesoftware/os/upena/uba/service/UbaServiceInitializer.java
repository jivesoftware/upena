package com.jivesoftware.os.upena.uba.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jivesoftware.os.jive.utils.http.client.HttpClient;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfig;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfiguration;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactory;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.jive.utils.http.client.rest.RequestHelper;
import java.io.File;
import java.util.Arrays;

public class UbaServiceInitializer {

    public UbaService initialize(String hostKey, String workingDir, String composerHost, int composerPort) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        UpenaClient conductorClient = new UpenaClient(buildRequestHelper(composerHost, composerPort, mapper));
        File root = new File(new File(workingDir), "services/");
        root.mkdirs();
        UbaTree tree = new UbaTree(root, new String[]{"cluster", "service", "release", "instance"});
        Uba conductor = new Uba(composerHost, composerHost, composerPort, mapper, tree);
        UbaService conductorService = new UbaService(conductorClient, conductor, hostKey);
        return conductorService;
    }

    RequestHelper buildRequestHelper(String host, int port, ObjectMapper mapper) {
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider()
                .createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(host, port);
        RequestHelper requestHelper = new RequestHelper(httpClient, mapper);
        return requestHelper;
    }
}