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
package com.jivesoftware.os.upena.deployable;

import com.jivesoftware.os.jive.utils.jaxrs.util.ResponseHelper;
import com.jivesoftware.os.jive.utils.logger.MetricLogger;
import com.jivesoftware.os.jive.utils.logger.MetricLoggerFactory;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.uba.service.Nanny;
import com.jivesoftware.os.upena.uba.service.UbaService;
import java.util.Map.Entry;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author jonathan.colt
 */
@Singleton
@Path("/upena/health")
public class UpenaHealthEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final UpenaStore upenaStore;
    private final UpenaService upenaService;
    private final UbaService ubaService;

    public UpenaHealthEndpoints(@Context UpenaStore upenaStore,
        @Context UpenaService upenaService,
        @Context UbaService ubaService) {
        this.upenaStore = upenaStore;
        this.upenaService = upenaService;
        this.ubaService = ubaService;
    }

    @GET
    @Consumes("application/json")
    @Path("/all")
    public Response getHealthAll() {
        try {
            StringBuilder s = new StringBuilder();
            for (Entry<String, Nanny> nannies : ubaService.iterateNannies()) {
                InstanceDescriptor id = nannies.getValue().getInstanceDescriptor();

                String instance = id.clusterName + " " + id.serviceName + " " + id.instanceName + " " + id.releaseGroupName;
                s.append("\t").append(instance).append("\n");
                s.append("\t\t").append(nannies.getValue().getDeployLog().getState()).append("\n");
                for (String log : nannies.getValue().getDeployLog().copyLog()) {
                    s.append("\t\t\t").append(log).append("\n");
                }
            }
            return Response.ok(s.toString(), MediaType.TEXT_PLAIN).build();
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

//    @GET
//    @Consumes("application/json")
//    @Path("/instance")
//    public Response getInstanceHealth(@QueryParam("instanceId") String clusterName) {
//        try {
//
//            return Response.ok("Propegated upena to: " + scpHost + ":" + scpHome, MediaType.TEXT_PLAIN).build();
//        } catch (Exception x) {
//            LOG.warn("Failed to deploy upena to: " + scpHost + ":" + scpHome, x);
//            return ResponseHelper.INSTANCE.errorResponse("Failed to deploy upena to:" + scpHost + ":" + scpHome, x);
//        }
//    }
}
