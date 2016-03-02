package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.upena.deployable.region.BreakpointDumperPluginRegion;
import com.jivesoftware.os.upena.deployable.region.BreakpointDumperPluginRegion.BreakpointDumperPluginRegionInput;
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
@Path("/ui/breakpoint")
public class BreakpointDumperPluginEndpoints {

    private final SoyService soyService;
    private final BreakpointDumperPluginRegion pluginRegion;

    public BreakpointDumperPluginEndpoints(@Context SoyService soyService,
        @Context BreakpointDumperPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response breakpoint(@Context HttpServletRequest httpRequest) {
        String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
            new BreakpointDumperPluginRegionInput("", -1, "", -1, ""));
        return Response.ok(rendered).build();
    }

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response breakpoint(@Context HttpServletRequest httpRequest,
        @FormParam("host") @DefaultValue("") String host,
        @FormParam("port") @DefaultValue("-1") int port,
        @FormParam("className") @DefaultValue("") String className,
        @FormParam("lineNumber") @DefaultValue("-1") int lineNumber,
        @FormParam("action") @DefaultValue("") String action) {
        String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
            new BreakpointDumperPluginRegionInput(host, port, className, lineNumber, action));
        return Response.ok(rendered).build();
    }
}
