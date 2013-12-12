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
import com.jivesoftware.os.upena.routing.shared.ConnectionDescriptor;
import com.jivesoftware.os.upena.routing.shared.InMemoryConnectionsDescriptorsProvider;
import java.util.HashMap;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/manual/tenant/routing")
public class ManualTenantRoutingRestEndpoints {

    private final InMemoryConnectionsDescriptorsProvider connectionsDescriptorsProvider;

    public ManualTenantRoutingRestEndpoints(@Context InMemoryConnectionsDescriptorsProvider connectionsDescriptorsProvider) {
        this.connectionsDescriptorsProvider = connectionsDescriptorsProvider;
    }

    @GET
    @Path("/clear")
    public Response clear(@QueryParam("tenantId") String tenantId,
            @QueryParam("instanceId") String instanceId,
            @QueryParam("connectToServiceNamed") String connectToServiceNamed,
            @QueryParam("portName") String portName) {
        connectionsDescriptorsProvider.clear(tenantId, instanceId, connectToServiceNamed, portName);
        return ResponseHelper.INSTANCE.jsonResponse("Cleared");
    }

    @GET
    @Path("/set")
    public Response set(@QueryParam("tenantId") String tenantId,
            @QueryParam("instanceId") String instanceId,
            @QueryParam("connectToServiceNamed") String connectToServiceNamed,
            @QueryParam("portName") String portName,
            @QueryParam("host") String host,
            @QueryParam("port") int port) {

        ConnectionDescriptor connectionDescriptor = new ConnectionDescriptor(host, port, new HashMap<String, String>());
        connectionsDescriptorsProvider.set(tenantId, instanceId, connectToServiceNamed, portName, connectionDescriptor);
        return ResponseHelper.INSTANCE.jsonResponse("Set: tenantId=" + tenantId
                + "&instanceId=" + instanceId
                + "&connectToServiceNamed=" + connectToServiceNamed
                + "&portName=" + portName + "->" + connectionDescriptor);
    }

    @GET
    @Path("/get")
    public Response get(@QueryParam("tenantId") String tenantId,
            @QueryParam("instanceId") String instanceId,
            @QueryParam("connectToServiceNamed") String connectToServiceNamed,
            @QueryParam("portName") String portName) {
        return ResponseHelper.INSTANCE.jsonResponse(connectionsDescriptorsProvider.get(tenantId, instanceId, connectToServiceNamed, portName));
    }

    @GET
    @Path("/keys")
    public Response getKeys() {
        return ResponseHelper.INSTANCE.jsonResponse(connectionsDescriptorsProvider.getRequestedRoutingKeys());
    }
}
