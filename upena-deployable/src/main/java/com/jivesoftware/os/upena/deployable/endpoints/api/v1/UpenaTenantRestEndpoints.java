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
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.Tenant;
import com.jivesoftware.os.upena.shared.TenantFilter;
import com.jivesoftware.os.upena.shared.TenantKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import io.swagger.annotations.Api;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(value = "Upena Tenant CRUD")
@Path("/api/v1/upena/tenant")
public class UpenaTenantRestEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final UpenaStore upenaStore;
    
    public UpenaTenantRestEndpoints(@Context UpenaStore upenaStore) {
        this.upenaStore = upenaStore;
    }

    @POST
    @Consumes("application/json")
    @Path("/add")
    public Response addTenant(Tenant value) {
        try {
            TenantKey tenantKey = upenaStore.tenants.update(new TenantKey(value.tenantId), value);
            return ResponseHelper.INSTANCE.jsonResponse(tenantKey);
        } catch (Exception x) {
            LOG.warn("Failed to add: " + value, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to update " + value, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/update")
    public Response updateTenant(Tenant value, @QueryParam(value = "key") String key) {
        try {
            TenantKey tenantKey = upenaStore.tenants.update(new TenantKey(key), value);
            return ResponseHelper.INSTANCE.jsonResponse(tenantKey);
        } catch (Exception x) {
            LOG.warn("Failed to update: " + value + " key:" + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to update " + value, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/get")
    public Response getTenant(TenantKey key) {
        try {
            Tenant tenant = upenaStore.tenants.get(key);
            return ResponseHelper.INSTANCE.jsonResponse(tenant);
        } catch (Exception x) {
            LOG.warn("Failed to get: " + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to get " + key, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/remove")
    public Response removeTenant(TenantKey key) {
        try {
            boolean removeTenant = upenaStore.tenants.remove(key);
            return ResponseHelper.INSTANCE.jsonResponse(removeTenant);
        } catch (Exception x) {
            LOG.warn("Failed to remove: " + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to remove " + key, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/find")
    public Response findTenant(TenantFilter filter) {
        try {
            Map<TenantKey, TimestampedValue<Tenant>> found = upenaStore.tenants.find(false, filter);
            LOG.debug("filter:" + filter + " found:" + found.size() + " items.");
            return ResponseHelper.INSTANCE.jsonResponse(found);
        } catch (Exception x) {
            LOG.warn("Failed to find: " + filter, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to find filter:" + filter, x);
        }
    }

    

    @GET
    @Consumes("application/json")
    @Path("/mapTenantToRelease")
    public Response getInstanceConfig(@QueryParam("tenantId") String tenantId, @QueryParam("releaseId") @DefaultValue("") String releaseId) {
        try {
            LOG.info("Attempting to map tenant to release: tenantId:" + tenantId + " releaseId:" + releaseId);
            ConcurrentNavigableMap<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> foundReleaseGroups = upenaStore.releaseGroups
                .find(false, new ReleaseGroupFilter(releaseId, null, null, null, null, 0, 1000));
            Map.Entry<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> firstReleaseGroup = foundReleaseGroups.firstEntry();

            TenantFilter tenantFilter = new TenantFilter(tenantId, null, 0, 1000);
            ConcurrentNavigableMap<TenantKey, TimestampedValue<Tenant>> foundTenants = upenaStore.tenants.find(false, tenantFilter);
            Map.Entry<TenantKey, TimestampedValue<Tenant>> firstTenant = foundTenants.firstEntry();
            Tenant t = firstTenant.getValue().getValue();
            if (releaseId == null || releaseId.length() == 0) {
                upenaStore.tenants.update(new TenantKey(tenantId), new Tenant(t.tenantId, t.description, new HashMap<ServiceKey, ReleaseGroupKey>()));
            } else {
                upenaStore.tenants.update(new TenantKey(tenantId), new Tenant(t.tenantId, t.description, new HashMap<ServiceKey, ReleaseGroupKey>()));
            }

            LOG.info("Mapped tenant to release: tenantId:" + tenantId + " releaseId:" + releaseId);
            return Response.ok("", MediaType.TEXT_PLAIN).build();
        } catch (Exception x) {
            LOG.warn("Failed to map tenant to release: tenantId:" + tenantId + " releaseId:" + releaseId, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to map tenant to release: tenantId:" + tenantId + " releaseId:" + releaseId, x);
        }
    }
}
