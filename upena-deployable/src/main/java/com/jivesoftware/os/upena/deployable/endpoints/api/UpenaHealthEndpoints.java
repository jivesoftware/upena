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
package com.jivesoftware.os.upena.deployable.endpoints.api;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import com.jivesoftware.os.upena.deployable.UpenaHealth;
import com.jivesoftware.os.upena.deployable.UpenaHealth.NodeHealth;
import io.swagger.annotations.Api;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 *
 * @author jonathan.colt
 */
@Api(value = "Upena Health Check")
@Singleton
@Path("/upena/health")
public class UpenaHealthEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final UpenaHealth upenaHealth;
    
    public UpenaHealthEndpoints(@Context UpenaHealth upenaHealth) {

        this.upenaHealth = upenaHealth;
    }

    @GET
    @Consumes("application/json")
    @Path("/instance")
    public Response getInstanceHealth() {
        try {
            NodeHealth node = upenaHealth.buildNodeHealth();
            return ResponseHelper.INSTANCE.jsonResponse(node);
        } catch (Exception x) {
            LOG.error("Failed getting instance health", x);
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

}
