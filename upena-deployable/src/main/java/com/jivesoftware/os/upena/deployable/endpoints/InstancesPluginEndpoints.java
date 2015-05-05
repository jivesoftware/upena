package com.jivesoftware.os.upena.deployable.endpoints;

import com.google.common.base.Optional;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.InstancesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.InstancesPluginRegion.InstancesPluginRegionInput;
import com.jivesoftware.os.upena.deployable.region.InstancesPluginRegion.PortUpdate;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Singleton
@Path("/ui/instances")
public class InstancesPluginEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SoyService soyService;
    private final InstancesPluginRegion pluginRegion;

    public InstancesPluginEndpoints(@Context SoyService soyService, @Context InstancesPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response instances(@Context HttpServletRequest httpRequest) {
        String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
            Optional.of(new InstancesPluginRegionInput("", "", "", "", "", "", "", "", "", "", false, "")));
        return Response.ok(rendered).build();
    }

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("key") @DefaultValue("") String key,
        @FormParam("clusterKey") @DefaultValue("") String clusterKey,
        @FormParam("cluster") @DefaultValue("") String cluster,
        @FormParam("hostKey") @DefaultValue("") String hostKey,
        @FormParam("host") @DefaultValue("") String host,
        @FormParam("serviceKey") @DefaultValue("") String serviceKey,
        @FormParam("service") @DefaultValue("") String service,
        @FormParam("instanceId") @DefaultValue("") String instanceId,
        @FormParam("releaseKey") @DefaultValue("") String releaseKey,
        @FormParam("release") @DefaultValue("") String release,
        @FormParam("enabled") @DefaultValue("false") boolean enabled,
        @FormParam("action") @DefaultValue("") String action) {
        String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
            Optional.of(new InstancesPluginRegionInput(
                    key, clusterKey, cluster, hostKey, host, serviceKey, service, instanceId, releaseKey, release, enabled, action)));
        return Response.ok(rendered).build();
    }

    @POST
    @Path("/ports/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@Context HttpServletRequest httpRequest, PortUpdate update) {

        try {
            pluginRegion.add(httpRequest.getRemoteUser(), update);
            return Response.ok().build();
        } catch (Exception x) {
            LOG.error("Failed to add ports for:" + update, x);
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/ports/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remove(@Context HttpServletRequest httpRequest, PortUpdate update) {
        try {
            pluginRegion.remove(httpRequest.getRemoteUser(), update);
            return Response.ok().build();
        } catch (Exception x) {
            LOG.error("Failed to remove to ports for:" + update, x);
            return Response.serverError().build();
        }
    }

}
