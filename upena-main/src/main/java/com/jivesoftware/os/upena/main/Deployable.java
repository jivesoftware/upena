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
package com.jivesoftware.os.upena.main;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.jivesoftware.os.jive.utils.base.service.ServiceHandle;
import com.jivesoftware.os.jive.utils.health.HealthCheck;
import com.jivesoftware.os.jive.utils.health.HealthCheckResponse;
import com.jivesoftware.os.jive.utils.health.HealthCheckResponseImpl;
import com.jivesoftware.os.mlogger.core.LoggerSummary;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.server.http.jetty.jersey.endpoints.configuration.MainProperties;
import com.jivesoftware.os.server.http.jetty.jersey.endpoints.configuration.MainPropertiesEndpoints;
import com.jivesoftware.os.server.http.jetty.jersey.server.InitializeRestfulServer;
import com.jivesoftware.os.server.http.jetty.jersey.server.JerseyEndpoints;
import com.jivesoftware.os.server.http.jetty.jersey.server.RestfulManageServer;
import com.jivesoftware.os.server.http.jetty.jersey.server.RestfulServer;
import com.jivesoftware.os.server.http.jetty.jersey.server.util.Resource;
import com.jivesoftware.os.upena.reporter.service.StatusReportBroadcaster;
import com.jivesoftware.os.upena.reporter.service.StatusReportBroadcaster.StatusReportCallback;
import com.jivesoftware.os.upena.reporter.service.StatusReportConfig;
import com.jivesoftware.os.upena.routing.shared.ConnectionDescriptorsProvider;
import com.jivesoftware.os.upena.routing.shared.TenantRoutingProvider;
import com.jivesoftware.os.upena.tenant.routing.http.server.endpoints.TenantRoutingRestEndpoints;
import com.jivesoftware.os.upena.uba.config.extractor.ConfigBinder;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.merlin.config.Config;

public class Deployable {

    private final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final ConfigBinder configBinder;
    private final TenantRoutingProvider tenantRoutingProvider;
    private final RestfulManageServer restfulManageServer;
    private final AtomicBoolean manageServerStarted = new AtomicBoolean(false);
    //--------------------------------------------------------------------------
    private final InitializeRestfulServer restfulServer;
    private final JerseyEndpoints jerseyEndpoints;
    private final AtomicBoolean serverStarted = new AtomicBoolean(false);
    private final InstanceConfig instanceConfig;

    public Deployable(String[] args) throws IOException {
        this(args, null);
    }

    public Deployable(String[] args, ConnectionDescriptorsProvider connectionsDescriptorProvider) throws IOException {

        configBinder = new ConfigBinder(args);
        instanceConfig = configBinder.bind(InstanceConfig.class);
        if (connectionsDescriptorProvider == null) {
            TenantRoutingBirdProviderBuilder tenantRoutingBirdBuilder = new TenantRoutingBirdProviderBuilder(instanceConfig.getRoutesHost(),
                instanceConfig.getRoutesPort());
            connectionsDescriptorProvider = tenantRoutingBirdBuilder.build();
        }

        tenantRoutingProvider = new TenantRoutingProvider(instanceConfig.getInstanceKey(), connectionsDescriptorProvider);

        String applicationName = "manage " + instanceConfig.getServiceName() + " " + instanceConfig.getClusterName();
        restfulManageServer = new RestfulManageServer(instanceConfig.getManagePort(),
            applicationName,
            instanceConfig.getManageMaxThreads(),
            instanceConfig.getManageMaxQueuedRequests());

        restfulManageServer.addEndpoint(TenantRoutingRestEndpoints.class);
        restfulManageServer.addInjectable(TenantRoutingProvider.class, tenantRoutingProvider);
        restfulManageServer.addEndpoint(MainPropertiesEndpoints.class);
        restfulManageServer.addInjectable(MainProperties.class, new MainProperties(args));

        jerseyEndpoints = new JerseyEndpoints();
        restfulServer = new InitializeRestfulServer(instanceConfig.getMainPort(),
            applicationName,
            instanceConfig.getMainMaxThreads(),
            instanceConfig.getMainMaxQueuedRequests());
    }

