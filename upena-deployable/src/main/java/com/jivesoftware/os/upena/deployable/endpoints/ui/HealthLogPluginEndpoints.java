package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.HealthLogPluginRegion;
import com.jivesoftware.os.upena.deployable.region.HealthLogPluginRegion.HealthLogPluginRegionInput;
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
import javax.ws.rs.core.SecurityContext;

/**
 *
 */
@Singleton
@Path("/ui/healthLog")
public class HealthLogPluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;
    private final SoyService soyService;
    private final HealthLogPluginRegion pluginRegion;

    public HealthLogPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context HealthLogPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response changelog(@Context SecurityContext sc, @Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("healthLog", (csrfToken) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken, pluginRegion,
                new HealthLogPluginRegionInput("", "", "", "", "", "", ""));
            return Response.ok(rendered);
        });
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context SecurityContext sc,
        @Context HttpServletRequest httpRequest,
        @FormParam("csrfToken") String csrfToken,
        @FormParam("who") @DefaultValue("") String who,
        @FormParam("what") @DefaultValue("") String what,
        @FormParam("when") @DefaultValue("") String when,
        @FormParam("where") @DefaultValue("") String where,
        @FormParam("why") @DefaultValue("") String why,
        @FormParam("how") @DefaultValue("") String how,
        @FormParam("action") @DefaultValue("") String action) {

        return shiroRequestHelper.csrfCall(csrfToken, "changeLog", (csrfToken1) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken1, pluginRegion,
                new HealthLogPluginRegionInput(who, what, when, where, why, how, action));
            return Response.ok(rendered);
        });

    }
}
