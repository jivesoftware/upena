package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.InstancesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.InstancesPluginRegion.InstancesPluginRegionInput;
import com.jivesoftware.os.upena.deployable.region.InstancesPluginRegion.PortUpdate;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import java.net.URI;
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
@Path("/ui/instances")
public class InstancesPluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;
    private final SoyService soyService;
    private final InstancesPluginRegion pluginRegion;

    public InstancesPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context InstancesPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response instances(@Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("instances", (csrfToken) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken, pluginRegion,
                new InstancesPluginRegionInput("", "", "", "", "", "", "", "", "", "", false, false, false, "", "", ""));
            return Response.ok(rendered);
        });
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("csrfToken") String csrfToken,
        @FormParam("key") @DefaultValue("") String key,
        @FormParam("clusterKey") @DefaultValue("") String clusterKey,
        @FormParam("cluster") @DefaultValue("") String cluster,
        @FormParam("hostKey") @DefaultValue("") String hostKey,
        @FormParam("host") @DefaultValue("") String host,
        @FormParam("serviceKey") @DefaultValue("") String serviceKey,
        @FormParam("service") @DefaultValue("") String service,
        @FormParam("instanceId") @DefaultValue("") String instanceId,
        @FormParam("releaseKey") @DefaultValue("") String releaseKey,
        @FormParam("release") @DefaultValue("") String release,
        @FormParam("sslEnabled") @DefaultValue("false") boolean sslEnabled,
        @FormParam("serviceAuthEnabled") @DefaultValue("false") boolean serviceAuthEnabled,
        @FormParam("enabled") @DefaultValue("false") boolean enabled,
        @FormParam("intervalUnits") @DefaultValue("SECONDS") String intervalUnits,
        @FormParam("interval") @DefaultValue("30") String interval,
        @FormParam("action") @DefaultValue("") String action) {

        return shiroRequestHelper.csrfCall(csrfToken, "instance/actions", (csrfToken1) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken1, pluginRegion,
                new InstancesPluginRegionInput(key,
                    clusterKey,
                    cluster,
                    hostKey,
                    host,
                    serviceKey,
                    service,
                    instanceId,
                    releaseKey,
                    release,
                    sslEnabled,
                    serviceAuthEnabled,
                    enabled,
                    intervalUnits,
                    interval,
                    action));
            return Response.ok(rendered);
        });
    }

    @POST
    @Path("/add")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response add(@Context HttpServletRequest httpRequest,
        @FormParam("csrfToken") String csrfToken,
        @FormParam("clusterKey") @DefaultValue("") String clusterKey,
        @FormParam("hostKeys") @DefaultValue("") List<String> hostKeys,
        @FormParam("serviceKey") @DefaultValue("") String serviceKey,
        @FormParam("releaseKey") @DefaultValue("") String releaseKey) {

        return shiroRequestHelper.csrfCall(csrfToken, "instance/add", (csrfToken1) -> {
            String result = pluginRegion.add(httpRequest.getRemoteUser(), clusterKey, hostKeys, serviceKey, releaseKey);
            URI location = new URI("/ui/health");
            return Response.seeOther(location);
        });
    }

    @POST
    @Path("/ports/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@Context HttpServletRequest httpRequest, PortUpdate update) {

        return shiroRequestHelper.call("instance/ports/add", (csrfToken) -> {
            pluginRegion.add(httpRequest.getRemoteUser(), update);
            return Response.ok();
        });
    }

    @POST
    @Path("/ports/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remove(@Context HttpServletRequest httpRequest, PortUpdate update) {
        return shiroRequestHelper.call("instance/ports/remove", (csrfToken) -> {
            pluginRegion.remove(httpRequest.getRemoteUser(), update);
            return Response.ok();
        });
    }

}
