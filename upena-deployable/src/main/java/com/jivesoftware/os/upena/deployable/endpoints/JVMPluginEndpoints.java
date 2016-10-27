package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.JVMPluginRegion;
import com.jivesoftware.os.upena.deployable.region.JVMPluginRegion.JVMPluginRegionInput;
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
@Path("/ui/jvm")
public class JVMPluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;
    private final SoyService soyService;
    private final JVMPluginRegion pluginRegion;

    public JVMPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context JVMPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response jmv(@Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("jvm", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new JVMPluginRegionInput("", "", "", ""));
            return Response.ok(rendered).build();
        });
    }

    @GET
    @Path("/memoryHisto/{instanceKey}")
    @Produces(MediaType.TEXT_HTML)
    public Response jmvMemoryHisto(@Context HttpServletRequest httpRequest, @PathParam("instanceKey") String instanceKey) {
        return shiroRequestHelper.call("jvmHisto", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new JVMPluginRegionInput("", "", instanceKey, "memoryHisto"));
            return Response.ok(rendered).build();
        });
    }

    @GET
    @Path("/threadDump/{instanceKey}")
    @Produces(MediaType.TEXT_HTML)
    public Response jmvThreadDump(@Context HttpServletRequest httpRequest, @PathParam("instanceKey") String instanceKey) {
        return shiroRequestHelper.call("jvmThreadDump", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new JVMPluginRegionInput("", "", instanceKey, "threadDump"));
            return Response.ok(rendered).build();
        });
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("host") @DefaultValue("") String host,
        @FormParam("port") @DefaultValue("") String port,
        @FormParam("action") @DefaultValue("") String action) {
        return shiroRequestHelper.call("jvm/actions", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new JVMPluginRegionInput(host, port, "", action));
            return Response.ok(rendered).build();
        });
    }

}
