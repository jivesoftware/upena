package com.jivesoftware.os.upena.deployable.endpoints;

import com.google.common.collect.Lists;
import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.LoadBalancersPluginRegion;
import com.jivesoftware.os.upena.deployable.region.LoadBalancersPluginRegion.LoadBalancersPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import java.util.Collections;
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
import org.apache.commons.lang.StringUtils;

/**
 *
 */
@Singleton
@Path("/ui/loadbalancers")
public class LoadBalancersPluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;

    private final SoyService soyService;
    private final LoadBalancersPluginRegion pluginRegion;

    public LoadBalancersPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context LoadBalancersPluginRegion pluginRegion) {
        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response loadBalancers(@Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("lb", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(),
                pluginRegion,
                new LoadBalancersPluginRegionInput("", "", "", "", 0, 0, Collections.emptyList(), "", "", "", Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyMap(), "", "", "", "", "", "", ""));
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
        @FormParam("clusterKey") @DefaultValue("") String clusterKey,
        @FormParam("cluster") @DefaultValue("") String cluster,
        @FormParam("serviceKey") @DefaultValue("") String serviceKey,
        @FormParam("service") @DefaultValue("") String service,
        @FormParam("releaseKey") @DefaultValue("") String releaseGroupKey,
        @FormParam("release") @DefaultValue("") String releaseGroup,
        @FormParam("action") @DefaultValue("") String action) {

        return shiroRequestHelper.call("lb/actions", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new LoadBalancersPluginRegionInput(key, name, description, null, -1, -1, null, null, null,
                    null, null, null, Collections.emptyMap(), clusterKey, cluster, serviceKey, service, releaseGroupKey, releaseGroup,
                    action));
            return Response.ok(rendered).build();
        });
    }

    @POST
    @Path("/config")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response config(@Context HttpServletRequest httpRequest,
        @FormParam("key") @DefaultValue("") String key,
        @FormParam("scheme") @DefaultValue("") String scheme,
        @FormParam("loadBalancerPort") @DefaultValue("-1") int loadBalancerPort,
        @FormParam("instancePort") @DefaultValue("-1") int instancePort,
        @FormParam("availabilityZones") @DefaultValue("") String availabilityZones,
        @FormParam("protocol") @DefaultValue("") String protocol,
        @FormParam("certificate") @DefaultValue("") String certificate,
        @FormParam("serviceProtocol") @DefaultValue("") String serviceProtocol,
        @FormParam("securityGroups") @DefaultValue("") String securityGroups,
        @FormParam("subnets") @DefaultValue("") String subnets) {

        return shiroRequestHelper.call("lb/config", () -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
                new LoadBalancersPluginRegionInput(key, null, null,
                    scheme,
                    loadBalancerPort,
                    instancePort,
                    sanitizedList(availabilityZones),
                    protocol,
                    certificate,
                    serviceProtocol,
                    sanitizedList(securityGroups),
                    sanitizedList(subnets),
                    Collections.emptyMap(),
                    null, null, null, null, null, null,
                    "update"));
            return Response.ok(rendered).build();
        });
    }

    private List<String> sanitizedList(String string) {
        if (string == null) {
            return Collections.emptyList();
        }
        if (string.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> sanitized = Lists.newArrayList();
        for (String s : string.split(",")) {
            s = s.trim();
            if (!StringUtils.isBlank(s)) {
                sanitized.add(s);
            }
        }
        return sanitized;
    }

}
