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

import com.jivesoftware.os.jive.utils.base.service.ServiceHandle;
import com.jivesoftware.os.server.http.jetty.jersey.endpoints.configuration.MainProperties;
import com.jivesoftware.os.server.http.jetty.jersey.endpoints.configuration.MainPropertiesEndpoints;
import com.jivesoftware.os.server.http.jetty.jersey.server.InitializeRestfulServer;
import com.jivesoftware.os.server.http.jetty.jersey.server.JerseyEndpoints;
import com.jivesoftware.os.server.http.jetty.jersey.server.RestfulManageServer;
import com.jivesoftware.os.server.http.jetty.jersey.server.util.Resource;
import com.jivesoftware.os.upena.routing.shared.TenantRoutingProvider;
import com.jivesoftware.os.upena.tenant.routing.http.server.endpoints.TenantRoutingRestEndpoints;
import com.jivesoftware.os.upena.uba.config.extractor.ConfigBinder;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.merlin.config.Config;

public class Deployable {

    private final ConfigBinder configBinder;
    private final TenantRoutingProvider tenantRoutingProvider;
    private final RestfulManageServer restfulManageServer;
    private final AtomicBoolean manageServerStarted = new AtomicBoolean(false);
    //--------------------------------------------------------------------------
    private final InitializeRestfulServer restfulServer;
    private final JerseyEndpoints jerseyEndpoints;
    private final AtomicBoolean serverStarted = new AtomicBoolean(false);

    public Deployable(String[] args) throws IOException {
        configBinder = new ConfigBinder(args);
        RoutableDeployableConfig routableDeployableConfig = configBinder.bind(RoutableDeployableConfig.class);

        TenantRoutingBirdBuilder tenantRoutingBirdBuilder = new TenantRoutingBirdBuilder(routableDeployableConfig.getRoutesHost(),
                routableDeployableConfig.getRoutesPort());
        tenantRoutingBirdBuilder.setInstanceId(routableDeployableConfig.getInstanceGUID());
        tenantRoutingProvider = tenantRoutingBirdBuilder.build();

        String applicationName = "manage " + routableDeployableConfig.getServiceName() + " " + routableDeployableConfig.getCluster();
        restfulManageServer = new RestfulManageServer(routableDeployableConfig.getManagePort(),
                applicationName,
                128,
                10000);

        restfulManageServer.addEndpoint(TenantRoutingRestEndpoints.class);
        restfulManageServer.addInjectable(TenantRoutingProvider.class, tenantRoutingProvider);
        restfulManageServer.addEndpoint(MainPropertiesEndpoints.class);
        restfulManageServer.addInjectable(MainProperties.class, new MainProperties(args));

        jerseyEndpoints = new JerseyEndpoints();
        restfulServer = new InitializeRestfulServer(routableDeployableConfig.getPort(),
                applicationName,
                128,
                10000);
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

    public ServiceHandle buildManageServer() throws Exception {
        if (manageServerStarted.compareAndSet(false, true)) {
            long time = System.currentTimeMillis();
            RoutableDeployableConfig routableDeployableConfig = configBinder.bind(RoutableDeployableConfig.class);
            String applicationName = "manage " + routableDeployableConfig.getServiceName() + " " + routableDeployableConfig.getCluster();
            ComponentHealthCheck healthCheck = new ComponentHealthCheck("'" + applicationName + "'service initialization", true);
            restfulManageServer.addHealthCheck(healthCheck);
            try {
                restfulManageServer.initialize();
                healthCheck.setHealthy("'" + applicationName + "' service is initialized.");
                banneredOneLiner("'" + applicationName + "' service started. Elapse:" + (System.currentTimeMillis() - time));
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
        restfulManageServer.addEndpoint(clazz);
    }

    public void addInjectables(Class clazz, Object injectable) {
        if (serverStarted.get()) {
            throw new IllegalStateException("Cannot add injectables after the server has been started.");
        }
        restfulManageServer.addInjectable(clazz, injectable);
    }

    public void addResource(Resource resource) {
        restfulServer.addResource(resource);
    }

    public ServiceHandle buildServer() throws Exception {
        if (manageServerStarted.compareAndSet(false, true)) {
            RoutableDeployableConfig routableDeployableConfig = configBinder.bind(RoutableDeployableConfig.class);
            String applicationName = routableDeployableConfig.getServiceName() + " " + routableDeployableConfig.getCluster();
            ComponentHealthCheck healthCheck = new ComponentHealthCheck("'" + applicationName + "' service initialization", true);
            restfulManageServer.addHealthCheck(healthCheck);
            try {
                restfulServer.addContextHandler("/", jerseyEndpoints);
                ServiceHandle serviceHandle = restfulServer.build();
                healthCheck.setHealthy("'" + applicationName + "' service is initialized.");
                startedUpBanner();
                return serviceHandle;
            } catch (Exception x) {
                healthCheck.setUnhealthy("Failed to initialize service '" + applicationName + "'.", x);
                throw x;
            }
        } else {
            throw new IllegalStateException("Cannot start server more than once.");
        }
    }

    void banneredOneLiner(String message) {
        System.out.println(pad("-", "", "-", '-', 100));
        System.out.println(pad("|", "", "|", ' ', 100));
        System.out.println(pad("|", "      " + message, "|", ' ', 100));
        System.out.println(pad("|", "", "|", ' ', 100));
        System.out.println(pad("-", "", "-", '-', 100));
    }

    void startedUpBanner() {
        RoutableDeployableConfig routableDeployableConfig = configBinder.bind(RoutableDeployableConfig.class);

        System.out.println(pad("-", "", "-", '-', 100));
        System.out.println(pad("|", "", "|", ' ', 100));
        System.out.println(pad("|", "      Service INSTANCEID:" + routableDeployableConfig.getInstanceGUID(), "|", ' ', 100));
        System.out.println(pad("|", "", "|", ' ', 100));
        System.out.println(pad("|", "      Service CLUSTER:" + routableDeployableConfig.getCluster(), "|", ' ', 100));
        System.out.println(pad("|", "         Service HOST:" + routableDeployableConfig.getHost(), "|", ' ', 100));
        System.out.println(pad("|", "      Service SERVICE:" + routableDeployableConfig.getServiceName(), "|", ' ', 100));
        System.out.println(pad("|", "     Service INSTANCE:" + routableDeployableConfig.getInstanceId(), "|", ' ', 100));
        System.out.println(pad("|", "         Primary PORT:" + routableDeployableConfig.getPort(), "|", ' ', 100));
        System.out.println(pad("|", "          Manage PORT:" + routableDeployableConfig.getManagePort(), "|", ' ', 100));
        System.out.println(pad("|", "", "|", ' ', 100));
        System.out.println(pad("|", "        curl " + routableDeployableConfig.getHost()
                + ":" + routableDeployableConfig.getManagePort() + "/manage/help", "|", ' ', 100));
        System.out.println(pad("|", "", "|", ' ', 100));
        System.out.println(pad("-", "", "-", '-', 100));
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
