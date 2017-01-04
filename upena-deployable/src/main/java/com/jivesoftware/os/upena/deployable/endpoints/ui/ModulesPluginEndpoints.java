package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.ModulesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ModulesPluginRegion.ModulesPluginRegionInput;
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
@Path("/ui/modules")
public class ModulesPluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;

    private final SoyService soyService;
    private final ModulesPluginRegion pluginRegion;

    public ModulesPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context ModulesPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response modules(@Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("modules", (csrfToken) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken, pluginRegion, new ModulesPluginRegionInput("", "", "", "", "", "", "", ""));
            return Response.ok(rendered);
        });
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response renderWithOptions(@Context HttpServletRequest httpRequest,
        @FormParam("csrfToken") String csrfToken,
        @FormParam("clusterKey") @DefaultValue("") String clusterKey,
        @FormParam("cluster") @DefaultValue("") String cluster,
        @FormParam("hostKey") @DefaultValue("") String hostKey,
        @FormParam("host") @DefaultValue("") String host,
        @FormParam("serviceKey") @DefaultValue("") String serviceKey,
        @FormParam("service") @DefaultValue("") String service,
        @FormParam("releaseKey") @DefaultValue("") String releaseKey,
        @FormParam("release") @DefaultValue("") String release) {
        return shiroRequestHelper.csrfCall(csrfToken, "modules/options", (csrfToken1) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken1, pluginRegion,
                new ModulesPluginRegionInput(clusterKey, cluster, hostKey, host, serviceKey, service, releaseKey, release));
            return Response.ok(rendered);
        });
    }
}
