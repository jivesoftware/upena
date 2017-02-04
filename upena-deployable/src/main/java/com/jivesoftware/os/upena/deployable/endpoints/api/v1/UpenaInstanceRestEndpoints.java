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
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import io.swagger.annotations.Api;
import java.util.Map;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Singleton
@Api(value = "Upena Instance CRUD")
@Path("/api/v1/upena/instance")
public class UpenaInstanceRestEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final UpenaStore upenaStore;

    public UpenaInstanceRestEndpoints(@Context UpenaStore upenaStore) {
        this.upenaStore = upenaStore;
    }

    @POST
    @Consumes("application/json")
    @Path("/add")
    public Response addInstance(Instance value) {
        try {
            LOG.info("Attempting to add: " + value);
            InstanceKey instanceKey = upenaStore.instances.update(null, value); // hack
            LOG.info("Added: " + value);
            return ResponseHelper.INSTANCE.jsonResponse(instanceKey);
        } catch (Exception x) {
            LOG.warn("Failed to add: " + value, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to update " + value, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/update")
    public Response updateInstance(Instance value, @QueryParam(value = "key") String key) {
        try {
            LOG.info("Attempting to update: " + value);
            InstanceKey instanceKey = upenaStore.instances.update(new InstanceKey(key), value);
            LOG.info("Updated: " + value);
            return ResponseHelper.INSTANCE.jsonResponse(instanceKey);
        } catch (Exception x) {
            LOG.warn("Failed to update: " + value + " key:" + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to update " + value, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/get")
    public Response getInstance(InstanceKey key) {
        try {
            Instance instance = upenaStore.instances.get(key);
            return ResponseHelper.INSTANCE.jsonResponse(instance);
        } catch (Exception x) {
            LOG.warn("Failed to get: " + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to get " + key, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/remove")
    public Response removeInstance(InstanceKey key) {
        try {
            boolean removeInstance = upenaStore.instances.remove(key);
            return ResponseHelper.INSTANCE.jsonResponse(removeInstance);
        } catch (Exception x) {
            LOG.warn("Failed to remove: " + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to remove " + key, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/find")
    public Response findInstance(InstanceFilter filter) {
        try {
            Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(false, filter);
            LOG.info("filter:" + filter + " found:" + found.size() + " items.");
            return ResponseHelper.INSTANCE.jsonResponse(found);
        } catch (Exception x) {
            LOG.warn("Failed to find: " + filter, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to find filter:" + filter, x);
        }
    }
}
