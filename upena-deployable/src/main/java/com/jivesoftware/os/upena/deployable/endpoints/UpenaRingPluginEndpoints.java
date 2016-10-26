package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.UpenaRingPluginRegion;
import com.jivesoftware.os.upena.deployable.region.UpenaRingPluginRegion.UpenaRingPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 *
 */
@Singleton
@Path("/ui/ring")
public class UpenaRingPluginEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SoyService soyService;
    private final UpenaRingPluginRegion pluginRegion;

    public UpenaRingPluginEndpoints(@Context SoyService soyService,
        @Context UpenaRingPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response ring(@Context HttpServletRequest httpRequest) {
        try {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new UpenaRingPluginRegionInput("", "", ""));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("ring GET", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("host") @DefaultValue("") String host,
        @FormParam("port") @DefaultValue("") String port,
        @FormParam("action") @DefaultValue("") String action) {
        try {
            if (action.startsWith("export")) {
                String export = pluginRegion.doExport(httpRequest.getRemoteUser());
                return Response.ok(export, MediaType.APPLICATION_OCTET_STREAM_TYPE).build();
            } else {
                String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                    new UpenaRingPluginRegionInput(host, port, action));
                return Response.ok(rendered).build();
            }
        } catch (Exception e) {
            LOG.error("ring action POST", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

     @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importConfig(@Context HttpServletRequest httpRequest,
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
        try {
            String json = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
            LOG.info("importing:{}", json);
            pluginRegion.doImport(json, httpRequest.getRemoteUser());
            URI location = new URI("/ui/services");
            return Response.seeOther(location).build();
        } catch (Throwable t) {
            LOG.error("Failed to import", t);
            return Response.serverError().entity(t.getMessage()).build();
        }
    }
}
