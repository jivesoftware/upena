package com.jivesoftware.os.upena.deployable.endpoints;

import com.google.common.base.Optional;
import com.jivesoftware.os.upena.deployable.region.InstancesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.InstancesPluginRegion.InstancesPluginRegionInput;
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
@Path("/ui/instances")
public class InstancesPluginEndpoints {

    private final SoyService soyService;
    private final InstancesPluginRegion pluginRegion;

    public InstancesPluginEndpoints(@Context SoyService soyService, @Context InstancesPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response instances() {
        String rendered = soyService.renderPlugin(pluginRegion,
            Optional.of(new InstancesPluginRegionInput("", "", "", "", "", "", "")));
        return Response.ok(rendered).build();
    }

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@FormParam("key") @DefaultValue("") String key,
        @FormParam("cluster") @DefaultValue("") String cluster,
        @FormParam("host") @DefaultValue("") String host,
        @FormParam("service") @DefaultValue("") String service,
        @FormParam("id") @DefaultValue("") String id,
        @FormParam("release") @DefaultValue("") String release,
        @FormParam("action") @DefaultValue("") String action) {
        String rendered = soyService.renderPlugin(pluginRegion,
            Optional.of(new InstancesPluginRegionInput(key, cluster, host, service, id, release, action)));
        return Response.ok(rendered).build();
    }

}
