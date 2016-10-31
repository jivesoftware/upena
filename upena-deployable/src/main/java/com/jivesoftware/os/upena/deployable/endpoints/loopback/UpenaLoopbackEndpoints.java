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
import com.jivesoftware.os.upena.service.DiscoveredRoutes;
import com.jivesoftware.os.upena.service.SessionValidation;
import com.jivesoftware.os.upena.service.UpenaService;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 *
 * @author jonathan.colt
 */
@Singleton
@Path("/")
public class UpenaLoopbackEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();


    private final ObjectMapper mapper = new ObjectMapper();
    private final DiscoveredRoutes discoveredRoutes;
    private final UpenaService upenaService;
    
    public UpenaLoopbackEndpoints(
        @Context DiscoveredRoutes discoveredRoutes,
        @Context UpenaService upenaService) {

        this.discoveredRoutes = discoveredRoutes;
        this.upenaService = upenaService;
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
}
