package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.ProbeJavaDeployablePluginRegion;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Singleton
@Path("/ui/java/deployable")
public class ProbeJavaDeployablePluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;
    private final SoyService soyService;
    private final ProbeJavaDeployablePluginRegion pluginRegion;

    public ProbeJavaDeployablePluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context ProbeJavaDeployablePluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @Path("/{instanceKey}")
    @GET()
    @Produces(MediaType.TEXT_HTML)
    public Response javaDeployableProbe(@PathParam("instanceKey") @DefaultValue("unspecified") String instanceKey,
        @Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("/ui/java/deployable", () -> {
            String rendered = soyService.renderNoChromePlugin(httpRequest.getRemoteUser(), pluginRegion,
                new ProbeJavaDeployablePluginRegion.ProbeJavaDeployablePluginRegionInput(instanceKey, ""));
            return Response.ok(rendered).build();
        });
    }

    @Path("/{instanceKey}")
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@PathParam("instanceKey") @DefaultValue("unspecified") String instanceKey,
        @Context HttpServletRequest httpRequest,
        @FormParam("action") @DefaultValue("") String action) {
        return shiroRequestHelper.call("/ui/java/deployable/action", () -> {
            String rendered = soyService.renderNoChromePlugin(httpRequest.getRemoteUser(), pluginRegion,
                new ProbeJavaDeployablePluginRegion.ProbeJavaDeployablePluginRegionInput(instanceKey, action));
            return Response.ok(rendered).build();
        });
    }
}
