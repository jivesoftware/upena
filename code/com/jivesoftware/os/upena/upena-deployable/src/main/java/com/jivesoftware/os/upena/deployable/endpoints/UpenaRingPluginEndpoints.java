package com.jivesoftware.os.upena.deployable.endpoints;

import com.google.common.base.Optional;
import com.jivesoftware.os.upena.deployable.region.UpenaRingPluginRegion;
import com.jivesoftware.os.upena.deployable.region.UpenaRingPluginRegion.UpenaRingPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Singleton
@Path("/ring")
public class UpenaRingPluginEndpoints {

    private final SoyService soyService;
    private final UpenaRingPluginRegion pluginRegion;

    public UpenaRingPluginEndpoints(@Context SoyService soyService, @Context UpenaRingPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response query() {
        String rendered = soyService.renderPlugin(pluginRegion,
            Optional.of(new UpenaRingPluginRegionInput("foo")));
        return Response.ok(rendered).build();
    }
}
