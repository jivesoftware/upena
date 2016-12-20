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
        return shiroRequestHelper.call("services", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new UsersPluginRegionInput("", "", "", ""));
            return Response.ok(rendered).build();
        });
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("key") @DefaultValue("") String key,
        @FormParam("name") @DefaultValue("") String name,
        @FormParam("email") @DefaultValue("") String email,
        @FormParam("action") @DefaultValue("") String action) {
        return shiroRequestHelper.call("service/action", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new UsersPluginRegionInput(key, name, email, action));
            return Response.ok(rendered).build();
        });
    }

    @POST
    @Path("/permission/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@Context HttpServletRequest httpRequest, PermissionUpdate update) {

        return shiroRequestHelper.call("users/permission/add", () -> {
            pluginRegion.add(httpRequest.getRemoteUser(), update);
            return Response.ok().build();
        });
    }

    @POST
    @Path("/permission/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remove(@Context HttpServletRequest httpRequest, PermissionUpdate update) {
        return shiroRequestHelper.call("users/permission/remove", () -> {
            pluginRegion.remove(httpRequest.getRemoteUser(), update);
            return Response.ok().build();
        });
    }


}
