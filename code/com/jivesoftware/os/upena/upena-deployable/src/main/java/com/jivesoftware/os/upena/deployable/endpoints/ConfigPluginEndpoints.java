package com.jivesoftware.os.upena.deployable.endpoints;

import com.google.common.base.Optional;
import com.jivesoftware.os.upena.deployable.region.ConfigPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ConfigPluginRegion.ConfigPluginRegionInput;
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
    public Response services() {
        String rendered = soyService.renderPlugin(pluginRegion,
            Optional.of(new ConfigPluginRegionInput("", "", "", "", "", "", "", "", "", "", "", "")));
        return Response.ok(rendered).build();
    }

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@FormParam("aCluster") @DefaultValue("") String aCluster,
        @FormParam("aHost") @DefaultValue("") String aHost,
        @FormParam("aService") @DefaultValue("") String aService,
        @FormParam("aInstance") @DefaultValue("") String aInstance,
        @FormParam("aRelease") @DefaultValue("") String aRelease,
        @FormParam("bCluster") @DefaultValue("") String bCluster,
        @FormParam("bHost") @DefaultValue("") String bHost,
        @FormParam("bService") @DefaultValue("") String bService,
        @FormParam("bInstance") @DefaultValue("") String bInstance,
        @FormParam("bRelease") @DefaultValue("") String bRelease,
        @FormParam("property") @DefaultValue("") String property,
        @FormParam("overriden") @DefaultValue("") String overriden) {
        String rendered = soyService.renderPlugin(pluginRegion,
            Optional.of(new ConfigPluginRegionInput(aCluster, aHost, aService, aInstance, aRelease,
                    bCluster, bHost, bService, bInstance, bRelease,
                    property, overriden)));
        return Response.ok(rendered).build();
    }

}
