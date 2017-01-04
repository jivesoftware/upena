package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.UsersPluginRegion;
import com.jivesoftware.os.upena.deployable.region.UsersPluginRegion.PermissionUpdate;
import com.jivesoftware.os.upena.deployable.region.UsersPluginRegion.UsersPluginRegionInput;
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
@Path("/ui/users")
public class UsersPluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;
    private final SoyService soyService;
    private final UsersPluginRegion pluginRegion;

    public UsersPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context UsersPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response services(@Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("services", (csrfToken1) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken1, pluginRegion,
                new UsersPluginRegionInput("", "", "", ""));
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
        @FormParam("email") @DefaultValue("") String email,
        @FormParam("action") @DefaultValue("") String action) {
        return shiroRequestHelper.csrfCall(csrfToken,"service/action", (csrfToken1) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken1, pluginRegion,
                new UsersPluginRegionInput(key, name, email, action));
            return Response.ok(rendered);
        });
    }

    @POST
    @Path("/permission/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@Context HttpServletRequest httpRequest, PermissionUpdate update) {

        return shiroRequestHelper.call("users/permission/add", (csrfToken1) -> {
            pluginRegion.add(httpRequest.getRemoteUser(), update);
            return Response.ok();
        });
    }

    @POST
    @Path("/permission/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remove(@Context HttpServletRequest httpRequest, PermissionUpdate update) {
        return shiroRequestHelper.call("users/permission/remove", (csrfToken1) -> {
            pluginRegion.remove(httpRequest.getRemoteUser(), update);
            return Response.ok();
        });
    }


}
