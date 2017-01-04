/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.region.MonkeyPluginRegion;
import com.jivesoftware.os.upena.deployable.region.MonkeyPluginRegion.MonkeyPluginRegionInput;
import com.jivesoftware.os.upena.deployable.region.MonkeyPluginRegion.MonkeyPropertyUpdate;
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

@Singleton
@Path("/ui/chaos")
public class MonkeyPluginEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;

    private final SoyService soyService;
    private final MonkeyPluginRegion pluginRegion;

    public MonkeyPluginEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context SoyService soyService,
        @Context MonkeyPluginRegion pluginRegion) {

        this.shiroRequestHelper = shiroRequestHelper;
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response monkeys(@Context HttpServletRequest httpRequest) {
        return shiroRequestHelper.call("monkeys", (csrfToken) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken, pluginRegion,
                new MonkeyPluginRegionInput("", false, "", "", "", "", "", "", "", "", ""));
            return Response.ok(rendered);
        });
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("csrfToken") String csrfToken,
        @FormParam("key") @DefaultValue("") String key,
        @FormParam("enabled") @DefaultValue("false") boolean enabled,
        @FormParam("clusterKey") @DefaultValue("") String clusterKey,
        @FormParam("cluster") @DefaultValue("") String cluster,
        @FormParam("hostKey") @DefaultValue("") String hostKey,
        @FormParam("host") @DefaultValue("") String host,
        @FormParam("serviceKey") @DefaultValue("") String serviceKey,
        @FormParam("service") @DefaultValue("") String service,
        @FormParam("strategyKey") @DefaultValue("") String strategyKey,
        @FormParam("strategy") @DefaultValue("") String strategy,
        @FormParam("action") @DefaultValue("") String action) {
        return shiroRequestHelper.csrfCall(csrfToken, "monkey/actions", (csrfToken1) -> {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), csrfToken1, pluginRegion,
                new MonkeyPluginRegionInput(key, enabled, clusterKey, cluster, hostKey,
                    host, serviceKey, service, strategyKey, strategy, action));
            return Response.ok(rendered);
        });
    }

    @POST
    @Path("/property/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@Context HttpServletRequest httpRequest, MonkeyPropertyUpdate update) {
        return shiroRequestHelper.call("monkeys/add", (csrfToken) -> {
            pluginRegion.add(update);
            return Response.ok();
        });
    }

    @POST
    @Path("/property/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remove(@Context HttpServletRequest httpRequest, MonkeyPropertyUpdate update) {
        return shiroRequestHelper.call("monkeys/remove", (csrfToken) -> {
            pluginRegion.remove(update);
            return Response.ok();
        });
    }

}
