package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jivesoftware.os.jive.utils.ordered.id.JiveEpochTimestampProvider;
import com.jivesoftware.os.jive.utils.ordered.id.SnowflakeIdPacker;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.Instance.Port;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
    private final UpenaStore upenaStore;

    public InstancesPluginRegion(String template,
        SoyRenderer renderer,
        UpenaStore upenaStore
    ) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
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
        final boolean enabled;
        final String action;

        public InstancesPluginRegionInput(String key, String clusterKey, String cluster, String hostKey, String host, String serviceKey, String service,
            String instanceId, String releaseKey, String release, boolean enabled, String action) {
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
            this.enabled = enabled;
            this.action = action;
        }

    }

    @Override
    public String render(Optional<InstancesPluginRegionInput> optionalInput) {
        Map<String, Object> data = Maps.newHashMap();

        try {
            if (optionalInput.isPresent()) {
                InstancesPluginRegionInput input = optionalInput.get();
                Map<String, Object> filters = new HashMap<>();
                filters.put("clusterKey", input.clusterKey);
                filters.put("cluster", input.cluster);
                filters.put("hostKey", input.hostKey);
                filters.put("host", input.host);
                filters.put("serviceKey", input.serviceKey);
                filters.put("service", input.service);
                filters.put("instanceId", input.instanceId);
                filters.put("releaseKey", input.releaseKey);
                filters.put("release", input.release);
                filters.put("enabled", input.enabled);
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
                        handleFilter(data, input);
                    } else if (input.action.equals("add")) {
                        handleAdd(filters, input, data);
                    } else if (input.action.equals("update")) {
                        handleUpdate(filters, input, data);
                    } else if (input.action.equals("restart")) {
                        handleRestart(filters, input, data);
                    } else if (input.action.equals("remove")) {
                        handleRemove(input, data);
                    } else if (input.action.equals("restartAllNow")) {
                        handleRestartAllNow(filter);
                    } else if (input.action.equals("restartAll")) {
                        handleRestartAll(filter);
                    } else if (input.action.equals("cancelRestartAll")) {
                        handleCancelRestartAll(filter);
                    }
                }

                List<Map<String, Object>> rows = new ArrayList<>();

                Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
                for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
                    InstanceKey key = entrySet.getKey();
                    TimestampedValue<Instance> timestampedValue = entrySet.getValue();
                    Instance value = timestampedValue.getValue();

                    rows.add(clusterToMap(key, value, timestampedValue));
                }

                data.put("instances", rows);
            }
        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    private void handleFilter(Map<String, Object> data, InstancesPluginRegionInput input) {
        data.put("message", "Filtering: "
            + "cluster.equals '" + input.cluster + "' "
            + "host.equals '" + input.host + "' "
            + "service.equals '" + input.service + "' "
            + "release.equals '" + input.release + "'"
            + "id.equals '" + input.instanceId + "'"
        );
    }

    private void handleCancelRestartAll(InstanceFilter filter) throws Exception {
        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
        List<Instance> canceled = new ArrayList<>();
        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
            InstanceKey key = entrySet.getKey();
            TimestampedValue<Instance> timestampedValue = entrySet.getValue();
            Instance value = timestampedValue.getValue();
            if (value.restartTimestampGMTMillis > 0) {
                value.restartTimestampGMTMillis = -1;
                upenaStore.instances.update(key, value);
                canceled.add(value);
            }
        }
        if (!canceled.isEmpty()) {
            upenaStore.record("Human", "cancelRestart", System.currentTimeMillis(), "", "instance", canceled.toString());

        }
    }

    private void handleRestartAll(InstanceFilter filter) throws Exception {
        long now = System.currentTimeMillis();
        long stagger = TimeUnit.SECONDS.toMillis(30);
        now += stagger;
        List<Instance> restart = new ArrayList<>();
        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
            InstanceKey key = entrySet.getKey();
            TimestampedValue<Instance> timestampedValue = entrySet.getValue();
            Instance value = timestampedValue.getValue();
            if (value.enabled) {
                value.restartTimestampGMTMillis = now;
                upenaStore.instances.update(key, value);
                now += stagger;
                restart.add(value);
            }
        }
        if (!restart.isEmpty()) {
            upenaStore.record("Human", "restart", System.currentTimeMillis(), "", "instance", restart.toString());
        }
    }

    private void handleRestartAllNow(InstanceFilter filter) throws Exception {
        long now = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
        List<Instance> restart = new ArrayList<>();
        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
            InstanceKey key = entrySet.getKey();
            TimestampedValue<Instance> timestampedValue = entrySet.getValue();
            Instance value = timestampedValue.getValue();
            if (value.enabled) {
                value.restartTimestampGMTMillis = now;
                upenaStore.instances.update(key, value);
                restart.add(value);
            }
        }
        if (!restart.isEmpty()) {
            upenaStore.record("Human", "restart", System.currentTimeMillis(), "", "instance", restart.toString());
        }
    }

    private void handleRemove(InstancesPluginRegionInput input, Map<String, Object> data) {
        if (input.key.isEmpty()) {
            data.put("message", "Failed to remove Instance:" + input.key);
        } else {
            try {
                InstanceKey instanceKey = new InstanceKey(input.key);
                Instance removing = upenaStore.instances.get(instanceKey);
                if (removing != null) {
                    upenaStore.instances.remove(instanceKey);
                    upenaStore.record("Human", "removed", System.currentTimeMillis(), "", "instance", removing.toString());
                }
            } catch (Exception x) {
                String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                data.put("message", "Error while trying to remove Instance:" + input.key + "\n" + trace);
            }
        }
    }

    private void handleAdd(Map<String, Object> filters, InstancesPluginRegionInput input, Map<String, Object> data) {
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
                Instance newInstance = new Instance(
                    new ClusterKey(input.clusterKey),
                    new HostKey(input.hostKey),
                    new ServiceKey(input.serviceKey),
                    new ReleaseGroupKey(input.releaseKey),
                    Integer.parseInt(input.instanceId),
                    input.enabled, false, System.currentTimeMillis()
                );
                upenaStore.instances.update(null, newInstance);
                upenaStore.record("Human", "added", System.currentTimeMillis(), "", "instance", newInstance.toString());

                data.put("message", "Created Instance.");
            }
        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error while trying to add Instance.\n" + trace);
        }
    }

    private void handleRestart(Map<String, Object> filters, InstancesPluginRegionInput input, Map<String, Object> data) {
        filters.clear();
        try {
            Instance instance = upenaStore.instances.get(new InstanceKey(input.key));
            if (instance == null) {
                data.put("message", "Couldn't update no existent Instance. Someone else likely just removed it since your last refresh.");
            } else if (instance.enabled) {
                InstanceKey key = new InstanceKey(input.key);
                instance.restartTimestampGMTMillis = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
                upenaStore.instances.update(key, instance);
                data.put("message", "Instance will be restarted momentarily.");
                upenaStore.record("Human", "restart", System.currentTimeMillis(), "", "instance", instance.toString());
            }

        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error while trying to update Instance:" + input.key + "\n" + trace);
        }
    }

    private void handleUpdate(Map<String, Object> filters, InstancesPluginRegionInput input, Map<String, Object> data) {
        filters.clear();
        try {
            Instance instance = upenaStore.instances.get(new InstanceKey(input.key));
            if (instance == null) {
                data.put("message", "Couldn't update no existent Instance. Someone else likely just removed it since your last refresh.");
            } else {
                ClusterKey clusterKey = new ClusterKey(input.clusterKey);
                Cluster cluster = upenaStore.clusters.get(clusterKey);
                if (cluster != null) {
                    Map<ServiceKey, ReleaseGroupKey> defaultReleaseGroups = cluster.defaultReleaseGroups;
                    if (!defaultReleaseGroups.containsKey(new ServiceKey(input.serviceKey))) {
                        defaultReleaseGroups.put(new ServiceKey(input.serviceKey), new ReleaseGroupKey(input.releaseKey));
                        upenaStore.clusters.update(clusterKey, cluster);
                    }
                }
                Instance updatedInstance = new Instance(
                    new ClusterKey(input.clusterKey),
                    new HostKey(input.hostKey),
                    new ServiceKey(input.serviceKey),
                    new ReleaseGroupKey(input.releaseKey),
                    Integer.parseInt(input.instanceId),
                    input.enabled, false, System.currentTimeMillis());

                upenaStore.instances.update(new InstanceKey(input.key), updatedInstance);

                upenaStore.record("Human", "updated", System.currentTimeMillis(), "", "instance", updatedInstance.toString());
                data.put("message", "Updated Instance:" + input.key);
            }

        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error while trying to update Instance:" + input.key + "\n" + trace);
        }
    }

    private Map<String, Object> clusterToMap(InstanceKey key, Instance value, TimestampedValue<Instance> timestampedValue) throws Exception {
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

        List<Map<String, Object>> ports = new ArrayList<>();
        for (Map.Entry<String, Port> e : value.ports.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("portName", e.getKey());
            row.put("port", String.valueOf(e.getValue().port));

            List<Map<String, String>> properties = new ArrayList<>();
            for (Entry<String, String> p : e.getValue().properties.entrySet()) {
                properties.add(ImmutableMap.of("name", p.getKey(), "value", p.getValue()));
            }
            row.put("properties", properties);
            ports.add(row);
        }
        map.put("ports", ports);
        map.put("enabled", value.enabled);
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
        return map;
    }

    public static class PortUpdate {

        public String instanceId;
        public String portName;
        public int port;
        public String propertyName;
        public String propertyValue;

        public PortUpdate() {
        }

        public PortUpdate(String instanceId, String portName, int port, String propertyName, String propertyValue) {
            this.instanceId = instanceId;
            this.portName = portName;
            this.port = port;
            this.propertyName = propertyName;
            this.propertyValue = propertyValue;
        }

        @Override
        public String toString() {
            return "PortUpdate{"
                + "instanceId=" + instanceId
                + ", portName=" + portName
                + ", port=" + port
                + ", propertyName=" + propertyName
                + ", propertyValue=" + propertyValue
                + '}';
        }

    }

    public void add(PortUpdate update) throws Exception {
        InstanceKey instanceKey = new InstanceKey(update.instanceId);
        Instance instance = upenaStore.instances.get(instanceKey);
        Port port = instance.ports.get(update.portName);
        if (port == null) {
            port = new Port(update.port, new HashMap<String, String>());
            if (update.propertyName != null && !update.propertyName.isEmpty()) {
                port.properties.put(update.propertyName, update.propertyValue);
            }
            instance.ports.put(update.portName, port);
        } else {
            if (update.propertyName != null && !update.propertyName.isEmpty()) {
                port.properties.put(update.propertyName, update.propertyValue);
            } else {
                port.port = update.port;
            }
        }
        upenaStore.record("Human", "updated", System.currentTimeMillis(), "", "instance", instance.toString());
        upenaStore.instances.update(instanceKey, instance);

    }

    public void remove(PortUpdate update) throws Exception {
        InstanceKey instanceKey = new InstanceKey(update.instanceId);
        Instance instance = upenaStore.instances.get(instanceKey);
        if (update.propertyName != null && !update.propertyName.isEmpty()) {
            Port port = instance.ports.get(update.portName);
            if (port != null) {
                port.properties.remove(update.propertyName);
                upenaStore.instances.update(instanceKey, instance);
                upenaStore.record("Human", "updated", System.currentTimeMillis(), "", "instance", instance.toString());
            }
        } else {
            instance.ports.remove(update.portName);
            upenaStore.instances.update(instanceKey, instance);
            upenaStore.record("Human", "updated", System.currentTimeMillis(), "", "instance", instance.toString());
        }
    }

    @Override
    public String getTitle() {
        return "Upena Instances";
    }
}
