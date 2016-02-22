package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
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

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SoyService soyService;
    private final ProjectsPluginRegion pluginRegion;

    public ProjectsPluginEndpoints(@Context SoyService soyService, @Context ProjectsPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response projects(@Context HttpServletRequest httpRequest) {
        String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(),
            pluginRegion,
            new ProjectsPluginRegionInput("", "", "", "", "", "", "", "", "", "", false));
        return Response.ok(rendered).build();
    }

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("key") @DefaultValue("") String key,
        @FormParam("name") @DefaultValue("") String name,
        @FormParam("description") @DefaultValue("") String description,
        @FormParam("localPath") @DefaultValue("") String localPath,
        @FormParam("scmUrl") @DefaultValue("") String scmUrl,
        @FormParam("branch") @DefaultValue("") String branch,
        @FormParam("pom") @DefaultValue("") String pom,
        @FormParam("goals") @DefaultValue("") String goals,
        @FormParam("mvnHome") @DefaultValue("") String mvnHome,
        @FormParam("action") @DefaultValue("") String action,
        @FormParam("refresh") @DefaultValue("true") boolean refresh) {

        String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
            new ProjectsPluginRegionInput(key, name, description, localPath, scmUrl, branch, pom, goals, mvnHome, action, refresh));
        return Response.ok(rendered).build();
    }

    @GET
    @Path("/output/{key}")
    @Produces(MediaType.TEXT_HTML)
    public Response output(@PathParam("key") @DefaultValue("") String key,
        @QueryParam("refresh") @DefaultValue("true") boolean refresh,
        @Context HttpServletRequest httpRequest) {
        try {

            String rendered = soyService.wrapWithChrome(pluginRegion.getRootPath(), httpRequest.getRemoteUser(), pluginRegion.getTitle(), "Project Output",
                pluginRegion.output(key, refresh));

            return Response.ok(rendered).build();
        } catch (Exception x) {
            LOG.error("Failed to generate output for:" + key, x);
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/tail/{key}/{offset}")
    @Produces(MediaType.TEXT_HTML)
    public Response tail(@PathParam("key") @DefaultValue("") String key,
        @PathParam("offset") @DefaultValue("0") int offset,
        @Context HttpServletRequest httpRequest) {
        try {

            String rendered = pluginRegion.tail(key, offset);
            return Response.ok(rendered).build();
        } catch (Exception x) {
            LOG.error("Failed to generate output for:" + key, x);
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(ProjectsPluginRegion.ProjectUpdate update, @Context HttpServletRequest httpRequest) {
        try {
            pluginRegion.add(httpRequest.getRemoteUser(), update);
            return Response.ok().build();
        } catch (Exception x) {
            LOG.error("Failed to add to default release groups for:" + update, x);
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remove(ProjectsPluginRegion.ProjectUpdate update, @Context HttpServletRequest httpRequest) {
        try {
            pluginRegion.remove(httpRequest.getRemoteUser(), update);
            return Response.ok().build();
        } catch (Exception x) {
            LOG.error("Failed to remove to default release groups for:" + update, x);
            return Response.serverError().build();
        }
    }

}