    /**
     Needs to be called before buildManageServer().
     @param statusReportCallback
     @return
     */
    public ServiceHandle buildStatusReporter(StatusReportCallback statusReportCallback) {

        StatusReportConfig config = configBinder.bind(StatusReportConfig.class);

        StatusReportBroadcaster broadcaster = new StatusReportBroadcaster(
            config.getAnnouceEveryNMills(),
            statusReportCallback);

        restfulManageServer.addEndpoint(StatusReportEndpoints.class);
        restfulManageServer.addInjectable(StatusReportBroadcaster.class, broadcaster);
        return broadcaster;
    }

    public ServiceHandle buildMetricPublisher() {
        return null;
    }

    public ServiceHandle buildLogPublisher() {
        return null;
    }

    public TenantRoutingProvider getTenantRoutingProvider() {
        return tenantRoutingProvider;
    }

    public <T extends Config> T config(Class<T> clazz) {
        return configBinder.bind(clazz);
    }

    public void addManageEndpoints(Class clazz) {
        if (manageServerStarted.get()) {
            throw new IllegalStateException("Cannot add endpoints after the manage server has been started.");
        }
        restfulManageServer.addEndpoint(clazz);
    }

    public void addManageInjectables(Class clazz, Object injectable) {
        if (manageServerStarted.get()) {
            throw new IllegalStateException("Cannot add injectables after the manage server has been started.");
        }
        restfulManageServer.addInjectable(clazz, injectable);
    }

    public void addHealthCheck(HealthCheck... healthCheck) {
        restfulManageServer.addHealthCheck(healthCheck);
    }

    public ServiceHandle buildManageServer() throws Exception {
        if (manageServerStarted.compareAndSet(false, true)) {
            long time = System.currentTimeMillis();
            String applicationName = "manage " + instanceConfig.getServiceName() + " " + instanceConfig.getClusterName();
            ComponentHealthCheck healthCheck = new ComponentHealthCheck("'" + applicationName + "'service initialization");
            restfulManageServer.addHealthCheck(healthCheck);
            try {
                RestfulManageServer server = restfulManageServer.initialize();
                healthCheck.setHealthy("'" + applicationName + "' service is initialized.");
                banneredOneLiner("'" + applicationName + "' service started. Elapse:" + (System.currentTimeMillis() - time));
                server.addHealthCheck((HealthCheck) () -> {
                    String status = "Currently this service on port:"
                        + instanceConfig.getManagePort() + " has " + server.getIdleThreads() + " idle thread/s.";
                    String description = "How many free thread are available to handle http request.";
                    String resolution = "Increase the number or threads or add more services.";
                    return new HealthCheckResponseImpl("manage>http>threadPool",
                        server.isLowOnThreads() ? 0d : 1d,
                        status, description, resolution, System.currentTimeMillis());
                });
                return new ServiceHandle() {

                    @Override
                    public void start() throws Exception {
                        restfulManageServer.start();
                    }

                    @Override
                    public void stop() throws Exception {
                        restfulManageServer.stop();
                    }
                };
            } catch (Exception x) {
                healthCheck.setUnhealthy("Failed to initialize service '" + applicationName + "'.", x);
                throw x;
            }
        } else {
            throw new IllegalStateException("Cannot start manage server more than once.");
        }
    }

    //--------------------------------------------------------------------------
    public void addEndpoints(Class clazz) {
        if (serverStarted.get()) {
            throw new IllegalStateException("Cannot add endpoints after the server has been started.");
        }
        jerseyEndpoints.addEndpoint(clazz);
    }

    public void addInjectables(Class clazz, Object injectable) {
        if (serverStarted.get()) {
            throw new IllegalStateException("Cannot add injectables after the server has been started.");
        }
        jerseyEndpoints.addInjectable(clazz, injectable);
    }

    public void addResource(Resource resource) {
        restfulServer.addResource(resource);
    }

    private class LoggerSummaryHealthCheck implements HealthCheck {

        private final LoggerSummary loggerSummary;

        public LoggerSummaryHealthCheck(LoggerSummary loggerSummary) {
            this.loggerSummary = loggerSummary;
        }

