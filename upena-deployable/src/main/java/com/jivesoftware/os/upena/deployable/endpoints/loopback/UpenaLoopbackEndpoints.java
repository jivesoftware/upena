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
package com.jivesoftware.os.upena.deployable.endpoints.loopback;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptorsRequest;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptorsResponse;
import com.jivesoftware.os.routing.bird.shared.InstanceConnectionHealth;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptorsRequest;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptorsResponse;
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import com.jivesoftware.os.upena.deployable.UpenaHealth;
import com.jivesoftware.os.upena.service.DiscoveredRoutes;
import com.jivesoftware.os.upena.service.SessionValidation;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 *
 * @author jonathan.colt
 */
@Singleton
@Path("/")
public class UpenaLoopbackEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final ObjectMapper mapper = new ObjectMapper();
    private final UpenaHealth upenaHealth;
    private final DiscoveredRoutes discoveredRoutes;
    private final UpenaService upenaService;
    private final UpenaStore upenaStore;

    public UpenaLoopbackEndpoints(@Context UpenaHealth upenaHealth,
        @Context DiscoveredRoutes discoveredRoutes,
        @Context UpenaService upenaService,
        @Context UpenaStore upenaStore) {

        this.upenaHealth = upenaHealth;
        this.discoveredRoutes = discoveredRoutes;
        this.upenaService = upenaService;
        this.upenaStore =upenaStore;
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @POST
    @Consumes("application/json")
    @Path("/request/connections")
    public Response requestConnections(ConnectionDescriptorsRequest connectionsRequest) {
        try {
            LOG.info("connectionsRequest:" + connectionsRequest);
            ConnectionDescriptorsResponse connectionDescriptorsResponse = upenaService.connectionRequest(connectionsRequest);
            LOG.info("connectionDescriptorsResponse:" + connectionDescriptorsResponse);
            return ResponseHelper.INSTANCE.jsonResponse(connectionDescriptorsResponse);
        } catch (Exception x) {
            LOG.warn("Failed to connectionsRequest:" + connectionsRequest, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to requestConnections for:" + connectionsRequest, x);
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/request/keyStorePassword/{instanceKey}")
    public Response requestKeyStorePassword(@PathParam("instanceKey") String instanceKey) {
        try {
            return Response.ok(upenaService.keyStorePassword(instanceKey)).build();
        } catch (Exception x) {
            LOG.warn("Failed to provide password for:" + instanceKey, x);
            return Response.serverError().build();
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/request/instance/publicKey/{instanceKey}")
    public Response requestInstancePublicKey(@PathParam("instanceKey") String instanceKey) {
        try {
            return Response.ok(mapper.writeValueAsString(upenaService.instancePublicKey(instanceKey))).build();
        } catch (Exception x) {
            LOG.warn("Failed to provide password for:" + instanceKey, x);
            return Response.serverError().build();
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/session/validate")
    public Response sessionValidate(SessionValidation sessionValidation) {
        try {
            return Response.ok(upenaService.isValid(sessionValidation)).build();
        } catch (Exception x) {
            LOG.warn("Failed validate session", x);
            return Response.serverError().build();
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/connections/health")
    public Response connectionsHealth(InstanceConnectionHealth instanceConnectionHealth) {
        try {
            discoveredRoutes.connectionHealth(instanceConnectionHealth);
            return ResponseHelper.INSTANCE.jsonResponse("thanks");
        } catch (Exception x) {
            LOG.warn("Failed to connectionsHealth:" + instanceConnectionHealth, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to requestConnections for:" + instanceConnectionHealth, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/request/instanceDescriptors")
    public Response instanceDescriptors(InstanceDescriptorsRequest instanceDescriptorsRequest) {
        try {
            LOG.debug("instanceDescriptorsRequest:" + instanceDescriptorsRequest);
            InstanceDescriptorsResponse response = upenaService.instanceDescriptors(instanceDescriptorsRequest);
            LOG.debug("returning:" + response + " for instanceDescriptorsRequest:" + instanceDescriptorsRequest);
            return ResponseHelper.INSTANCE.jsonResponse(response);
        } catch (Exception x) {
            LOG.warn("Failed to instanceDescriptorsRequest:" + instanceDescriptorsRequest, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to instanceDescriptorsRequest for:" + instanceDescriptorsRequest, x);
        }
    }

    @GET
    @Path("/health/check/{clusterName}/{healthy}")
    public Response getHealthCheck(@Context UriInfo uriInfo,
        @PathParam("clusterName") String clusterName,
        @PathParam("healthy") float health) {
        try {
            UpenaHealth.NodeHealth upenaHealth = this.upenaHealth.buildNodeHealth();
            double minHealth = 1.0d;
            StringBuilder sb = new StringBuilder();
            sb.append("<ul>");
            for (UpenaHealth.NannyHealth nannyHealth : upenaHealth.nannyHealths) {
                if (clusterName.equals("all") || nannyHealth.instanceDescriptor.clusterName.equals(clusterName)) {
                    if (nannyHealth.serviceHealth.health < health) {
                        for (UpenaHealth.Health h : nannyHealth.serviceHealth.healthChecks) {
                            if (h.health < health) {
                                sb.append("<li>");
                                sb.append(nannyHealth.instanceDescriptor.clusterName).append(":");
                                sb.append(nannyHealth.instanceDescriptor.serviceName).append(":");
                                sb.append(nannyHealth.instanceDescriptor.releaseGroupName).append(":");
                                sb.append(nannyHealth.instanceDescriptor.instanceName).append("=");
                                sb.append(h.health);
                                sb.append("</li>");
                                sb.append("<li><ul>");
                                sb.append("</li>").append(h.status).append("</li>");
                                sb.append("</ul></li>");

                                if (h.health < minHealth) {
                                    minHealth = h.health;
                                }
                            }
                        }
                    }
                }
            }
            sb.append("</ul>");
            if (minHealth < health) {
                upenaStore.record(upenaHealth.host, "checkHealth", System.currentTimeMillis(), "health:" + minHealth + " < " + health, "endpoint", sb
                    .toString());
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(sb.toString()).type(MediaType.TEXT_PLAIN).build();
            } else {
                return Response.ok(minHealth, MediaType.TEXT_PLAIN).build();
            }
        } catch (Exception x) {
            LOG.error("Failed to check instance health. {} {} ", new Object[]{clusterName, health}, x);
            return Response.serverError().entity(x.toString()).type(MediaType.TEXT_PLAIN).build();
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/health/cluster")
    public Response getClusterHealth(@Context UriInfo uriInfo) {
        try {
            UpenaHealth.ClusterHealth clusterHealth = upenaHealth.buildClusterHealth(uriInfo);
            return ResponseHelper.INSTANCE.jsonResponse(clusterHealth);
        } catch (Exception x) {
            LOG.error("Failed getting cluster health", x);
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

   
}
