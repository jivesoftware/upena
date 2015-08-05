package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.upena.deployable.region.TopologyPluginRegion;
import com.jivesoftware.os.upena.deployable.region.TopologyPluginRegion.TopologyPluginRegionInput;
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
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Singleton
@Path("/ui/topology")
public class TopologyPluginEndpoints {

    private final SoyService soyService;
    private final TopologyPluginRegion pluginRegion;

    public TopologyPluginEndpoints(@Context SoyService soyService, @Context TopologyPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response render(@Context HttpServletRequest httpRequest) {
        String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,new TopologyPluginRegionInput("", "", "", "", "", "", "", "",
                    new HashSet<>(Arrays.asList("linkCluster", "linkService", "linkInstance")),
                    new HashSet<>(Arrays.asList("topology,connectivity"))));
        return Response.ok(rendered).build();
    }

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response renderWithOptions(@Context HttpServletRequest httpRequest,
        @FormParam("clusterKey") @DefaultValue("") String clusterKey,
        @FormParam("cluster") @DefaultValue("") String cluster,
        @FormParam("hostKey") @DefaultValue("") String hostKey,
        @FormParam("host") @DefaultValue("") String host,
        @FormParam("serviceKey") @DefaultValue("") String serviceKey,
        @FormParam("service") @DefaultValue("") String service,
        @FormParam("releaseKey") @DefaultValue("") String releaseKey,
        @FormParam("release") @DefaultValue("") String release,
        @FormParam("linkType") @DefaultValue("linkCluster,linkService,linkInstance") List<String> linkType,
        @FormParam("graphType") @DefaultValue("topology,connectivity") List<String> graphType) {

        String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
            new TopologyPluginRegionInput(clusterKey, cluster, hostKey, host, serviceKey, service, releaseKey, release, new HashSet<>(linkType), new HashSet<>(
                    graphType)));
        return Response.ok(rendered).build();
    }

}
