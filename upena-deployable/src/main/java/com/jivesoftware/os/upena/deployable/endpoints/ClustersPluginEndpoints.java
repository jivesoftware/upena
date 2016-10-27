package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.ClustersPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ClustersPluginRegion.ClustersPluginRegionInput;
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
@Path("/ui/clusters")
public class ClustersPluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;
    private final SoyService soyService;
    private final ClustersPluginRegion pluginRegion;

    public ClustersPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context ClustersPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response clusters(@Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("clusters", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(),
                pluginRegion,
                new ClustersPluginRegionInput("", "", "", ""));
            return Response.ok(rendered).build();
        });
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("key") @DefaultValue("") String key,
        @FormParam("name") @DefaultValue("") String name,
        @FormParam("description") @DefaultValue("") String description,
        @FormParam("action") @DefaultValue("") String action) {

        return shiroRequestHelper.call("clusters/action", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new ClustersPluginRegionInput(key, name, description, action));
            return Response.ok(rendered).build();
        });
    }

    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(ClustersPluginRegion.ReleaseGroupUpdate update, @Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("clusters/add", () -> {
            pluginRegion.add(httpRequest.getRemoteUser(), update);
            return Response.ok().build();
        });
    }

    @POST
    @Path("/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remove(ClustersPluginRegion.ReleaseGroupUpdate update, @Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("clusters/remove", () -> {
            pluginRegion.remove(httpRequest.getRemoteUser(), update);
            return Response.ok().build();
        });
    }

}
