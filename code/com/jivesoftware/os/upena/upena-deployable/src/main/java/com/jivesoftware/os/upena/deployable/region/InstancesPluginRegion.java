package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.amza.shared.RingHost;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
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
import com.jivesoftware.os.upena.shared.TimestampedValue;
import com.jivesoftware.os.upena.uba.service.UbaService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
// soy.page.instancesPluginRegion
public class InstancesPluginRegion implements PageRegion<Optional<InstancesPluginRegion.InstancesPluginRegionInput>> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final UpenaService upenaService;
    private final UbaService ubaService;
    private final RingHost ringHost;

    public InstancesPluginRegion(String template,
        SoyRenderer renderer,
        AmzaInstance amzaInstance,
        UpenaStore upenaStore,
        UpenaService upenaService,
        UbaService ubaService,
        RingHost ringHost) {
        this.template = template;
        this.renderer = renderer;
        this.amzaInstance = amzaInstance;
        this.upenaStore = upenaStore;
        this.upenaService = upenaService;
        this.ubaService = ubaService;
        this.ringHost = ringHost;
    }

    public static class InstancesPluginRegionInput {

        final String key;
        final String clusterKey;
        final String cluster;
        final String hostKey;
        final String host;
        final String serviceKey;
        final String service;
        final String id;
        final String releaseKey;
        final String release;
        final String action;

        public InstancesPluginRegionInput(String key, String clusterKey, String cluster, String hostKey, String host, String serviceKey, String service,
            String id, String releaseKey, String release, String action) {
            this.key = key;
            this.clusterKey = clusterKey;
            this.cluster = cluster;
            this.hostKey = hostKey;
            this.host = host;
            this.serviceKey = serviceKey;
            this.service = service;
            this.id = id;
            this.releaseKey = releaseKey;
            this.release = release;
            this.action = action;
        }

    }

    @Override
    public String render(Optional<InstancesPluginRegionInput> optionalInput) {
        Map<String, Object> data = Maps.newHashMap();

        try {
            if (optionalInput.isPresent()) {
                InstancesPluginRegionInput input = optionalInput.get();

                InstanceFilter filter = new InstanceFilter(null, null, null, null, null, 0, 10000);
                if (input.action.equals("restart")) {
                    Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
                    for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
                        InstanceKey key = entrySet.getKey();
                        TimestampedValue<Instance> timestampedValue = entrySet.getValue();
                        Instance value = timestampedValue.getValue();
                        ubaService.restartInstance(value.hostKey.getKey(), key.getKey());
                    }
                }

                List<Map<String, String>> rows = new ArrayList<>();

                Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
                for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
                    InstanceKey key = entrySet.getKey();
                    TimestampedValue<Instance> timestampedValue = entrySet.getValue();
                    Instance value = timestampedValue.getValue();
                    Host host = upenaStore.hosts.get(value.hostKey);

                    Map<String, String> map = new HashMap<>();
                    map.put("key", key.getKey());
                    map.put("cluster", upenaStore.clusters.get(value.clusterKey).name);
                    map.put("host", host.hostName + ":" + host.port);
                    map.put("host", upenaStore.hosts.get(value.hostKey).name);
                    map.put("service", upenaStore.services.get(value.serviceKey).name);
                    map.put("id", String.valueOf(value.instanceId));
                    map.put("release", upenaStore.releaseGroups.get(value.releaseGroupKey).name);

                    rows.add(map);
                }

                data.put("instances", rows);

                ClusterFilter clusterFilter = new ClusterFilter(null, null, 0, 10000);
                Map<ClusterKey, TimestampedValue<Cluster>> clustersFound = upenaStore.clusters.find(clusterFilter);
                List<String> clusters = Lists.newArrayList();
                for (Map.Entry<ClusterKey, TimestampedValue<Cluster>> entrySet : clustersFound.entrySet()) {
                    clusters.add(entrySet.getValue().getValue().name);
                }
                data.put("clusters", clusters);

                HostFilter hostsFilter = new HostFilter(null, null, null, null, null, 0, 10000);
                Map<HostKey, TimestampedValue<Host>> hostsFound = upenaStore.hosts.find(hostsFilter);
                List<String> hosts = Lists.newArrayList();
                for (Map.Entry<HostKey, TimestampedValue<Host>> entrySet : hostsFound.entrySet()) {
                    Host value = entrySet.getValue().getValue();
                    hosts.add(value.hostName + ":" + value.port);
                }
                data.put("hosts", hosts);

                ServiceFilter serviceFilter = new ServiceFilter(null, null, 0, 10000);
                Map<ServiceKey, TimestampedValue<Service>> servicesFound = upenaStore.services.find(serviceFilter);
                List<String> services = Lists.newArrayList();
                for (Map.Entry<ServiceKey, TimestampedValue<Service>> entrySet : servicesFound.entrySet()) {
                    services.add(entrySet.getValue().getValue().name);
                }
                data.put("services", services);

                ReleaseGroupFilter releasesFilter = new ReleaseGroupFilter(null, null, null, null, null, 0, 10000);
                Map<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> releasesFound = upenaStore.releaseGroups.find(releasesFilter);
                List<String> releases = Lists.newArrayList();
                for (Map.Entry<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> entrySet : releasesFound.entrySet()) {
                    releases.add(entrySet.getValue().getValue().name);
                }
                data.put("releases", releases);
            }
        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Upena Clusters";
    }
}
