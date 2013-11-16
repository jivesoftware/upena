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
