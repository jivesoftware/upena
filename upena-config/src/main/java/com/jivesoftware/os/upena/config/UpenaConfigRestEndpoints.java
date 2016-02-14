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
package com.jivesoftware.os.upena.config;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.deployable.config.shared.DeployableConfig;
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import java.util.ArrayList;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/upenaConfig")
public class UpenaConfigRestEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final UpenaConfigStore upenaConfigStore;

    public UpenaConfigRestEndpoints(@Context UpenaConfigStore upenaConfigStore) {
        this.upenaConfigStore = upenaConfigStore;
    }

    @POST
    @Consumes("application/json")
    @Path("/set")
    public Response set(DeployableConfig config) {
        try {
            LOG.debug("Attempting to get: " + config);
            upenaConfigStore.putAll(config.instanceKey, config.context, config.properties);
            Map<String, String> got = upenaConfigStore.get(config.instanceKey, config.context, new ArrayList<>(config.properties.keySet()), true);
            LOG.info("Set " + got.size() + " properties");
            return ResponseHelper.INSTANCE.jsonResponse(new DeployableConfig(config.context,
                config.instanceKey, config.instanceVersion, got));
        } catch (Exception x) {
            LOG.warn("Failed to get: " + config, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to get " + config, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/get")
    public Response get(DeployableConfig config) {
        try {
            LOG.debug("Attempting to get: " + config);
            Map<String, String> got = upenaConfigStore.get(config.instanceKey, config.context,
                new ArrayList<>(config.properties.keySet()), config.context.startsWith("override")); // barf
            LOG.info("Got " + got.size() + " properties for " + config);
            return ResponseHelper.INSTANCE.jsonResponse(new DeployableConfig(config.context,
                config.instanceKey, config.instanceVersion, got));
        } catch (Exception x) {
            LOG.warn("Failed to get: " + config, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to get " + config, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/remove")
    public Response remove(DeployableConfig config) {
        try {
            LOG.debug("Attempting to remove: " + config);
            upenaConfigStore.remove(config.instanceKey, config.context,
                config.properties.keySet());
            Map<String, String> got = upenaConfigStore.get(config.instanceKey, config.context,
                new ArrayList<>(config.properties.keySet()), true);
            LOG.info("Removed " + got.size() + " properties");
            return ResponseHelper.INSTANCE.jsonResponse(new DeployableConfig(config.context,
                config.instanceKey, config.instanceVersion, got));
        } catch (Exception x) {
            LOG.warn("Failed to remove: " + config, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to get " + config, x);
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/instanceConfig")
    public Response getInstanceConfig(@QueryParam("instanceKey") String instanceKey, @QueryParam("context") String context) {
        try {
            LOG.debug("Attempting to get: " + instanceKey);
            Map<String, String> got = upenaConfigStore.get(instanceKey, context, new ArrayList<>(), false);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : got.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                sb.append(key).append('=').append(value).append('\n');
            }

            return Response.ok(sb.toString(), MediaType.TEXT_PLAIN).build();
        } catch (Exception x) {
            LOG.warn("Failed to get: " + instanceKey + " " + context, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to get " + instanceKey + " " + context, x);
        }
    }
}
