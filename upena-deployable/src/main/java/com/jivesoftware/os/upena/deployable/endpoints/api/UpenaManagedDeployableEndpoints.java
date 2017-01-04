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
package com.jivesoftware.os.upena.deployable.endpoints.api;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelperUtils;
import com.jivesoftware.os.upena.deployable.HeaderDecoration;
import com.jivesoftware.os.upena.service.SessionStore;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceKey;
import java.nio.charset.StandardCharsets;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author jonathan.colt
 */
@Singleton
@Path("/upena/deployable")
public class UpenaManagedDeployableEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final HostKey hostKey;
    private final UpenaStore upenaStore;
    private final SessionStore sessionStore;


    public UpenaManagedDeployableEndpoints(@Context HostKey hostKey,
        @Context UpenaStore upenaStore,
        @Context SessionStore sessionStore) {
        this.hostKey = hostKey;
        this.upenaStore = upenaStore;
        this.sessionStore = sessionStore;
    }

    public static class LoopbackGet {

        public String path;

        public LoopbackGet() {
        }

        public LoopbackGet(String path) {
            this.path = path;
        }

    }

    @Path("/loopback/{instanceKey}")
    @POST
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response loopbackGet(@PathParam("instanceKey") @DefaultValue("unspecified") String instanceKey, LoopbackGet get) throws Exception {
        try {
            Instance instance = upenaStore.instances.get(new InstanceKey(instanceKey));
            if (instance == null) {
                LOG.warn("There is no instance for key:{}", instanceKey);
                return Response.serverError().build();
            } else if (!instance.hostKey.equals(hostKey)) {
                LOG.warn("This is the wrong host fot instance for key:{}", instanceKey);
                return Response.serverError().build();
            }

            Instance.Port manage = instance.ports.get("manage");
            HttpRequestHelper requestHelper = HttpRequestHelperUtils.buildRequestHelper(manage.sslEnabled, true, null, "localhost", manage.port);

            byte[] r = requestHelper.executeGet(get.path);
            return HeaderDecoration.decorate(Response.ok(r)).build();

        } catch (Exception x) {
            LOG.warn("HasUI proxy failed", x);
            return Response.serverError().build();
        }
    }

    @Path("/accessToken/{instanceKey}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response uiAccessToken(@PathParam("instanceKey") @DefaultValue("unspecified") String instanceKey) throws Exception {
        try {
            return HeaderDecoration.decorate(Response.ok(sessionStore.generateAccessToken(instanceKey).getBytes(StandardCharsets.UTF_8))).build();
        } catch (Exception x) {
            LOG.warn("UI access token failed", x);
            return Response.serverError().build();
        }
    }
}
