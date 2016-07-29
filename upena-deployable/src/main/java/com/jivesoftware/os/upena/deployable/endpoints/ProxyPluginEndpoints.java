package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.UpenaProxy;
import com.jivesoftware.os.upena.deployable.region.ProxyPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ProxyPluginRegion.ProxyInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import java.net.URI;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Singleton
@Path("/ui/proxy")
public class ProxyPluginEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SoyService soyService;
    private final ProxyPluginRegion pluginRegion;

    public ProxyPluginEndpoints(@Context SoyService soyService,
        @Context ProxyPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response proxy(@Context HttpServletRequest httpRequest) {
        try {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion, new ProxyInput(-1, "", -1, ""));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("proxy GET", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("localPort") @DefaultValue("-1") int localPort,
        @FormParam("remoteHost") @DefaultValue("") String remoteHost,
        @FormParam("remotePort") @DefaultValue("-1") int remotePort,
        @FormParam("urlHost") @DefaultValue("") String urlHost,
        @FormParam("urlPort") @DefaultValue("-1") int urlPort,
        @FormParam("url") @DefaultValue("") String url,
        @FormParam("action") @DefaultValue("") String action) {
        try {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new ProxyInput(localPort, remoteHost, remotePort, action));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("proxy action POST", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/redirect")
    @Produces(MediaType.TEXT_HTML)
    public Response redirect(@Context HttpServletRequest httpRequest,
        @QueryParam("host") @DefaultValue("") String host,
        @QueryParam("port") @DefaultValue("-1") int port,
        @QueryParam("path") @DefaultValue("") String path) {
        try {

            UpenaProxy redirect = pluginRegion.redirect(host, port);
            return Response.temporaryRedirect(URI.create("http://" + httpRequest.getLocalAddr()
                + ":" + redirect.getLocalPort()
                + (path.startsWith("/") ? path : "/" + path))).build();
        } catch (Exception e) {
            LOG.error("proxy redirect GET", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
