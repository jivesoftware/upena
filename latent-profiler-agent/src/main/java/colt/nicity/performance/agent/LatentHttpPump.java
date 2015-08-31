/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package colt.nicity.performance.agent;

import colt.nicity.performance.latent.Latency;
import colt.nicity.performance.latent.LatentDepth;
import colt.nicity.performance.latent.LatentDepthCallback;
import colt.nicity.performance.latent.http.ApacheHttpClient;
import colt.nicity.performance.latent.http.json.JSONObject;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpMethodParams;

/**
 *
 */
public class LatentHttpPump {

    private final Latency latency = Latency.singleton();
    private final String clusterName;
    private final String serviceName;
    private final String serviceVersion;
    private final ApacheHttpClient httpClient;

    public LatentHttpPump(String hostName, String clusterName, String serviceName, String serviceVersion) {
        this.clusterName = clusterName;
        this.serviceName = serviceName;
        this.serviceVersion = serviceVersion;
        httpClient = new ApacheHttpClient(createApacheClient(hostName, 1175, 10, 30000), new HashMap<String, String>());
    }

    private org.apache.commons.httpclient.HttpClient createApacheClient(String host, int port, int maxConnections, int socketTimeoutInMillis) {




        HttpConnectionManager connectionManager = createConnectionManager(maxConnections);

        org.apache.commons.httpclient.HttpClient client =
                new org.apache.commons.httpclient.HttpClient(connectionManager);
        client.getParams().setParameter(HttpMethodParams.COOKIE_POLICY, CookiePolicy.RFC_2109);
        client.getParams().setParameter(HttpMethodParams.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        client.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "UTF-8");
        client.getParams().setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, false);
        client.getParams().setBooleanParameter(HttpConnectionParams.STALE_CONNECTION_CHECK, true);
        client.getParams().setParameter(HttpConnectionParams.CONNECTION_TIMEOUT,
                socketTimeoutInMillis > 0 ? socketTimeoutInMillis : 0);
        client.getParams().setParameter(HttpConnectionParams.SO_TIMEOUT,
                socketTimeoutInMillis > 0 ? socketTimeoutInMillis : 0);


        HostConfiguration hostConfiguration = new HostConfiguration();
        configureSsl(hostConfiguration, host, port);
        configureProxy(hostConfiguration);

        client.setHostConfiguration(hostConfiguration);
        return client;

    }

    private void configureProxy(HostConfiguration hostConfiguration) {
//        HttpClientProxyConfig httpClientProxyConfig = locateConfig(HttpClientProxyConfig.class, null);
//        if (httpClientProxyConfig != null) {
//            hostConfiguration.setProxy(httpClientProxyConfig.getProxyHost(), httpClientProxyConfig.getProxyPort());
//            if (hasValidProxyUsernameAndPasswordSettings(httpClientProxyConfig)) {
//                HttpState state = new HttpState();
//                state.setProxyCredentials(AuthScope.ANY, new UsernamePasswordCredentials(httpClientProxyConfig.getProxyUsername(),
//                        httpClientProxyConfig.getProxyPassword()));
//                httpClient.setState(state);
//            }
//        }
    }

    private void configureSsl(HostConfiguration hostConfiguration, String host, int port) throws IllegalStateException {
//        HttpClientSSLConfig httpClientSSLConfig = locateConfig(HttpClientSSLConfig.class, null);
//        if (httpClientSSLConfig != null) {
//            Protocol sslProtocol;
//            if (httpClientSSLConfig.getCustomSSLSocketFactory() != null) {
//                sslProtocol = new Protocol(HTTPS_PROTOCOL,
//                        new CustomSecureProtocolSocketFactory(httpClientSSLConfig.getCustomSSLSocketFactory()), SSL_PORT);
//            } else {
//                sslProtocol = Protocol.getProtocol(HTTPS_PROTOCOL);
//            }
//            hostConfiguration.setHost(host, port, sslProtocol);
//            httpClient.setUsingSSL();
//        } else {
        hostConfiguration.setHost(host, port);
//        }
    }

    private HttpConnectionManager createConnectionManager(int maxConnections) {
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        if (maxConnections > 0) {
            connectionManager.getParams().setMaxTotalConnections(maxConnections);
        }
        return connectionManager;
    }

    public void start() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    final long now = System.currentTimeMillis();
                    latency.getLatentGraph().latentDepths(new LatentDepthCallback() {
                        @Override
                        public void calls(LatentDepth from, LatentDepth to) { // TODO batching
                            try {
                                JSONObject sample = new JSONObject();
                                sample.put("clusterName", clusterName);
                                sample.put("serviceName", serviceName);
                                sample.put("serviceVersion", serviceVersion);
                                sample.put("sampleTimestampEpochMillis", now);
                                sample.put("from", latentJson(from));
                                sample.put("to", latentJson(to));
                                httpClient.postJson("/profile/latents", sample.toJSONString());
                            } catch (Exception x) {
                                //if (verbose) {
                                //System.out.println("Latent Service is inaccessible. "+x.getMessage());
                                //}
                            }
                        }
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                }

            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public void stop() {
    }

    private JSONObject latentJson(LatentDepth latentDepth) {
        if (latentDepth == null) {
            return null;
        }
        JSONObject latent = new JSONObject();
        latent.put("stackDepth", latentDepth.getDepth());
        latent.put("interfaceName", latentDepth.getLatent().getInterfaceName());
        latent.put("className", latentDepth.getLatent().getClassName());
        latent.put("methodName", latentDepth.getLatent().getMethodName());
        latent.put("callCount", latentDepth.getLatent().getCalled());
        latent.put("callLatency", latentDepth.getLatent().getSuccesslatency());
        latent.put("failedCount", latentDepth.getLatent().getFailed());
        latent.put("failedLatency", latentDepth.getLatent().getFailedlatency());
        return latent;
    }
}
