package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.ChangeLogPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ChangeLogPluginRegion.ChangeLogPluginRegionInput;
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
@Path("/ui/changeLog")
public class ChangeLogPluginEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SoyService soyService;
    private final ChangeLogPluginRegion pluginRegion;

    public ChangeLogPluginEndpoints(@Context SoyService soyService, @Context ChangeLogPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response changelog(@Context SecurityContext sc, @Context HttpServletRequest httpRequest) {
        try {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new ChangeLogPluginRegionInput("", "", "", "", "", "", ""));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("changelog GET.", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context SecurityContext sc, @Context HttpServletRequest httpRequest,
        @FormParam("who") @DefaultValue("") String who,
        @FormParam("what") @DefaultValue("") String what,
        @FormParam("when") @DefaultValue("") String when,
        @FormParam("where") @DefaultValue("") String where,
        @FormParam("why") @DefaultValue("") String why,
        @FormParam("how") @DefaultValue("") String how,
        @FormParam("action") @DefaultValue("") String action) {
        try {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new ChangeLogPluginRegionInput(who, what, when, where, why, how, action));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("changelog POST.", e);
            return Response.serverError().entity(e.getMessage()).build();
        }

    }
}
