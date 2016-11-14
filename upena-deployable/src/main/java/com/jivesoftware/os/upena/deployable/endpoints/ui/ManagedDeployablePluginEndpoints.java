package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.ManagedDeployablePluginRegion;
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
import java.net.URI;

/**
 *
 */
@Singleton
@Path("/ui/deployable")
public class ManagedDeployablePluginEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final ShiroRequestHelper shiroRequestHelper;
    private final SoyService soyService;
    private final ManagedDeployablePluginRegion pluginRegion;

    public ManagedDeployablePluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context ManagedDeployablePluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @Path("/probe/{instanceKey}")
    @GET()
    @Produces(MediaType.TEXT_HTML)
    public Response javaDeployableProbe(@PathParam("instanceKey") @DefaultValue("unspecified") String instanceKey,
        @Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("/ui/deployable/probe", () -> {
            String rendered = soyService.renderNoChromePlugin(httpRequest.getRemoteUser(), pluginRegion,
                new ManagedDeployablePluginRegion.ManagedDeployablePluginRegionInput(instanceKey, ""));
            return Response.ok(rendered).build();
        });
    }

    @Path("/probe/{instanceKey}")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@PathParam("instanceKey") @DefaultValue("unspecified") String instanceKey,
        @Context HttpServletRequest httpRequest,
        @FormParam("action") @DefaultValue("") String action) {
        return shiroRequestHelper.call("/ui/deployable/probe/action", () -> {
            String rendered = soyService.renderNoChromePlugin(httpRequest.getRemoteUser(), pluginRegion,
                new ManagedDeployablePluginRegion.ManagedDeployablePluginRegionInput(instanceKey, action));
            return Response.ok(rendered).build();
        });
    }

    @Path("/embeddedProbe/{instanceKey}/{action}")
    @GET()
    @Produces(MediaType.TEXT_HTML)
    public Response embeddedProbe(@PathParam("instanceKey") @DefaultValue("unspecified") String instanceKey,
        @PathParam("action") @DefaultValue("unspecified") String action,
        @Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("/ui/deployable/embeddedProbe", () -> {
            return Response.ok(pluginRegion.render("",  new ManagedDeployablePluginRegion.ManagedDeployablePluginRegionInput(instanceKey, action))).build();
        });
    }


    @Path("/redirect/{instanceKey}")
    @GET()
    @Produces(MediaType.TEXT_HTML)
    public Response redirectToUI(@PathParam("instanceKey") @DefaultValue("unspecified") String instanceKey,
        @QueryParam("portName") @DefaultValue("unspecified") String portName,
        @QueryParam("path") @DefaultValue("unspecified") String uiPath,
        @Context HttpServletRequest httpRequest) {

        return shiroRequestHelper.call("/ui/deployable/redirect", () -> {
            URI uri = pluginRegion.redirectToUI(instanceKey, portName, uiPath);
            if (uri == null) {
                return Response.ok("Failed to redirect.").build();
            }
            return Response.temporaryRedirect(uri).build();
        });
    }

}
