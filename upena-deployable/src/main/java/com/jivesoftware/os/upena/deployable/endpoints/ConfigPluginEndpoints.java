package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.ConfigPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ConfigPluginRegion.ConfigPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 *
 */
@Singleton
@Path("/ui/config")
public class ConfigPluginEndpoints {

    
    private final ShiroRequestHelper shiroRequestHelper;
    private final SoyService soyService;
    private final ConfigPluginRegion pluginRegion;

    public ConfigPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context ConfigPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response config(@Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("config", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new ConfigPluginRegionInput("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", false, true, false,
                    "", -1, "", -1, ""));
            return Response.ok(rendered).build();
        });
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("aClusterKey") @DefaultValue("") String aClusterKey,
        @FormParam("aCluster") @DefaultValue("") String aCluster,
        @FormParam("aHostKey") @DefaultValue("") String aHostKey,
        @FormParam("aHost") @DefaultValue("") String aHost,
        @FormParam("aServiceKey") @DefaultValue("") String aServiceKey,
        @FormParam("aService") @DefaultValue("") String aService,
        @FormParam("aInstance") @DefaultValue("") String aInstance,
        @FormParam("aReleaseKey") @DefaultValue("") String aReleaseKey,
        @FormParam("aRelease") @DefaultValue("") String aRelease,
        @FormParam("bClusterKey") @DefaultValue("") String bClusterKey,
        @FormParam("bCluster") @DefaultValue("") String bCluster,
        @FormParam("bHostKey") @DefaultValue("") String bHostKey,
        @FormParam("bHost") @DefaultValue("") String bHost,
        @FormParam("bServiceKey") @DefaultValue("") String bServiceKey,
        @FormParam("bService") @DefaultValue("") String bService,
        @FormParam("bInstance") @DefaultValue("") String bInstance,
        @FormParam("bReleaseKey") @DefaultValue("") String bReleaseKey,
        @FormParam("bRelease") @DefaultValue("") String bRelease,
        @FormParam("property") @DefaultValue("") String property,
        @FormParam("healthProperty") @DefaultValue("") String healthProperty,
        @FormParam("value") @DefaultValue("") String value,
        @FormParam("overridden") @DefaultValue("false") boolean overridden,
        @FormParam("service") @DefaultValue("false") boolean service,
        @FormParam("health") @DefaultValue("false") boolean health,
        @FormParam("aRemoteConfigHost") @DefaultValue("") String aRemoteConfigHost,
        @FormParam("aRemoteConfigPort") @DefaultValue("-1") int aRemoteConfigPort,
        @FormParam("bRemoteConfigHost") @DefaultValue("") String bRemoteConfigHost,
        @FormParam("bRemoteConfigPort") @DefaultValue("-1") int bRemoteConfigPort,
        @FormParam("action") @DefaultValue("") String action) throws Exception {

        return shiroRequestHelper.call("config/action", () -> {

            ConfigPluginRegionInput configPluginRegionInput = new ConfigPluginRegionInput(
                aClusterKey, aCluster, aHostKey, aHost, aServiceKey, aService, aInstance, aReleaseKey, aRelease, bClusterKey,
                bCluster, bHostKey, bHost, bServiceKey, bService, bInstance, bReleaseKey, bRelease,
                property, healthProperty, value, overridden, service, health, aRemoteConfigHost, aRemoteConfigPort, bRemoteConfigHost, bRemoteConfigPort, action);
            if (action.equals("export")) {
                String export = pluginRegion.export(configPluginRegionInput);
                return Response.ok(export, MediaType.TEXT_PLAIN_TYPE).build();
            } else {
                String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                    configPluginRegionInput);
                return Response.ok(rendered, MediaType.TEXT_HTML).build();
            }
        });
    }

    @POST
    @Path("/modify")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyConfigs(@Context HttpServletRequest httpRequest, ModifyRequest modifyRequest) {
        return shiroRequestHelper.call("clusters/modify", () -> {
            Map<String, Map<String, String>> propertyMap = modifyRequest.getUpdates();
            pluginRegion.modified(httpRequest.getRemoteUser(), propertyMap);
            return Response.ok().build();
        });
    }

    public static class ModifyRequest {

        private Map<String, Map<String, String>> updates;

        public Map<String, Map<String, String>> getUpdates() {
            return updates;
        }

        public void setUpdates(Map<String, Map<String, String>> updates) {
            this.updates = updates;
        }
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadConfigFile(
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
        return shiroRequestHelper.call("clusters/upload", () -> {
            saveFile(fileInputStream);
            String output = "Your config was uploaded";
            return Response.status(200).entity(output).build();
        });
    }

    private void saveFile(InputStream uploadedInputStream) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
    }

}
