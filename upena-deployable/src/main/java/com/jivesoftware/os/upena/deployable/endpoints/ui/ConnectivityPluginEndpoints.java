package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.ConnectivityPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ConnectivityPluginRegion.ConnectivityPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
@Path("/ui/connectivity")
public class ConnectivityPluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;

    private final SoyService soyService;
    private final ConnectivityPluginRegion pluginRegion;

    public ConnectivityPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context ConnectivityPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response render(@Context HttpServletRequest httpRequest) {
       return shiroRequestHelper.call("connectivity", (csrfToken) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken, pluginRegion, new ConnectivityPluginRegionInput("", "", "", "", "", "", "",
                "",
                new HashSet<>(Arrays.asList("linkCluster", "linkService", "linkInstance"))));
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
        @FormParam("release") @DefaultValue("") String release,
        @FormParam("linkType") @DefaultValue("linkCluster,linkService,linkInstance") List<String> linkType) {

        return shiroRequestHelper.csrfCall(csrfToken, "connectivity", (csrfToken1) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken1, pluginRegion,
                new ConnectivityPluginRegionInput(clusterKey, cluster, hostKey, host, serviceKey, service, releaseKey, release, new HashSet<>(linkType)));
            return Response.ok(rendered);
        });
    }

    @GET
    @Path("/{instanceKey}")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response renderInstance(@Context HttpServletRequest httpRequest,
        @PathParam("instanceKey") @DefaultValue("") String instanceKey) {

        return shiroRequestHelper.call("connectivity/instance", (csrfToken) -> {
            String rendered = soyService.wrapWithChrome("/ui/connectivity/" + instanceKey, httpRequest.getRemoteUser(), csrfToken,
                "foo", "Instance", pluginRegion.renderInstance(httpRequest.getRemoteUser(), instanceKey));
            return Response.ok(rendered);
        });
    }

}
