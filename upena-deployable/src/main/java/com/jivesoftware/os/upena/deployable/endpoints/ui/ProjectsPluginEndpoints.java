package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.ProjectsPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ProjectsPluginRegion.ProjectsPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Singleton
@Path("/ui/projects")
public class ProjectsPluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;

    private final SoyService soyService;
    private final ProjectsPluginRegion pluginRegion;

    public ProjectsPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context ProjectsPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response projects(@Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("projects", (csrfToken) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(),
                csrfToken,
                pluginRegion,
                new ProjectsPluginRegionInput("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", false));
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
        @FormParam("localPath") @DefaultValue("") String localPath,
        @FormParam("scmUrl") @DefaultValue("") String scmUrl,
        @FormParam("branch") @DefaultValue("") String branch,
        @FormParam("pom") @DefaultValue("") String pom,
        @FormParam("goals") @DefaultValue("") String goals,
        @FormParam("profiles") @DefaultValue("") String profiles,
        @FormParam("properties") @DefaultValue("") String properties,
        @FormParam("mavenOpts") @DefaultValue("") String mavenOpts,
        @FormParam("mvnHome") @DefaultValue("") String mvnHome,
        @FormParam("oldCoordinate") @DefaultValue("") String oldCoordinate,
        @FormParam("newCoordinate") @DefaultValue("") String newCoordinate,
        @FormParam("action") @DefaultValue("") String action,
        @FormParam("refresh") @DefaultValue("true") boolean refresh) {
        return shiroRequestHelper.csrfCall(csrfToken, "projects/actions", (csrfToken1) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(),
                csrfToken1,
                pluginRegion,
                new ProjectsPluginRegionInput(key, name, description, localPath, scmUrl, branch, pom, goals, profiles, properties, mavenOpts, mvnHome,
                    oldCoordinate,
                    newCoordinate,
                    action,
                    refresh));
            return Response.ok(rendered);
        });
    }

    @GET
    @Path("/output/{key}")
    @Produces(MediaType.TEXT_HTML)
    public Response output(@PathParam("key") @DefaultValue("") String key,
        @QueryParam("refresh") @DefaultValue("true") boolean refresh,
        @Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("project/output", (csrfToken) -> {

            String rendered = soyService.wrapWithChrome(pluginRegion.getRootPath(), httpRequest.getRemoteUser(), csrfToken, pluginRegion.getTitle(), "Project Output",
                pluginRegion.output(key, refresh));

            return Response.ok(rendered);
        });
    }

    @GET
    @Path("/tail/{key}/{offset}")
    @Produces(MediaType.TEXT_HTML)
    public Response tail(@PathParam("key") @DefaultValue("") String key,
        @PathParam("offset") @DefaultValue("0") int offset,
        @Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("projects/tail", (csrfToken) -> {
            String rendered = pluginRegion.tail(key, offset);
            return Response.ok(rendered);
        });
    }

    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(ProjectsPluginRegion.ProjectUpdate update, @Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("projects/add", (csrfToken) -> {
            pluginRegion.add(httpRequest.getRemoteUser(), update);
            return Response.ok();
        });
    }

    @POST
    @Path("/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remove(ProjectsPluginRegion.ProjectUpdate update, @Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("projects/remove", (csrfToken) -> {
            pluginRegion.remove(httpRequest.getRemoteUser(), update);
            return Response.ok();
        });
    }

}