        @Override
        public HealthCheckResponse checkHealth() throws Exception {
            return new HealthCheckResponse() {

                @Override
                public String getName() {
                    return "logged>errors";
                }

                @Override
                public double getHealth() {
                    return loggerSummary.errors > 0 ? 0d : 1d;
                }

                @Override
                public String getStatus() {
                    return "infos:" + loggerSummary.infos + " warns:" + loggerSummary.warns + " errors:" + loggerSummary.errors;
                }

                @Override
                public String getDescription() {
                    String[] errors = loggerSummary.lastNErrors.get();
                    return "Recent Errors:\n" + Joiner.on("\n").join(Objects.firstNonNull(errors, new String[]{""}));
                }

                @Override
                public String getResolution() {
                    return "Look confirn errors are benign. Click/Curl/Hit to clear reset. http://" + instanceConfig.getHost() + ":" + instanceConfig
                        .getManagePort() + "/manage/resetErrors";
                }

                @Override
                public long getTimestamp() {
                    return System.currentTimeMillis();
                }
            };
        }

    }

    public void addErrorHealthChecks() {
        restfulManageServer.addHealthCheck(new LoggerSummaryHealthCheck(LoggerSummary.INSTANCE),
            new LoggerSummaryHealthCheck(LoggerSummary.INSTANCE_EXTERNAL_INTERACTIONS));
    }

    public ServiceHandle buildServer() throws Exception {
        if (serverStarted.compareAndSet(false, true)) {
            String applicationName = instanceConfig.getServiceName() + " " + instanceConfig.getClusterName();
            ComponentHealthCheck healthCheck = new ComponentHealthCheck("'" + applicationName + "' service initialization");
            restfulManageServer.addHealthCheck(healthCheck);
            try {
                restfulServer.addContextHandler("/", jerseyEndpoints);
                final RestfulServer server = restfulServer.build();
                healthCheck.setHealthy("'" + applicationName + "' service is initialized.");
                startedUpBanner();
                restfulManageServer.addHealthCheck((HealthCheck) () -> {
                    String status = "Currently this service on port:"
                        + instanceConfig.getMainPort() + " has " + server.getIdleThreads() + " idle thread/s.";
                    String description = "How many free thread are available to handle http request.";
                    String resolution = "Increase the number or threads or add more services.";
                    return new HealthCheckResponseImpl("main>http>threadPool",
                        server.isLowOnThreads() ? 0d : 1d,
                        status, description, resolution, System.currentTimeMillis());
                });
                return server;
            } catch (Exception x) {
                healthCheck.setUnhealthy("Failed to initialize service '" + applicationName + "'.", x);
                throw x;
            }
        } else {
            throw new IllegalStateException("Cannot start server more than once.");
        }
    }

    void banneredOneLiner(String message) {
        LOG.info(pad("-", "", "-", '-', 100));
        LOG.info(pad("|", "", "|", ' ', 100));
        LOG.info(pad("|", "      " + message, "|", ' ', 100));
        LOG.info(pad("|", "", "|", ' ', 100));
        LOG.info(pad("-", "", "-", '-', 100));
    }

    void startedUpBanner() {

        LOG.info(pad("-", "", "-", '-', 100));
        LOG.info(pad("|", "", "|", ' ', 100));
        LOG.info(pad("|", "      Service INSTANCEKEY:" + instanceConfig.getInstanceKey(), "|", ' ', 100));
        LOG.info(pad("|", "", "|", ' ', 100));
        LOG.info(pad("|", "      Service CLUSTER:" + instanceConfig.getClusterName(), "|", ' ', 100));
        LOG.info(pad("|", "         Service HOST:" + instanceConfig.getHost(), "|", ' ', 100));
        LOG.info(pad("|", "      Service SERVICE:" + instanceConfig.getServiceName(), "|", ' ', 100));
        LOG.info(pad("|", "     Service INSTANCE:" + instanceConfig.getInstanceName(), "|", ' ', 100));
        LOG.info(pad("|", "         Primary PORT:" + instanceConfig.getMainPort(), "|", ' ', 100));
        LOG.info(pad("|", "          Manage PORT:" + instanceConfig.getManagePort(), "|", ' ', 100));
        LOG.info(pad("|", "", "|", ' ', 100));
        LOG.info(pad("|", "        curl " + instanceConfig.getHost() + ":" + instanceConfig.getManagePort() + "/manage/help", "|", ' ', 100));
        LOG.info(pad("|", "", "|", ' ', 100));
        LOG.info(pad("-", "", "-", '-', 100));
    }

    String pad(String prefic, String string, String postFix, char pad, int totalLength) {
        if (string.length() >= totalLength) {
            return string;
        }
        char[] padding = new char[totalLength - string.length()];
        Arrays.fill(padding, pad);
        return prefic + string + new String(padding) + postFix;
    }
}
