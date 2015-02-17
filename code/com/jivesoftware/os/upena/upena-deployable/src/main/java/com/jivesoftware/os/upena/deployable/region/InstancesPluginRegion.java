package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.amza.shared.RingHost;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
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

                List<Map<String, Object>> rows = new ArrayList<>();

                Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
                for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
                    InstanceKey key = entrySet.getKey();
                    TimestampedValue<Instance> timestampedValue = entrySet.getValue();
                    Instance value = timestampedValue.getValue();
                    Host host = upenaStore.hosts.get(value.hostKey);

                    Map<String, Object> map = new HashMap<>();
                    map.put("key", key.getKey());
                    map.put("cluster", ImmutableMap.of(
                        "key", value.clusterKey.getKey(),
                        "name", upenaStore.clusters.get(value.clusterKey).name));
                    map.put("host", ImmutableMap.of(
                        "key", value.hostKey.getKey(),
                        "name", upenaStore.hosts.get(value.hostKey).name));
                    map.put("service", ImmutableMap.of(
                        "key", value.serviceKey.getKey(),
                        "name", upenaStore.services.get(value.serviceKey).name));
                    map.put("id", String.valueOf(value.instanceId));
                    map.put("release", ImmutableMap.of(
                        "key", value.releaseGroupKey.getKey(),
                        "name", upenaStore.releaseGroups.get(value.releaseGroupKey).name));

                    rows.add(map);
                }

                data.put("instances", rows);
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
