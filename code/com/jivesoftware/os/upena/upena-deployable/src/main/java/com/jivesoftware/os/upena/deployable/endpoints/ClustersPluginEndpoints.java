package com.jivesoftware.os.upena.deployable.endpoints;

import com.google.common.base.Optional;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.ClustersPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ClustersPluginRegion.ClustersPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import javax.inject.Singleton;
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
@Path("/ui/clusters")
public class ClustersPluginEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SoyService soyService;
    private final ClustersPluginRegion pluginRegion;

    public ClustersPluginEndpoints(@Context SoyService soyService, @Context ClustersPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response clusters() {
        String rendered = soyService.renderPlugin(pluginRegion,
            Optional.of(new ClustersPluginRegionInput("", "", "", "")));
        return Response.ok(rendered).build();
    }

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@FormParam("key") @DefaultValue("") String key,
        @FormParam("name") @DefaultValue("") String name,
        @FormParam("description") @DefaultValue("") String description,
        @FormParam("action") @DefaultValue("") String action) {
        String rendered = soyService.renderPlugin(pluginRegion,
            Optional.of(new ClustersPluginRegionInput(key, name, description, action)));
        return Response.ok(rendered).build();
    }

    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(ClustersPluginRegion.ReleaseGroupUpdate update) {

        try {
            pluginRegion.add(update);
            return Response.ok().build();
        } catch (Exception x) {
            LOG.error("Failed to add to default release groups for:" + update, x);
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remove(ClustersPluginRegion.ReleaseGroupUpdate update) {
        try {
            pluginRegion.remove(update);
            return Response.ok().build();
        } catch (Exception x) {
            LOG.error("Failed to remove to default release groups for:" + update, x);
            return Response.serverError().build();
        }
    }

}
