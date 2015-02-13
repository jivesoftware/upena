package com.jivesoftware.os.upena.deployable.endpoints;

import com.google.common.base.Optional;
import com.jivesoftware.os.upena.deployable.region.HostsPluginRegion;
import com.jivesoftware.os.upena.deployable.region.HostsPluginRegion.HostsPluginRegionInput;
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
@Path("/hosts")
public class HostsPluginEndpoints {

    private final SoyService soyService;
    private final HostsPluginRegion pluginRegion;

    public HostsPluginEndpoints(@Context SoyService soyService, @Context HostsPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response hosts() {
        String rendered = soyService.renderPlugin(pluginRegion,
            Optional.of(new HostsPluginRegionInput("", "", "", "", "", "")));
        return Response.ok(rendered).build();
    }

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@FormParam("key") @DefaultValue("") String key,
        @FormParam("name") @DefaultValue("") String name,
        @FormParam("host") @DefaultValue("") String host,
        @FormParam("port") @DefaultValue("") String port,
        @FormParam("workingDirectory") @DefaultValue("") String workingDirectory,
        @FormParam("action") @DefaultValue("") String action) {
        String rendered = soyService.renderPlugin(pluginRegion,
            Optional.of(new HostsPluginRegionInput(key, name, host, port, workingDirectory, action)));
        return Response.ok(rendered).build();
    }
}
