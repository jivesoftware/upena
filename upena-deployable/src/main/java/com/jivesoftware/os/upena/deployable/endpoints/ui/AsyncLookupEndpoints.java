package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.jivesoftware.os.upena.deployable.ShiroRequestHelper;
import com.jivesoftware.os.upena.deployable.lookup.AsyncLookupService;
import com.jivesoftware.os.upena.shared.ChaosStrategyKey;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Singleton
@Path("/ui/lookup")
public class AsyncLookupEndpoints {

    private final ShiroRequestHelper shiroRequestHelper;
    private final AsyncLookupService asyncLookupService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AsyncLookupEndpoints(@Context ShiroRequestHelper shiroRequestHelper,
        @Context AsyncLookupService asyncLookupService) {
        this.shiroRequestHelper = shiroRequestHelper;
        this.asyncLookupService = asyncLookupService;
    }

    @GET
    @Path("/clusters")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findClusters(
        @QueryParam("remoteHost") @DefaultValue("") String remoteHost,
        @QueryParam("remotePort") @DefaultValue("-1") int remotePort,
        @QueryParam("contains") String contains) {
        return shiroRequestHelper.call("lookup/clusters", () -> {
            Map<ClusterKey, TimestampedValue<Cluster>> clusters = asyncLookupService.findClusters(remoteHost, remotePort, contains);
            List<Map<String, String>> results = Lists.newArrayList();
            for (Map.Entry<ClusterKey, TimestampedValue<Cluster>> entry : clusters.entrySet()) {
                results.add(ImmutableMap.of("key", entry.getKey().getKey(), "name", entry.getValue().getValue().name));
            }
            Collections.sort(results, (Map<String, String> o1, Map<String, String> o2) -> {
                int c = o1.get("name").compareTo(o2.get("name"));
                if (c != 0) {
                    return c;
                }
                return o1.get("key").compareTo(o2.get("key"));
            });
            return Response.ok(objectMapper.writeValueAsString(results)).build();
        });
    }

    @GET
    @Path("/hosts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findHosts(@QueryParam("remoteHost") @DefaultValue("") String remoteHost,
        @QueryParam("remotePort") @DefaultValue("-1") int remotePort,
        @QueryParam("contains") String contains) {
        return shiroRequestHelper.call("lookup/hosts", () -> {
            Map<HostKey, TimestampedValue<Host>> hosts = asyncLookupService.findHosts(remoteHost, remotePort, contains);
            List<Map<String, String>> results = Lists.newArrayList();
            for (Map.Entry<HostKey, TimestampedValue<Host>> entry : hosts.entrySet()) {
                String name = entry.getValue().getValue().hostName + "/" + entry.getValue().getValue().name;
                if (entry.getValue().getValue().name.equals(entry.getValue().getValue().hostName)) {
                    name = entry.getValue().getValue().hostName;
                }
                results.add(ImmutableMap.of("key", entry.getKey().getKey(), "name", name));
            }
            Collections.sort(results, (Map<String, String> o1, Map<String, String> o2) -> {
                int c = o1.get("name").compareTo(o2.get("name"));
                if (c != 0) {
                    return c;
                }
                return o1.get("key").compareTo(o2.get("key"));
            });
            return Response.ok(objectMapper.writeValueAsString(results)).build();
        });
    }

    @GET
    @Path("/services")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findServices(@QueryParam("remoteHost") @DefaultValue("") String remoteHost,
        @QueryParam("remotePort") @DefaultValue("-1") int remotePort,
        @QueryParam("contains") String contains) {
        return shiroRequestHelper.call("lookup/services", () -> {
            Map<ServiceKey, TimestampedValue<Service>> services = asyncLookupService.findServices(remoteHost, remotePort, contains);
            List<Map<String, String>> results = Lists.newArrayList();
            for (Map.Entry<ServiceKey, TimestampedValue<Service>> entry : services.entrySet()) {
                results.add(ImmutableMap.of("key", entry.getKey().getKey(), "name", entry.getValue().getValue().name));
            }
            Collections.sort(results, (Map<String, String> o1, Map<String, String> o2) -> {
                int c = o1.get("name").compareTo(o2.get("name"));
                if (c != 0) {
                    return c;
                }
                return o1.get("key").compareTo(o2.get("key"));
            });
            return Response.ok(objectMapper.writeValueAsString(results)).build();
        });
    }

    @GET
    @Path("/releases")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findReleases(@QueryParam("remoteHost") @DefaultValue("") String remoteHost,
        @QueryParam("remotePort") @DefaultValue("-1") int remotePort,
        @QueryParam("contains") String contains) {
        return shiroRequestHelper.call("lookup/releases", () -> {
            Map<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> services = asyncLookupService.findReleases(remoteHost, remotePort, contains);
            List<Map<String, String>> results = Lists.newArrayList();
            for (Map.Entry<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> entry : services.entrySet()) {
                results.add(ImmutableMap.of("key", entry.getKey().getKey(), "name", entry.getValue().getValue().name));
            }
            Collections.sort(results, (Map<String, String> o1, Map<String, String> o2) -> {
                int c = o1.get("name").compareTo(o2.get("name"));
                if (c != 0) {
                    return c;
                }
                return o1.get("key").compareTo(o2.get("key"));
            });
            return Response.ok(objectMapper.writeValueAsString(results)).build();
        });
    }

    @GET
    @Path("/strategies")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findStrategies(@QueryParam("remoteHost") @DefaultValue("") String remoteHost,
        @QueryParam("remotePort") @DefaultValue("-1") int remotePort,
        @QueryParam("contains") String contains) {
        return shiroRequestHelper.call("lookup/strategies", () -> {
            List<Map<String, String>> results = Lists.newArrayList();
            for (ChaosStrategyKey s : ChaosStrategyKey.values()) {
                results.add(ImmutableMap.of("key", s.name(), "name", s.description));
            }

            results.sort((Map<String, String> o1, Map<String, String> o2) -> {
                int c = o1.get("name").compareTo(o2.get("name"));
                if (c != 0) {
                    return c;
                }
                return o1.get("key").compareTo(o2.get("key"));
            });

            return Response.ok(objectMapper.writeValueAsString(results)).build();
        });
    }

}
