package com.jivesoftware.os.upena.deployable.endpoints;

import com.google.common.base.Optional;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.ConfigPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ConfigPluginRegion.ConfigPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import java.util.Map;
import javax.inject.Singleton;
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
@Path("/ui/config")
public class ConfigPluginEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SoyService soyService;
    private final ConfigPluginRegion pluginRegion;

    public ConfigPluginEndpoints(@Context SoyService soyService, @Context ConfigPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response services() {
        String rendered = soyService.renderPlugin(pluginRegion,
            Optional.of(new ConfigPluginRegionInput("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")));
        return Response.ok(rendered).build();
    }

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(
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
        @FormParam("value") @DefaultValue("") String value,
        @FormParam("overridden") @DefaultValue("") String overridden) {
        String rendered = soyService.renderPlugin(pluginRegion,
            Optional.of(new ConfigPluginRegionInput(
                    aClusterKey, aCluster, aHostKey, aHost, aServiceKey, aService, aInstance, aReleaseKey, aRelease, bClusterKey,
                    bCluster, bHostKey, bHost, bServiceKey, bService, bInstance, bReleaseKey, bRelease,
                    property, value, overridden)));
        return Response.ok(rendered).build();
    }

    @POST
    @Path("/modify")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyConfigs(ModifyRequest modifyRequest) {
        Map<String, Map<String, String>> propertyMap = modifyRequest.getUpdates();
        try {
            pluginRegion.modified(propertyMap);
            return Response.ok().build();
        } catch (Exception x) {
            LOG.error("Failed while setting properties:" + propertyMap);
            return Response.serverError().build();
        }
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

}
