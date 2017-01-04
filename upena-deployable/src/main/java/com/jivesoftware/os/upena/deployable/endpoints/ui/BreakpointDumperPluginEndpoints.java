package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

/**
 *
 */
@Singleton
@Path("/ui/breakpoint")
public class BreakpointDumperPluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;
    private final SoyService soyService;
    private final BreakpointDumperPluginRegion pluginRegion;

    public BreakpointDumperPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context BreakpointDumperPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response breakpoint(@Context HttpServletRequest httpRequest) {

        return shiroRequestHelper.call("breakpoint", (csrfToken) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken, pluginRegion,
                new BreakpointDumperPluginRegionInput("", "", "", "", "", "", "", "", "", "", Collections.emptyList(),
                    "", 0, 1, 0, "", "", "", 0, 0, "", ""));
            return Response.ok(rendered);
        });
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response breakpoint(@Context HttpServletRequest httpRequest,
        @FormParam("csrfToken") String csrfToken,
        @FormParam("instanceKey") @DefaultValue("") String instanceKey,
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
        @FormParam("port") @DefaultValue("0") int port,
        @FormParam("sessionId") @DefaultValue("0") long sessionId,
        @FormParam("connectionId") @DefaultValue("0") long connectionId,
        @FormParam("breakPointFieldName") @DefaultValue("") String breakPointFieldName,
        @FormParam("filter") @DefaultValue("") String filter,
        @FormParam("className") @DefaultValue("") String className,
        @FormParam("lineNumber") @DefaultValue("0") int lineNumber,
        @FormParam("maxVersions") @DefaultValue("1") int maxVersions,
        @FormParam("breakpoint") @DefaultValue("") String breakpoint,
        @FormParam("action") @DefaultValue("") String action) {

        return shiroRequestHelper.csrfCall(csrfToken, "breakpoint", (csrfToken1) -> {

            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(),
                csrfToken1,
                pluginRegion,
                new BreakpointDumperPluginRegionInput(instanceKey,
                    clusterKey,
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
                    sessionId,
                    connectionId,
                    breakPointFieldName,
                    filter,
                    className,
                    lineNumber,
                    maxVersions,
                    breakpoint,
                    action));
            return Response.ok(rendered);
        });
    }

    @Path("/dump/{sessionId}/{connectionId}")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response dumpId(@Context HttpServletRequest httpRequest,
        @PathParam("sessionId") @DefaultValue("") String sessionId,
        @PathParam("connectionId") @DefaultValue("") String connectionId) throws Exception {
        return shiroRequestHelper.call("breakpoint", (csrfToken) -> {
            return Response.ok(pluginRegion.dump(httpRequest.getRemoteUser(), sessionId, connectionId));
        });
    }
}
