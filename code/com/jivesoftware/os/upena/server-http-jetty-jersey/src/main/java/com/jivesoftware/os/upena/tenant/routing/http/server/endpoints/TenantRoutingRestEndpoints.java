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
package com.jivesoftware.os.upena.tenant.routing.http.server.endpoints;

import com.jivesoftware.os.jive.utils.jaxrs.util.ResponseHelper;
import com.jivesoftware.os.jive.utils.logger.MetricLogger;
import com.jivesoftware.os.jive.utils.logger.MetricLoggerFactory;
import com.jivesoftware.os.upena.routing.shared.TenantRoutingProvider;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/tenant/routing")
public class TenantRoutingRestEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final TenantRoutingProvider routingProvider;

    public TenantRoutingRestEndpoints(@Context TenantRoutingProvider tenantRoutingConnectionPoolProvider) {
        this.routingProvider = tenantRoutingConnectionPoolProvider;
    }

    @GET
    @Path("/report")
    public Response report() {
        return ResponseHelper.INSTANCE.jsonResponse(routingProvider.getRoutingReport());
    }

    @GET
    @Path("/invaliateAll")
    public Response invalidateAll() {
        routingProvider.invalidateAll();
        return ResponseHelper.INSTANCE.jsonResponse("InvalidatdAll");
    }

    @GET
    @Path("/invalidate")
    public Response invalidate(
            @QueryParam("connectToServiceId") String connectToServiceId,
            @QueryParam("portName") String portName,
            @QueryParam("tenantId") String tenantId) {

        routingProvider.invalidateTenant(connectToServiceId, portName, tenantId);
        return ResponseHelper.INSTANCE.jsonResponse("Invalidated connectToServiceId:" + connectToServiceId + " for tenantId:" + tenantId);
    }
}
