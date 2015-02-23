package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.amza.shared.RingHost;
import com.jivesoftware.os.jive.utils.ordered.id.JiveEpochTimestampProvider;
import com.jivesoftware.os.jive.utils.ordered.id.SnowflakeIdPacker;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import com.jivesoftware.os.upena.uba.service.UbaService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.time.DurationFormatUtils;

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
        final String instanceId;
        final String releaseKey;
        final String release;
        final String action;

        public InstancesPluginRegionInput(String key, String clusterKey, String cluster, String hostKey, String host, String serviceKey, String service,
            String instanceId, String releaseKey, String release, String action) {
            this.key = key;
            this.clusterKey = clusterKey;
            this.cluster = cluster;
            this.hostKey = hostKey;
            this.host = host;
            this.serviceKey = serviceKey;
            this.service = service;
            this.instanceId = instanceId;
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
                Map<String, String> filters = new HashMap<>();
                filters.put("clusterKey", input.clusterKey);
                filters.put("cluster", input.cluster);
                filters.put("hostKey", input.hostKey);
                filters.put("host", input.host);
                filters.put("serviceKey", input.serviceKey);
                filters.put("service", input.service);
                filters.put("instanceId", input.instanceId);
                filters.put("releaseKey", input.releaseKey);
                filters.put("release", input.release);
                data.put("filters", filters);

                InstanceFilter filter = new InstanceFilter(
                    input.clusterKey.isEmpty() ? null : new ClusterKey(input.clusterKey),
                    input.hostKey.isEmpty() ? null : new HostKey(input.hostKey),
                    input.serviceKey.isEmpty() ? null : new ServiceKey(input.serviceKey),
                    input.releaseKey.isEmpty() ? null : new ReleaseGroupKey(input.releaseKey),
                    input.instanceId.isEmpty() ? null : Integer.parseInt(input.instanceId),
                    0, 10000);

                if (input.action != null) {
                    if (input.action.equals("filter")) {

                        data.put("message", "Filtering: "
                            + "cluster.equals '" + input.cluster + "' "
                            + "host.equals '" + input.host + "' "
                            + "service.equals '" + input.service + "' "
                            + "release.equals '" + input.release + "'"
                            + "id.equals '" + input.instanceId + "'"
                        );
                    } else if (input.action.equals("add")) {
                        filters.clear();
                        try {
                            boolean valid = true;
                            Cluster cluster = upenaStore.clusters.get(new ClusterKey(input.clusterKey));
                            if (cluster == null) {
                                data.put("message", "Cluster key:" + input.clusterKey + " is invalid.");
                                valid = false;
                            }
                            Host host = upenaStore.hosts.get(new HostKey(input.hostKey));
                            if (host == null) {
                                data.put("message", "Host key:" + input.hostKey + " is invalid.");
                                valid = false;
                            }
                            Service service = upenaStore.services.get(new ServiceKey(input.serviceKey));
                            if (service == null) {
                                data.put("message", "Service key:" + input.serviceKey + " is invalid.");
                                valid = false;
                            }
                            ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(new ReleaseGroupKey(input.releaseKey));
                            if (releaseGroup == null) {
                                data.put("message", "ReleaseGroup key:" + input.releaseKey + " is invalid.");
                                valid = false;
                            }

                            if (valid) {
                                upenaStore.instances.update(null, new Instance(
                                    new ClusterKey(input.clusterKey),
                                    new HostKey(input.hostKey),
                                    new ServiceKey(input.serviceKey),
                                    new ReleaseGroupKey(input.releaseKey),
                                    Integer.parseInt(input.instanceId),
                                    true, false, System.currentTimeMillis()
                                ));

                                data.put("message", "Created Instance.");
                            }
                        } catch (Exception x) {
                            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                            data.put("message", "Error while trying to add Instance.\n" + trace);
                        }
                    } else if (input.action.equals("update")) {
                        filters.clear();
                        try {
                            Instance instance = upenaStore.instances.get(new InstanceKey(input.key));
                            if (instance == null) {
                                data.put("message", "Couldn't update no existent Instance. Someone else likely just removed it since your last refresh.");
                            } else {
                                upenaStore.instances.update(new InstanceKey(input.key), new Instance(
                                    new ClusterKey(input.clusterKey),
                                    new HostKey(input.hostKey),
                                    new ServiceKey(input.serviceKey),
                                    new ReleaseGroupKey(input.releaseKey),
                                    Integer.parseInt(input.instanceId),
                                    true, false, System.currentTimeMillis()));
                                data.put("message", "Updated Instance:" + input.key);
                            }

                        } catch (Exception x) {
                            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                            data.put("message", "Error while trying to update Instance:" + input.key + "\n" + trace);
                        }
                    } else if (input.action.equals("remove")) {
                        if (input.key.isEmpty()) {
                            data.put("message", "Failed to remove Instance:" + input.key);
                        } else {
                            try {
                                upenaStore.instances.remove(new InstanceKey(input.key));
                            } catch (Exception x) {
                                String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                                data.put("message", "Error while trying to remove Instance:" + input.key + "\n" + trace);
                            }
                        }
                    } else if (input.action.equals("restartAll")) {
                        long now = System.currentTimeMillis();
                        long stagger = TimeUnit.SECONDS.toMillis(30);
                        now += stagger;
                        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
                        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
                            InstanceKey key = entrySet.getKey();
                            TimestampedValue<Instance> timestampedValue = entrySet.getValue();
                            Instance value = timestampedValue.getValue();
                            value.restartTimestampGMTMillis = now;
                            upenaStore.instances.update(key, value);
                            now += stagger;
                        }
                    } else if (input.action.equals("cancelRestartAll")) {
                        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
                        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
                            InstanceKey key = entrySet.getKey();
                            TimestampedValue<Instance> timestampedValue = entrySet.getValue();
                            Instance value = timestampedValue.getValue();
                            if (value.restartTimestampGMTMillis > 0) {
                                value.restartTimestampGMTMillis = -1;
                                upenaStore.instances.update(key, value);
                            }
                        }
                    } else if (input.action.equals("removeAll")) {
                        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
                        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
                            InstanceKey key = entrySet.getKey();
                            try {
                                upenaStore.instances.remove(key);
                            } catch (Exception x) {
                                String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                                data.put("message", "Error while trying to remove Instance:" + input.key + "\n" + trace);
                            }
                        }
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
                    long now = System.currentTimeMillis();
                    if (value.restartTimestampGMTMillis > 0 && now < value.restartTimestampGMTMillis) {
                        map.put("status", "Will restart in:" + DurationFormatUtils.formatDurationHMS(value.restartTimestampGMTMillis - now));
                    } else {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z");
                        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                        long snowflakeTime = timestampedValue.getTimestamp();
                        long time = JiveEpochTimestampProvider.JIVE_EPOCH + new SnowflakeIdPacker().unpack(snowflakeTime)[0];
                        String gmtTimeString = simpleDateFormat.format(new Date(time));
                        map.put("status", "Last Modified:" + gmtTimeString);
                    }
                    map.put("cluster", ImmutableMap.of(
                        "key", value.clusterKey.getKey(),
                        "name", upenaStore.clusters.get(value.clusterKey).name));
                    map.put("host", ImmutableMap.of(
                        "key", value.hostKey.getKey(),
                        "name", upenaStore.hosts.get(value.hostKey).name));
                    map.put("service", ImmutableMap.of(
                        "key", value.serviceKey.getKey(),
                        "name", upenaStore.services.get(value.serviceKey).name));
                    map.put("instanceId", String.valueOf(value.instanceId));
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
