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
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import com.jivesoftware.os.upena.service.DiscoveredRoutes;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostFilter;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import io.swagger.annotations.Api;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Api(value = "Upena Host CRUD")
@Path("/upena/host")
public class UpenaHostRestEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final UpenaStore upenaStore;
    private final UpenaService upenaService;
    private final DiscoveredRoutes discoveredRoutes;

    public UpenaHostRestEndpoints(@Context UpenaStore upenaStore,
        @Context UpenaService upenaService,
        @Context DiscoveredRoutes discoveredRoutes) {
        this.upenaStore = upenaStore;
        this.upenaService = upenaService;
        this.discoveredRoutes = discoveredRoutes;
    }

    @POST
    @Consumes("application/json")
    @Path("/add")
    public Response addHost(Host value) {
        try {
            HostKey hostKey = upenaStore.hosts.update(null, value);
            return ResponseHelper.INSTANCE.jsonResponse(hostKey);
        } catch (Exception x) {
            LOG.warn("Failed to add: " + value, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to update " + value, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/update")
    public Response updateHost(Host value, @QueryParam(value = "key") String key) {
        try {
            HostKey hostKey = upenaStore.hosts.update(new HostKey(key), value);
            return ResponseHelper.INSTANCE.jsonResponse(hostKey);
        } catch (Exception x) {
            LOG.warn("Failed to update: " + value + " key:" + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to update " + value, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/get")
    public Response getHost(HostKey key) {
        try {
            Host host = upenaStore.hosts.get(key);
            return ResponseHelper.INSTANCE.jsonResponse(host);
        } catch (Exception x) {
            LOG.warn("Failed to get: " + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to get " + key, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/remove")
    public Response removeHost(HostKey key) {
        try {
            boolean removeHost = upenaStore.hosts.remove(key);
            return ResponseHelper.INSTANCE.jsonResponse(removeHost);
        } catch (Exception x) {
            LOG.warn("Failed to remove: " + key, x);

            return ResponseHelper.INSTANCE.errorResponse("Failed to remove " + key, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/find")
    public Response findHost(HostFilter filter) {
        try {
            Map<HostKey, TimestampedValue<Host>> found = upenaStore.hosts.find(false, filter);
            LOG.debug("filter:" + filter + " found:" + found.size() + " items.");
            return ResponseHelper.INSTANCE.jsonResponse(found);
        } catch (Exception x) {
            LOG.warn("Failed to find: " + filter, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to find filter:" + filter, x);
        }
    }

}
