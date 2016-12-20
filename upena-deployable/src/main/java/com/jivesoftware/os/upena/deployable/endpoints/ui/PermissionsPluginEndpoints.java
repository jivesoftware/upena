package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.ServicesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ServicesPluginRegion.ServicesPluginRegionInput;
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
@Path("/ui/services")
public class PermissionsPluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;
    private final SoyService soyService;
    private final ServicesPluginRegion pluginRegion;

    public PermissionsPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context ServicesPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response services(@Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("services", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new ServicesPluginRegionInput("", "", "", ""));
            return Response.ok(rendered).build();
        });
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("key") @DefaultValue("") String key,
        @FormParam("name") @DefaultValue("") String name,
        @FormParam("description") @DefaultValue("") String description,
        @FormParam("action") @DefaultValue("") String action) {
        return shiroRequestHelper.call("service/action", () -> {
            if (action.startsWith("export")) {
                String export = pluginRegion.doExport(new ServicesPluginRegionInput(key, name, description, "export"), httpRequest.getRemoteUser());
                return Response.ok(export, MediaType.APPLICATION_OCTET_STREAM_TYPE).build();
            } else {
                String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                    new ServicesPluginRegionInput(key, name, description, action));
                return Response.ok(rendered).build();
            }
        });
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importTopology(@Context HttpServletRequest httpRequest,
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
        return shiroRequestHelper.call("service/import", () -> {
            String json = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
            pluginRegion.doImport(json, httpRequest.getRemoteUser());
            URI location = new URI("/ui/services");
            return Response.seeOther(location).build();
        });
    }

}
