package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.ClustersPluginRegion.ClustersPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterFilter;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 */
// soy.page.clustersPluginRegion
public class ClustersPluginRegion implements PageRegion<ClustersPluginRegionInput> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    public ClustersPluginRegion(String template,
        SoyRenderer renderer,
        UpenaStore upenaStore
    ) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
    }

    @Override
    public String getRootPath() {
        return "/ui/clusters";
    }

    public static class ClustersPluginRegionInput implements PluginInput {

        final String key;
        final String name;
        final String description;
        final String action;

        public ClustersPluginRegionInput(String key, String name, String description, String action) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.action = action;
        }

        @Override
        public String name() {
            return "Clusters";
        }

    }

    @Override
    public String render(String user, ClustersPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();

        try {

            Map<ServiceKey, String> serviceColor = ServiceColorUtil.serviceKeysColor(upenaStore);

            Map<String, String> filters = new HashMap<>();
            filters.put("name", input.name);
            filters.put("description", input.description);
            data.put("filters", filters);

            ClusterFilter filter = new ClusterFilter(null, null, 0, 10000);
            if (input.action != null) {
                if (input.action.equals("filter")) {
                    filter = new ClusterFilter(
                        input.name.isEmpty() ? null : input.name,
                        input.description.isEmpty() ? null : input.description,
                        0, 10000);
                    data.put("message", "Filtering: name.contains '" + input.name + "' description.contains '" + input.description + "'");
                } else if (input.action.equals("add")) {
                    filters.clear();
                    try {
                        Cluster newCluster = new Cluster(input.name, input.description,
                            new HashMap<>());
                        upenaStore.clusters.update(null, newCluster);
                        upenaStore.record(user, "added", System.currentTimeMillis(), "", "clusters-ui", newCluster.toString());

                        data.put("message", "Created Cluster:" + input.name);
                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to add Cluster:" + input.name + "\n" + trace);
                    }
                } else if (input.action.equals("update")) {
                    filters.clear();
                    try {
                        Cluster cluster = upenaStore.clusters.get(new ClusterKey(input.key));
                        if (cluster == null) {
                            data.put("message", "Couldn't update no existent cluster. Someone else likely just removed it since your last refresh.");
                        } else {
                            Cluster updatedCluster = new Cluster(input.name, input.description,
                                cluster.defaultReleaseGroups);
                            upenaStore.clusters.update(new ClusterKey(input.key), updatedCluster);
                            data.put("message", "Updated Cluster:" + input.name);
                            upenaStore.record(user, "updated", System.currentTimeMillis(), "", "clusters-ui", cluster.toString());
                        }
                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to add Cluster:" + input.name + "\n" + trace);
                    }
                } else if (input.action.equals("remove")) {
                    if (input.key.isEmpty()) {
                        data.put("message", "Failed to remove Cluster:" + input.name);
                    } else {
                        try {
                            ClusterKey clusterKey = new ClusterKey(input.key);
                            Cluster removing = upenaStore.clusters.get(clusterKey);
                            if (removing != null) {
                                upenaStore.clusters.remove(clusterKey);
                                upenaStore.record(user, "removed", System.currentTimeMillis(), "", "clusters-ui", removing.toString());
                            }
                        } catch (Exception x) {
                            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                            data.put("message", "Error while trying to remove Cluster:" + input.name + "\n" + trace);
                        }
                    }
                }
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            Map<ClusterKey, TimestampedValue<Cluster>> found = upenaStore.clusters.find(filter);
            for (Map.Entry<ClusterKey, TimestampedValue<Cluster>> entrySet : found.entrySet()) {
                ClusterKey key = entrySet.getKey();
                TimestampedValue<Cluster> timestampedValue = entrySet.getValue();
                Cluster value = timestampedValue.getValue();

                List<Map<String, String>> defaultReleaseGroups = new ArrayList<>();
                for (Entry<ServiceKey, ReleaseGroupKey> e : value.defaultReleaseGroups.entrySet()) {
                    Map<String, String> row = new HashMap<>();
                    row.put("serviceKey", e.getKey().getKey());
                    Service service = upenaStore.services.get(e.getKey());
                    if (service != null) {
                        row.put("serviceName", service.name);
                    } else {
                        row.put("serviceName", "missing");
                    }
                    row.put("releaseGroupKey", e.getValue().getKey());
                    ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(e.getValue());
                    if (releaseGroup != null) {
                        row.put("releaseGroupName", releaseGroup.name);
                    } else {
                        row.put("releaseGroupName", "missing");
                    }
                    defaultReleaseGroups.add(row);
                }

                InstanceFilter instanceFilter = new InstanceFilter(
                    key,
                    null,
                    null,
                    null,
                    null,
                    0, 10000);

                HashMultiset<ServiceKey> serviceKeyCount = HashMultiset.create();
                Map<InstanceKey, TimestampedValue<Instance>> instances = upenaStore.instances.find(instanceFilter);
                for (TimestampedValue<Instance> i : instances.values()) {
                    if (!i.getTombstoned()) {
                        serviceKeyCount.add(i.getValue().serviceKey);
                    }
                }

                List<Map<String, String>> instanceCounts = new ArrayList<>();
                for (ServiceKey sk : new HashSet<>(serviceKeyCount)) {
                    instanceCounts.add(ImmutableMap.of(
                        "count", String.valueOf(serviceKeyCount.count(sk)),
                        "color", serviceColor.get(sk)
                    ));
                }

                Map<String, Object> row = new HashMap<>();
                row.put("instanceCounts", instanceCounts);
                row.put("key", key.getKey());
                row.put("name", value.name);
                row.put("description", value.description);
                row.put("defaultReleaseGroups", defaultReleaseGroups);
                rows.add(row);
            }

            Collections.sort(rows, (Map<String, Object> o1, Map<String, Object> o2) -> {
                String clusterName1 = (String) o1.get("name");
                String clusterName2 = (String) o2.get("name");

                int c = clusterName1.compareTo(clusterName2);
                if (c != 0) {
                    return c;
                }
                return c;
            });

            data.put("clusters", rows);

        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    public void add(String user, ReleaseGroupUpdate releaseGroupUpdate) throws Exception {
        ClusterKey clusterKey = new ClusterKey(releaseGroupUpdate.clusterId);
        Cluster cluster = upenaStore.clusters.get(clusterKey);
        if (cluster != null) {
            cluster.defaultReleaseGroups.put(new ServiceKey(releaseGroupUpdate.serviceId), new ReleaseGroupKey(releaseGroupUpdate.releaseGroupId));
            upenaStore.clusters.update(clusterKey, cluster);
            upenaStore.record(user, "updated", System.currentTimeMillis(), "", "clusters-ui", cluster.toString());
        }
    }

    public void remove(String user, ReleaseGroupUpdate releaseGroupUpdate) throws Exception {
        ClusterKey clusterKey = new ClusterKey(releaseGroupUpdate.clusterId);
        Cluster cluster = upenaStore.clusters.get(clusterKey);
        if (cluster != null) {
            if (cluster.defaultReleaseGroups.remove(new ServiceKey(releaseGroupUpdate.serviceId)) != null) {
                upenaStore.clusters.update(clusterKey, cluster);
                upenaStore.record(user, "updated", System.currentTimeMillis(), "", "clusters-ui", cluster.toString());
            }
        }
    }

    public static class ReleaseGroupUpdate {

        public String clusterId;
        public String serviceId;
        public String releaseGroupId;

        public ReleaseGroupUpdate() {
        }

        public ReleaseGroupUpdate(String clusterId, String serviceId, String releaseGroupId) {
            this.clusterId = clusterId;
            this.serviceId = serviceId;
            this.releaseGroupId = releaseGroupId;
        }

    }

    @Override
    public String getTitle() {
        return "Upena Clusters";
    }

}
