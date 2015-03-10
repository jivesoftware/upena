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
package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.jive.utils.jaxrs.util.ResponseHelper;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.routing.shared.ConnectionDescriptorsRequest;
import com.jivesoftware.os.upena.routing.shared.ConnectionDescriptorsResponse;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptorsRequest;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptorsResponse;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterFilter;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostFilter;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceFilter;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.Tenant;
import com.jivesoftware.os.upena.shared.TenantFilter;
import com.jivesoftware.os.upena.shared.TenantKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
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

@Path("/upena")
public class UpenaRestEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final UpenaStore upenaStore;
    private final UpenaService upenaService;

    public UpenaRestEndpoints(@Context UpenaStore upenaStore, @Context UpenaService upenaService) {
        this.upenaStore = upenaStore;
        this.upenaService = upenaService;
    }

    @POST
    @Consumes("application/json")
    @Path("/cluster/add")
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
    @Path("/cluster/update")
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
    @Path("/cluster/get")
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
    @Path("/cluster/remove")
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
    @Path("/cluster/find")
    public Response findCluster(ClusterFilter filter) {
        try {
            Map<ClusterKey, TimestampedValue<Cluster>> found = upenaStore.clusters.find(filter);
            LOG.debug("filter:" + filter + " found:" + found.size() + " items.");
            return ResponseHelper.INSTANCE.jsonResponse(found);
        } catch (Exception x) {
            LOG.warn("Failed to find: " + filter, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to find filter:" + filter, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/host/add")
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
    @Path("/host/update")
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
    @Path("/host/get")
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
    @Path("/host/remove")
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
    @Path("/host/find")
    public Response findHost(HostFilter filter) {
        try {
            Map<HostKey, TimestampedValue<Host>> found = upenaStore.hosts.find(filter);
            LOG.debug("filter:" + filter + " found:" + found.size() + " items.");
            return ResponseHelper.INSTANCE.jsonResponse(found);
        } catch (Exception x) {
            LOG.warn("Failed to find: " + filter, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to find filter:" + filter, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/service/add")
    public Response addService(Service value) {
        try {
            ServiceKey serviceKey = upenaStore.services.update(null, value);
            return ResponseHelper.INSTANCE.jsonResponse(serviceKey);
        } catch (Exception x) {
            LOG.warn("Failed to add: " + value, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to update " + value, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/service/update")
    public Response updateService(Service value, @QueryParam(value = "key") String key) {
        try {
            ServiceKey serviceKey = upenaStore.services.update(new ServiceKey(key), value);
            return ResponseHelper.INSTANCE.jsonResponse(serviceKey);
        } catch (Exception x) {
            LOG.warn("Failed to update: " + value + " key:" + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to update " + value, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/service/get")
    public Response getService(ServiceKey key) {
        try {
            Service service = upenaStore.services.get(key);
            return ResponseHelper.INSTANCE.jsonResponse(service);
        } catch (Exception x) {
            LOG.warn("Failed to get: " + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to get " + key, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/service/remove")
    public Response removeService(ServiceKey key) {
        try {
            boolean removeService = upenaStore.services.remove(key);
            return ResponseHelper.INSTANCE.jsonResponse(removeService);
        } catch (Exception x) {
            LOG.warn("Failed to remove: " + key, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to remove " + key, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/service/find")
    public Response findService(ServiceFilter filter) {
        try {
            Map<ServiceKey, TimestampedValue<Service>> found = upenaStore.services.find(filter);
            LOG.debug("filter:" + filter + " found:" + found.size() + " items.");
            return ResponseHelper.INSTANCE.jsonResponse(found);
        } catch (Exception x) {
            LOG.warn("Failed to filter: " + filter, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to find filter:" + filter, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/releaseGroup/add")
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
    @Path("/releaseGroup/update")
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
    @Path("/releaseGroup/get")
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
    @Path("/releaseGroup/remove")
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
    @Path("/releaseGroup/find")
    public Response findReleaseGroup(ReleaseGroupFilter filter) {
        try {
            Map<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> found = upenaStore.releaseGroups.find(filter);
            LOG.debug("filter:" + filter + " found:" + found.size() + " items.");
            return ResponseHelper.INSTANCE.jsonResponse(found);
        } catch (Exception x) {
            LOG.warn("Failed to find: " + filter, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to find filter:" + filter, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/instance/add")
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
    @Path("/instance/update")
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
    @Path("/instance/get")
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
    @Path("/instance/remove")
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
    @Path("/instance/find")
    public Response findInstance(InstanceFilter filter) {
        try {
            Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
            LOG.info("filter:" + filter + " found:" + found.size() + " items.");
            return ResponseHelper.INSTANCE.jsonResponse(found);
        } catch (Exception x) {
            LOG.warn("Failed to find: " + filter, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to find filter:" + filter, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/tenant/add")
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
    @Path("/tenant/update")
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
    @Path("/tenant/get")
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
    @Path("/tenant/remove")
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
    @Path("/tenant/find")
    public Response findTenant(TenantFilter filter) {

        try {
            Map<TenantKey, TimestampedValue<Tenant>> found = upenaStore.tenants.find(filter);
            LOG.debug("filter:" + filter + " found:" + found.size() + " items.");
            return ResponseHelper.INSTANCE.jsonResponse(found);
        } catch (Exception x) {
            LOG.warn("Failed to find: " + filter, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to find filter:" + filter, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/request/connections")
    public Response requestConnections(ConnectionDescriptorsRequest connectionsRequest) {
        try {
            LOG.info("connectionsRequest:" + connectionsRequest);
            ConnectionDescriptorsResponse connectionDescriptorsResponse = upenaService.connectionRequest(connectionsRequest);
            LOG.info("connectionDescriptorsResponse:" + connectionDescriptorsResponse);
            return ResponseHelper.INSTANCE.jsonResponse(connectionDescriptorsResponse);
        } catch (Exception x) {
            LOG.warn("Failed to connectionsRequest:" + connectionsRequest, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to requestConnections for:" + connectionsRequest, x);
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/request/instanceDescriptors")
    public Response instanceDescriptors(InstanceDescriptorsRequest instanceDescriptorsRequest) {
        try {
            LOG.debug("instanceDescriptorsRequest:" + instanceDescriptorsRequest);
            InstanceDescriptorsResponse response = upenaService.instanceDescriptors(instanceDescriptorsRequest);
            LOG.debug("returning:" + response + " for instanceDescriptorsRequest:" + instanceDescriptorsRequest);
            return ResponseHelper.INSTANCE.jsonResponse(response);
        } catch (Exception x) {
            LOG.warn("Failed to instanceDescriptorsRequest:" + instanceDescriptorsRequest, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to instanceDescriptorsRequest for:" + instanceDescriptorsRequest, x);
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/mapTenantToRelease")
    public Response getInstanceConfig(@QueryParam("tenantId") String tenantId, @QueryParam("releaseId")  @DefaultValue("") String releaseId) {
        try {
            LOG.info("Attempting to map tenant to release: tenantId:" + tenantId + " releaseId:" + releaseId);
            ConcurrentNavigableMap<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> foundReleaseGroups = upenaStore.releaseGroups
                    .find(new ReleaseGroupFilter(releaseId, null, null, null, null, 0, 1000));
            Map.Entry<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> firstReleaseGroup = foundReleaseGroups.firstEntry();

            TenantFilter tenantFilter =  new TenantFilter(tenantId, null, 0, 1000);
            ConcurrentNavigableMap<TenantKey, TimestampedValue<Tenant>> foundTenants = upenaStore.tenants.find(tenantFilter);
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