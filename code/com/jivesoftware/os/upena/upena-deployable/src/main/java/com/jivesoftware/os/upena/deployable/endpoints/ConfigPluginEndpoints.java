package com.jivesoftware.os.upena.deployable.endpoints;

import com.google.common.base.Optional;
import com.jivesoftware.os.upena.deployable.region.ConfigPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ConfigPluginRegion.ConfigPluginRegionInput;
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
@Path("/config")
public class ConfigPluginEndpoints {

    private final SoyService soyService;
    private final ConfigPluginRegion pluginRegion;

    public ConfigPluginEndpoints(@Context SoyService soyService, @Context ConfigPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response query() {
        String rendered = soyService.renderPlugin(pluginRegion,
            Optional.of(new ConfigPluginRegionInput("foo")));
        return Response.ok(rendered).build();
    }
}
