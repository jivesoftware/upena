package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
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

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SoyService soyService;
    private final HealthPluginRegion pluginRegion;
    private final ResponseHelper responseHelper = ResponseHelper.INSTANCE;

    public HealthPluginEndpoints(@Context SoyService soyService, @Context HealthPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response filter(@Context HttpServletRequest httpRequest,
        @QueryParam("cluster") @DefaultValue("") String cluster,
        @QueryParam("host") @DefaultValue("") String host,
        @QueryParam("service") @DefaultValue("") String service) {
        String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
            new HealthPluginRegionInput(cluster, host, service));
        return Response.ok(rendered).build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/uis")
    public Response uis(@QueryParam("instanceKey") @DefaultValue("") String instanceKey) throws Exception {
        return Response.ok(pluginRegion.renderInstanceHealth(instanceKey)).build();
    }

    @GET
    @Path("/live")
    @Produces(MediaType.APPLICATION_JSON)
    public Response live(@Context HttpServletRequest httpRequest,
        @QueryParam("cluster") @DefaultValue("") String cluster,
        @QueryParam("host") @DefaultValue("") String host,
        @QueryParam("service") @DefaultValue("") String service) {
        return Response.ok(pluginRegion.renderLive(httpRequest.getRemoteUser(), new HealthPluginRegionInput(cluster, host, service))).build();
    }


    @POST
    @Path("/poll")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response poll(
        @FormParam("cluster") @DefaultValue("dev") String cluster,
        @FormParam("host") @DefaultValue("") String host,
        @FormParam("service") @DefaultValue("") String service) {
        try {
            Map<String, Object> result = pluginRegion.poll(new HealthPluginRegionInput(cluster,
                host,
                service));
            return responseHelper.jsonResponse(result != null ? result : "");
        } catch (Exception e) {
            LOG.error("Health poll failed", e);
            return responseHelper.errorResponse("Health poll failed", e);
        }
    }

}
