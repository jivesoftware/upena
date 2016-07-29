package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import com.jivesoftware.os.upena.deployable.region.ReleasesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ReleasesPluginRegion.ReleasesPluginRegionInput;
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
import javax.ws.rs.QueryParam;
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
@Path("/ui/releases")
public class ReleasesPluginEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SoyService soyService;
    private final ReleasesPluginRegion pluginRegion;

    private final ResponseHelper responseHelper = ResponseHelper.INSTANCE;

    public ReleasesPluginEndpoints(@Context SoyService soyService, @Context ReleasesPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response releases(@Context HttpServletRequest httpRequest) {
        try {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new ReleasesPluginRegionInput("", "", "", "", "", "", "", "", false, ""));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("releases GET", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("key") @DefaultValue("") String key,
        @FormParam("name") @DefaultValue("") String name,
        @FormParam("description") @DefaultValue("") String description,
        @FormParam("rollback") @DefaultValue("") String rollback,
        @FormParam("version") @DefaultValue("") String version,
        @FormParam("upgrade") @DefaultValue("") String upgrade,
        @FormParam("repository") @DefaultValue("") String repository,
        @FormParam("email") @DefaultValue("") String email,
        @FormParam("autoRelease") @DefaultValue("false") boolean autoRelease,
        @FormParam("action") @DefaultValue("") String action) {

        try {
            if (action.startsWith("export")) {
                String export = pluginRegion.doExport(new ReleasesPluginRegionInput(key, name, description, rollback, version, upgrade, repository, email,
                    autoRelease, "export"), httpRequest.getRemoteUser());
                return Response.ok(export, MediaType.APPLICATION_OCTET_STREAM_TYPE).build();
            } else {
                String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                    new ReleasesPluginRegionInput(key, name, description, rollback, version, upgrade, repository, email, autoRelease, action));
                return Response.ok(rendered, MediaType.TEXT_HTML).build();
            }
        } catch (Exception e) {
            LOG.error("action", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/changelog")
    public Response changelog(@QueryParam("releaseKey") @DefaultValue("") String releaseKey) throws Exception {
        try {
            return Response.ok(pluginRegion.renderChangelog(releaseKey)).build();
        } catch (Exception e) {
            LOG.error("changelog", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/scm")
    public Response scm(@QueryParam("releaseKey") @DefaultValue("") String releaseKey) throws Exception {
        try {
            return Response.ok(pluginRegion.renderScm(releaseKey)).build();
        } catch (Exception e) {
            LOG.error("scm", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importTopology(@Context HttpServletRequest httpRequest,
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
        try {
            String json = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
            LOG.info("importing:{}", json);
            pluginRegion.doImport(json, httpRequest.getRemoteUser());
            URI location = new URI("/ui/releases");
            return Response.seeOther(location).build();
        } catch (Throwable t) {
            LOG.error("Failed to import", t);
            return Response.serverError().entity(t.getMessage()).build();
        }
    }

}
