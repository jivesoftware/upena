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
package com.jivesoftware.os.upena.deployable.endpoints.api.v1;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import io.swagger.annotations.Api;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Api(value = "Upena Release CRUD")
@Path("/api/v1/upena/releaseGroup")
public class UpenaReleaseRestEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final UpenaStore upenaStore;
    
    public UpenaReleaseRestEndpoints(@Context UpenaStore upenaStore) {
        this.upenaStore = upenaStore;
    }


    @POST
    @Consumes("application/json")
    @Path("/add")
    public Response addReleaseGroup(ReleaseGroup value) {
        try {
            ReleaseGroupKey releaseGroupKey = upenaStore.releaseGroups.update(null, value);
            LOG.debug("add:" + value);
            return ResponseHelper.INSTANCE.jsonResponse(releaseGroupKey);
        } catch (Exception x) {
            LOG.warn("Failed to add: " + value, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to update " + value, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/update")
    public Response updateReleaseGroup(ReleaseGroup value, @QueryParam(value = "key") String key) {
        try {
            ReleaseGroupKey releaseGroupKey = upenaStore.releaseGroups.update(new ReleaseGroupKey(key), value);
            LOG.debug("update:" + key + " " + value);
            return ResponseHelper.INSTANCE.jsonResponse(releaseGroupKey);
        } catch (Exception x) {
            LOG.warn("Failed to update: " + value + " key:" + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to update " + value, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/get")
    public Response getReleaseGroup(ReleaseGroupKey key) {
        try {
            ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(key);
            return ResponseHelper.INSTANCE.jsonResponse(releaseGroup);
        } catch (Exception x) {
            LOG.warn("Failed to get: " + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to get " + key, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/remove")
    public Response removeReleaseGroup(ReleaseGroupKey key) {
        try {
            boolean remove = upenaStore.releaseGroups.remove(key);
            return ResponseHelper.INSTANCE.jsonResponse(remove);
        } catch (Exception x) {
            LOG.warn("Failed to remove: " + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to remove " + key, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/find")
    public Response findReleaseGroup(ReleaseGroupFilter filter) {
        try {
            Map<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> found = upenaStore.releaseGroups.find(false, filter);
            LOG.debug("filter:" + filter + " found:" + found.size() + " items.");
            return ResponseHelper.INSTANCE.jsonResponse(found);
        } catch (Exception x) {
            LOG.warn("Failed to find: " + filter, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to find filter:" + filter, x);
        }
    }

}
