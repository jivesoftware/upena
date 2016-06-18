package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
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

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SoyService soyService;
    private final ConnectivityPluginRegion pluginRegion;

    public ConnectivityPluginEndpoints(@Context SoyService soyService, @Context ConnectivityPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response render(@Context HttpServletRequest httpRequest) {
        try {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion, new ConnectivityPluginRegionInput("", "", "", "", "", "", "",
                "",
                new HashSet<>(Arrays.asList("linkCluster", "linkService", "linkInstance"))));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("render connectivity GET.", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
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
        @FormParam("linkType") @DefaultValue("linkCluster,linkService,linkInstance") List<String> linkType) {

        try {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new ConnectivityPluginRegionInput(clusterKey, cluster, hostKey, host, serviceKey, service, releaseKey, release, new HashSet<>(linkType)));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("render connectivity POST.", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/{instanceKey}")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response renderInstance(@Context HttpServletRequest httpRequest,
        @PathParam("instanceKey") @DefaultValue("") String instanceKey) {

        try {
            String rendered = soyService.wrapWithChrome("/ui/connectivity/" + instanceKey, httpRequest.getRemoteUser(),
                "foo", "Instance", pluginRegion.renderInstance(httpRequest.getRemoteUser(), instanceKey));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("render connectivity POST.", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
