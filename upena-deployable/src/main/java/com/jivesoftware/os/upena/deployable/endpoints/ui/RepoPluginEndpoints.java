package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.RepoPluginRegion;
import com.jivesoftware.os.upena.deployable.region.RepoPluginRegion.RepoPluginRegionInput;
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
@Path("/ui/repo")
public class RepoPluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;

    private final SoyService soyService;
    private final RepoPluginRegion pluginRegion;
    private final ResponseHelper responseHelper = ResponseHelper.INSTANCE;

    public RepoPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context RepoPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response repo(@Context HttpServletRequest httpRequest) {
       return shiroRequestHelper.call("repo", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new RepoPluginRegionInput("", "", "", "", ""));
            return Response.ok(rendered).build();
        });
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("groupIdFilter") @DefaultValue("") String groupIdFilter,
        @FormParam("artifactIdFilter") @DefaultValue("") String artifactIdFilter,
        @FormParam("versionFilter") @DefaultValue("") String versionFilter,
        @FormParam("fileNameFilter") @DefaultValue("") String fileNameFilter,
        @FormParam("action") @DefaultValue("") String action) {
        return shiroRequestHelper.call("repo/action", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new RepoPluginRegionInput(groupIdFilter, artifactIdFilter, versionFilter, fileNameFilter, action));
            return Response.ok(rendered).build();
        });
    }
}
