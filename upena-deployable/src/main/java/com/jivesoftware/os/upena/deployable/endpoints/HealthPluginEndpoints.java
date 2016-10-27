package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.HealthPluginRegion;
import com.jivesoftware.os.upena.deployable.region.HealthPluginRegion.HealthPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import java.util.Map;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Singleton
@Path("/ui/health")
public class HealthPluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;

    private final SoyService soyService;
    private final HealthPluginRegion pluginRegion;
    private final ResponseHelper responseHelper = ResponseHelper.INSTANCE;

    public HealthPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context HealthPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response filter(@Context HttpServletRequest httpRequest,
        @QueryParam("datacenter") @DefaultValue("") String datacenter,
        @QueryParam("rack") @DefaultValue("") String rack,
        @QueryParam("cluster") @DefaultValue("") String cluster,
        @QueryParam("host") @DefaultValue("") String host,
        @QueryParam("service") @DefaultValue("") String service) {

        return shiroRequestHelper.call("health", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new HealthPluginRegionInput(datacenter, rack, cluster, host, service));
            return Response.ok(rendered).build();
        });
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/uis")
    public Response uis(@QueryParam("instanceKey") @DefaultValue("") String instanceKey) throws Exception {
        return shiroRequestHelper.call("health/uis", () -> {
            return Response.ok(pluginRegion.renderInstanceHealth(instanceKey)).build();
        });
    }

    @GET
    @Path("/live")
    @Produces(MediaType.APPLICATION_JSON)
    public Response live(@Context HttpServletRequest httpRequest,
        @QueryParam("datacenter") @DefaultValue("") String datacenter,
        @QueryParam("rack") @DefaultValue("") String rack,
        @QueryParam("cluster") @DefaultValue("") String cluster,
        @QueryParam("host") @DefaultValue("") String host,
        @QueryParam("service") @DefaultValue("") String service) {

        return shiroRequestHelper.call("health/live", () -> {
            return Response.ok(pluginRegion.renderLive(httpRequest.getRemoteUser(), new HealthPluginRegionInput(datacenter, rack, cluster, host, service)))
                .build();
        });
    }

    @POST
    @Path("/poll")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response poll(
        @FormParam("datacenter") @DefaultValue("") String datacenter,
        @FormParam("rack") @DefaultValue("") String rack,
        @FormParam("cluster") @DefaultValue("dev") String cluster,
        @FormParam("host") @DefaultValue("") String host,
        @FormParam("service") @DefaultValue("") String service) {

        return shiroRequestHelper.call("health/poll", () -> {
            Map<String, Object> result = pluginRegion.poll(new HealthPluginRegionInput(datacenter, rack, cluster, host, service));
            return responseHelper.jsonResponse(result != null ? result : "");
        });
    }

}
