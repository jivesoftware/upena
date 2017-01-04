package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.ReleasesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ReleasesPluginRegion.PropertyUpdate;
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

    private final ShiroRequestHelper shiroRequestHelper;

    private final SoyService soyService;
    private final ReleasesPluginRegion pluginRegion;

    public ReleasesPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context ReleasesPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response releases(@Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("releases", (csrfToken) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(),
                csrfToken,
                pluginRegion,
                new ReleasesPluginRegionInput("", "", "", "", "", "", "", "", false, ""));
            return Response.ok(rendered);
        });
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("csrfToken") String csrfToken,
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

        return shiroRequestHelper.csrfCall(csrfToken, "releases/action", (csrfToken1) -> {
            if (action.startsWith("export")) {
                String export = pluginRegion.doExport(new ReleasesPluginRegionInput(key, name, description, rollback, version, upgrade, repository, email,
                    autoRelease, "export"), httpRequest.getRemoteUser());
                return Response.ok(export, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            } else {
                String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken1, pluginRegion,
                    new ReleasesPluginRegionInput(key, name, description, rollback, version, upgrade, repository, email, autoRelease, action));
                return Response.ok(rendered, MediaType.TEXT_HTML);
            }
        });
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/changelog")
    public Response changelog(@QueryParam("releaseKey") @DefaultValue("") String releaseKey) throws Exception {
        return shiroRequestHelper.call("releases/changeLog", (csrfToken1) -> {
            return Response.ok(pluginRegion.renderChangelog(releaseKey));
        });
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/scm")
    public Response scm(@QueryParam("releaseKey") @DefaultValue("") String releaseKey) throws Exception {
        return shiroRequestHelper.call("releases/scm", (csrfToken1) -> {
            return Response.ok(pluginRegion.renderScm(releaseKey));
        });
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importTopology(@Context HttpServletRequest httpRequest,
        @FormParam("csrfToken") String csrfToken,
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
        return shiroRequestHelper.csrfCall(csrfToken, "releases/import", (csrfToken1) -> {
            String json = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
            pluginRegion.doImport(json, httpRequest.getRemoteUser());
            URI location = new URI("/ui/releases");
            return Response.seeOther(location);
        });
    }

    @POST
    @Path("/property/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@Context HttpServletRequest httpRequest, PropertyUpdate update) {
       return shiroRequestHelper.call("releases/property/add", (csrfToken1) -> {
            pluginRegion.add(httpRequest.getRemoteUser(), update);
            return Response.ok();
        });
    }

    @POST
    @Path("/property/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remove(@Context HttpServletRequest httpRequest, PropertyUpdate update) {
        return shiroRequestHelper.call("releases/property/remove", (csrfToken1) -> {
            pluginRegion.remove(httpRequest.getRemoteUser(), update);
            return Response.ok();
        });
    }

}
