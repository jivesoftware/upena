package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.LoadBalancersPluginRegion;
import com.jivesoftware.os.upena.deployable.region.LoadBalancersPluginRegion.ListenerUpdate;
import com.jivesoftware.os.upena.deployable.region.LoadBalancersPluginRegion.LoadBalancersPluginRegionInput;
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
@Path("/ui/loadbalancers")
public class LoadBalancersPluginEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SoyService soyService;
    private final LoadBalancersPluginRegion pluginRegion;

    public LoadBalancersPluginEndpoints(@Context SoyService soyService, @Context LoadBalancersPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response clusters(@Context HttpServletRequest httpRequest) {
        try {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(),
                pluginRegion,
                new LoadBalancersPluginRegionInput("", "", "", ""));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("clusters GET.", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("key") @DefaultValue("") String key,
        @FormParam("name") @DefaultValue("") String name,
        @FormParam("description") @DefaultValue("") String description,
        @FormParam("action") @DefaultValue("") String action) {
        try {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new LoadBalancersPluginRegionInput(key, name, description, action));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("clusters action  POST.", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(ListenerUpdate update, @Context HttpServletRequest httpRequest) {
        try {
            pluginRegion.add(httpRequest.getRemoteUser(), update);
            return Response.ok().build();
        } catch (Exception x) {
            LOG.error("clusters add POST.", x);
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remove(ListenerUpdate update, @Context HttpServletRequest httpRequest) {
        try {
            pluginRegion.remove(httpRequest.getRemoteUser(), update);
            return Response.ok().build();
        } catch (Exception x) {
            LOG.error("clusters remove  POST.", x);
            return Response.serverError().build();
        }
    }

}
