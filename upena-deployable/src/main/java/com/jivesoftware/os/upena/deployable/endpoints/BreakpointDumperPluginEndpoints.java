package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.BreakpointDumperPluginRegion;
import com.jivesoftware.os.upena.deployable.region.BreakpointDumperPluginRegion.BreakpointDumperPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import java.util.Collections;
import java.util.List;
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

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

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

        try {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new BreakpointDumperPluginRegionInput("", "", "", "", "", "", "", "", "", Collections.emptyList(),
                    "", -1, "", -1, "", ""));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("breakpoint GET.", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response breakpoint(@Context HttpServletRequest httpRequest,
        @FormParam("clusterKey") @DefaultValue("") String clusterKey,
        @FormParam("cluster") @DefaultValue("") String cluster,
        @FormParam("hostKey") @DefaultValue("") String hostKey,
        @FormParam("host") @DefaultValue("") String host,
        @FormParam("serviceKey") @DefaultValue("") String serviceKey,
        @FormParam("service") @DefaultValue("") String service,
        @FormParam("instanceId") @DefaultValue("") String instanceId,
        @FormParam("releaseKey") @DefaultValue("") String releaseKey,
        @FormParam("release") @DefaultValue("") String release,
        @FormParam("instanceKeys") @DefaultValue("") List<String> instanceKeys,
        @FormParam("hostName") @DefaultValue("") String hostName,
        @FormParam("port") @DefaultValue("-1") int port,
        @FormParam("className") @DefaultValue("") String className,
        @FormParam("lineNumber") @DefaultValue("-1") int lineNumber,
        @FormParam("breakpoint") @DefaultValue("") String breakpoint,
        @FormParam("action") @DefaultValue("") String action) {
        try {

            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(),
                pluginRegion,
                new BreakpointDumperPluginRegionInput(clusterKey,
                    cluster,
                    hostKey,
                    host,
                    serviceKey,
                    service,
                    instanceId,
                    releaseKey,
                    release,
                    instanceKeys,
                    hostName,
                    port,
                    className,
                    lineNumber,
                    breakpoint,
                    action));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("breakpoint POST.", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
