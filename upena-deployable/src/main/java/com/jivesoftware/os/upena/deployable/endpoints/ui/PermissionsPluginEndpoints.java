package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.PermissionsPluginRegion;
import com.jivesoftware.os.upena.deployable.region.PermissionsPluginRegion.PermissionsPluginRegionInput;
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
@Path("/ui/permissions")
public class PermissionsPluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;
    private final SoyService soyService;
    private final PermissionsPluginRegion pluginRegion;

    public PermissionsPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context PermissionsPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response services(@Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("permissions", (csrfToken) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken, pluginRegion,
                new PermissionsPluginRegionInput("", "", "", ""));
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
        return shiroRequestHelper.csrfCall(csrfToken, "permission/action", (csrfToken1) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken1, pluginRegion,
                new PermissionsPluginRegionInput(key, name, description, action));
            return Response.ok(rendered);
        });
    }

}
