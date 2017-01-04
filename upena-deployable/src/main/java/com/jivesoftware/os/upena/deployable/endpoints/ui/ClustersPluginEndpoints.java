package com.jivesoftware.os.upena.deployable.endpoints.ui;

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
        return shiroRequestHelper.call("clusters", (csrfToken) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(),
                csrfToken,
                pluginRegion,
                new ClustersPluginRegionInput("", "", "", ""));
            return Response.ok(rendered);
        });
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("csrfToken") String csrfToken,
        @FormParam("key") @DefaultValue("") String key,
        @FormParam("name") @DefaultValue("") String name,
        @FormParam("description") @DefaultValue("") String description,
        @FormParam("action") @DefaultValue("") String action) {

        return shiroRequestHelper.csrfCall(csrfToken, "clusters/action", (csrfToken1) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken1, pluginRegion,
                new ClustersPluginRegionInput(key, name, description, action));
            return Response.ok(rendered);
        });
    }

    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(ClustersPluginRegion.ReleaseGroupUpdate update, @Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("clusters/add", (csrfToken) -> {
            pluginRegion.add(httpRequest.getRemoteUser(), update);
            return Response.ok();
        });
    }

    @POST
    @Path("/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remove(ClustersPluginRegion.ReleaseGroupUpdate update, @Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("clusters/remove", (csrfToken) -> {
            pluginRegion.remove(httpRequest.getRemoteUser(), update);
            return Response.ok();
        });
    }

}
