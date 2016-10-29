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
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterFilter;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import io.swagger.annotations.Api;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Api(value = "Upena Cluster CRUD")
@Path("/upena/cluster")
public class UpenaClusterRestEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final UpenaStore upenaStore;
   
    public UpenaClusterRestEndpoints(@Context UpenaStore upenaStore) {
        this.upenaStore = upenaStore;
    }

    @POST
    @Consumes("application/json")
    @Path("/add")
    public Response addCluster(Cluster value) {
        try {
            LOG.debug("Attempting to add: " + value);
            ClusterKey key = upenaStore.clusters.update(null, value);
            return ResponseHelper.INSTANCE.jsonResponse(key);
        } catch (Exception x) {
            LOG.warn("Failed to add: " + value, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to update " + value, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/update")
    public Response updateCluster(Cluster value, @QueryParam(value = "key") String key) {
        try {
            LOG.debug("Attempting to update: " + value);
            ClusterKey clusterKey = upenaStore.clusters.update(new ClusterKey(key), value);
            return ResponseHelper.INSTANCE.jsonResponse(clusterKey);
        } catch (Exception x) {
            LOG.warn("Failed to update: " + value, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to update " + value, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/get")
    public Response getCluster(ClusterKey key) {
        try {
            Cluster cluster = upenaStore.clusters.get(key);
            return ResponseHelper.INSTANCE.jsonResponse(cluster);
        } catch (Exception x) {
            LOG.warn("Failed to get: " + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to remove " + key, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/remove")
    public Response removeCluster(ClusterKey key) {
        try {
            boolean removeCluster = upenaStore.clusters.remove(key);
            return ResponseHelper.INSTANCE.jsonResponse(removeCluster);
        } catch (Exception x) {
            LOG.warn("Failed to remove: " + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to remove " + key, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/find")
    public Response findCluster(ClusterFilter filter) {
        try {
            Map<ClusterKey, TimestampedValue<Cluster>> found = upenaStore.clusters.find(false, filter);
            LOG.debug("filter:" + filter + " found:" + found.size() + " items.");
            return ResponseHelper.INSTANCE.jsonResponse(found);
        } catch (Exception x) {
            LOG.warn("Failed to find: " + filter, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to find filter:" + filter, x);
        }
    }
}
