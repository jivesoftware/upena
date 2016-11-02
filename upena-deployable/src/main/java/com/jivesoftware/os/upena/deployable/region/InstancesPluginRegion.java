package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jivesoftware.os.jive.utils.ordered.id.JiveEpochTimestampProvider;
import com.jivesoftware.os.jive.utils.ordered.id.SnowflakeIdPacker;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.UpenaHealth;
import com.jivesoftware.os.upena.deployable.aws.AWSClientFactory;
import com.jivesoftware.os.upena.deployable.region.InstancesPluginRegion.InstancesPluginRegionInput;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;

/**
 *
 */
// soy.page.instancesPluginRegion
public class InstancesPluginRegion implements PageRegion<InstancesPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final String simpleTemplate;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;
    private final HostKey selfHostKey;

    private final HealthPluginRegion healthPluginRegion;
    private final AWSClientFactory awsClientFactory;

    public InstancesPluginRegion(String template,
        String simpleTemplate,
        SoyRenderer renderer,
        UpenaStore upenaStore,
        HostKey selfHostKey,
        HealthPluginRegion healthPluginRegion,
        AWSClientFactory awsClientFactory
    ) {
        this.template = template;
        this.simpleTemplate = simpleTemplate;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
        this.selfHostKey = selfHostKey;
        this.healthPluginRegion = healthPluginRegion;
        this.awsClientFactory = awsClientFactory;
    }

    @Override
    public String getRootPath() {
        return "/ui/instances";
    }

    public static class InstancesPluginRegionInput implements PluginInput {

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
        final boolean sslEnabled;
        final boolean serviceAuthEnabled;
        final boolean enabled;
        final String intervalUnits;
        final String interval;
        final String action;

        public InstancesPluginRegionInput(String key, String clusterKey, String cluster, String hostKey, String host, String serviceKey, String service,
            String instanceId, String releaseKey, String release, boolean sslEnabled, boolean serviceAuthEnabled, boolean enabled, String intervalUnits,
            String interval, String action) {
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
            this.sslEnabled = sslEnabled;
            this.serviceAuthEnabled = serviceAuthEnabled;
            this.enabled = enabled;
            this.intervalUnits = intervalUnits;
            this.interval = interval;
            this.action = action;
        }

        @Override
        public String name() {
            return "Instances";
        }

    }

    @Override
    public String render(String user, InstancesPluginRegionInput input) {
        Map<String, Object> data = renderData(input, user);
        return renderer.render(template, data);
    }

    public String renderSimple(String user, InstancesPluginRegionInput input) {
        Map<String, Object> data = renderData(input, user);
        if (SecurityUtils.getSubject().isPermitted("write")) {
            data.put("readWrite", true);
        }
        data.put("filters", null);
        return renderer.render(simpleTemplate, data);
    }

    private Map<String, Object> renderData(InstancesPluginRegionInput input, String user) {
        Map<String, Object> data = Maps.newHashMap();
        if (SecurityUtils.getSubject().isPermitted("write")) {
            data.put("readWrite", true);
        }
        try {
            Map<ServiceKey, String> serviceColor = ServiceColorUtil.serviceKeysColor(upenaStore);

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
            filters.put("sslEnabled", input.sslEnabled);
            filters.put("serviceAuthEnabled", input.serviceAuthEnabled);
            filters.put("enabled", input.enabled);
            data.put("filters", filters);

            InstanceFilter filter = new InstanceFilter(
                input.clusterKey.isEmpty() ? null : new ClusterKey(input.clusterKey),
                input.hostKey.isEmpty() ? null : new HostKey(input.hostKey),
                input.serviceKey.isEmpty() ? null : new ServiceKey(input.serviceKey),
                input.releaseKey.isEmpty() ? null : new ReleaseGroupKey(input.releaseKey),
                input.instanceId.isEmpty() || !StringUtils.isNumeric(input.instanceId) ? null : Integer.parseInt(input.instanceId),
                0, 100_000);

            if (input.action != null) {
                if (input.action.equals("filter")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    handleFilter(data, input);
                } else if (input.action.equals("add")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleAdd(user, input, data);
                } else if (input.action.equals("update")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleUpdate(user, input, data);
                } else if (input.action.equals("restart")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleRestart(user, input, data);
                } else if (input.action.equals("remove")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleRemove(user, input, data);
                } else if (input.action.equals("restartAllNow")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleRestartAllNow(user, filter);
                } else if (input.action.equals("enable")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleEnable(user, filter);
                } else if (input.action.equals("enableSSL")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleSSL(user, filter, true);
                } else if (input.action.equals("disableSSL")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleSSL(user, filter, false);
                } else if (input.action.equals("enableSAUTH")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleSAUTH(user, filter, true);
                } else if (input.action.equals("disableSAUTH")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleSAUTH(user, filter, false);
                } else if (input.action.equals("disable")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleDisable(user, filter);
                } else if (input.action.equals("restartAll")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleRestartAll(user, filter);
                } else if (input.action.equals("cancelRestartAll")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleCancelRestartAll(user, filter);
                }
            }

            List<Map<String, Object>> rows = new ArrayList<>();

            Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(false, filter);
            for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
                InstanceKey key = entrySet.getKey();
                TimestampedValue<Instance> timestampedValue = entrySet.getValue();
                rows.add(clusterToMap(key, timestampedValue, serviceColor));
            }

            Collections.sort(rows, (o1, o2) -> {
                String clusterName1 = (String) ((Map) o1.get("cluster")).get("name");
                String clusterName2 = (String) ((Map) o2.get("cluster")).get("name");

                int c = clusterName1.compareTo(clusterName2);
                if (c != 0) {
                    return c;
                }

                String release1 = (String) ((Map) o1.get("release")).get("name");
                String release2 = (String) ((Map) o2.get("release")).get("name");

                c = release1.compareTo(release2);
                if (c != 0) {
                    return c;
                }

                String serviceName1 = (String) ((Map) o1.get("service")).get("name");
                String serviceName2 = (String) ((Map) o2.get("service")).get("name");
                c = serviceName1.compareTo(serviceName2);
                if (c != 0) {
                    return c;
                }

                String hostName1 = (String) ((Map) o1.get("host")).get("name");
                String hostName2 = (String) ((Map) o2.get("host")).get("name");
                c = hostName1.compareTo(hostName2);
                if (c != 0) {
                    return c;
                }

                int instanceId1 = Integer.parseInt((String) o1.get("instanceId"));
                int instanceId2 = Integer.parseInt((String) o1.get("instanceId"));

                return Integer.compare(instanceId1, instanceId2);
            });

            data.put("instances", rows);

        } catch (AuthorizationException x) {
            throw x;
        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }
        return data;
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

    private void handleCancelRestartAll(String user, InstanceFilter filter) throws Exception {
        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(false, filter);
        List<String> canceled = new ArrayList<>();
        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
            InstanceKey key = entrySet.getKey();
            TimestampedValue<Instance> timestampedValue = entrySet.getValue();
            Instance instance = timestampedValue.getValue();
            if (instance.restartTimestampGMTMillis > 0) {
                instance.restartTimestampGMTMillis = -1;
                upenaStore.instances.update(key, instance);
                canceled.add(instanceToHumanReadableString(instance));
            }
        }
        if (!canceled.isEmpty()) {
            upenaStore.record(user, "cancelRestart", System.currentTimeMillis(), "", "instance-ui", canceled.toString());

        }
    }

    private void handleEnable(String user, InstanceFilter filter) throws Exception {
        List<String> enable = new ArrayList<>();
        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(false, filter);
        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
            InstanceKey key = entrySet.getKey();
            TimestampedValue<Instance> timestampedValue = entrySet.getValue();
            Instance instance = timestampedValue.getValue();
            if (!instance.enabled) {
                instance.enabled = true;
                upenaStore.instances.update(key, instance);
                enable.add(instanceToHumanReadableString(instance));
            }
        }
        if (!enable.isEmpty()) {
            upenaStore.record(user, "enabled", System.currentTimeMillis(), "", "instance-ui", enable.toString());
        }
    }

    private void handleSSL(String user, InstanceFilter filter, boolean state) throws Exception {
        List<String> enable = new ArrayList<>();
        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(false, filter);
        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
            InstanceKey key = entrySet.getKey();
            TimestampedValue<Instance> timestampedValue = entrySet.getValue();
            Instance instance = timestampedValue.getValue();
            if (instance.ports.get("main").sslEnabled != state) {
                instance.ports.get("main").sslEnabled = state;
                upenaStore.instances.update(key, instance);
                enable.add(instanceToHumanReadableString(instance));
            }
        }
        if (!enable.isEmpty()) {
            upenaStore.record(user, "SSL=" + state, System.currentTimeMillis(), "", "instance-ui", enable.toString());
        }
    }

    private void handleSAUTH(String user, InstanceFilter filter, boolean state) throws Exception {
        List<String> enable = new ArrayList<>();
        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(false, filter);
        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
            InstanceKey key = entrySet.getKey();
            TimestampedValue<Instance> timestampedValue = entrySet.getValue();
            Instance instance = timestampedValue.getValue();
            if (instance.ports.get("main").serviceAuthEnabled != state) {
                instance.ports.get("main").serviceAuthEnabled = state;
                upenaStore.instances.update(key, instance);
                enable.add(instanceToHumanReadableString(instance));
            }
        }
        if (!enable.isEmpty()) {
            upenaStore.record(user, "SAUTH=" + state, System.currentTimeMillis(), "", "instance-ui", enable.toString());
        }
    }

    private void handleDisable(String user, InstanceFilter filter) throws Exception {
        List<String> enable = new ArrayList<>();
        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(false, filter);
        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
            InstanceKey key = entrySet.getKey();
            TimestampedValue<Instance> timestampedValue = entrySet.getValue();
            Instance instance = timestampedValue.getValue();
            if (instance.enabled) {
                instance.enabled = false;
                upenaStore.instances.update(key, instance);
                enable.add(instanceToHumanReadableString(instance));
            }
        }
        if (!enable.isEmpty()) {
            upenaStore.record(user, "disabled", System.currentTimeMillis(), "", "instance-ui", enable.toString());
        }
    }

    private void handleRestartAll(String user, InstanceFilter filter) throws Exception {
        long now = System.currentTimeMillis();
        long stagger = TimeUnit.SECONDS.toMillis(30);
        now += stagger;
        List<String> restart = new ArrayList<>();
        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(false, filter);
        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
            InstanceKey key = entrySet.getKey();
            TimestampedValue<Instance> timestampedValue = entrySet.getValue();
            Instance instance = timestampedValue.getValue();
            if (instance.enabled) {
                instance.restartTimestampGMTMillis = now;
                upenaStore.instances.update(key, instance);
                now += stagger;
                restart.add(instanceToHumanReadableString(instance));
            }
        }
        if (!restart.isEmpty()) {
            upenaStore.record(user, "restart", System.currentTimeMillis(), "", "instance-ui", restart.toString());
        }
    }

    private void handleRestartAllNow(String user, InstanceFilter filter) throws Exception {
        long now = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(false, filter);
        List<String> restart = new ArrayList<>();
        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
            InstanceKey key = entrySet.getKey();
            TimestampedValue<Instance> timestampedValue = entrySet.getValue();
            Instance instance = timestampedValue.getValue();
            if (instance.enabled) {
                instance.restartTimestampGMTMillis = now;
                upenaStore.instances.update(key, instance);
                restart.add(instanceToHumanReadableString(instance));
            }
        }
        if (!restart.isEmpty()) {
            upenaStore.record(user, "restart", System.currentTimeMillis(), "", "instance-ui", restart.toString());
        }
    }

    private void handleRemove(String user, InstancesPluginRegionInput input, Map<String, Object> data) {
        if (input.key.isEmpty()) {
            data.put("message", "Failed to remove Instance:" + input.key);
        } else {
            try {
                InstanceKey instanceKey = new InstanceKey(input.key);
                Instance removing = upenaStore.instances.get(instanceKey);
                if (removing != null) {
                    upenaStore.instances.remove(instanceKey);
                    upenaStore.record(user, "removed", System.currentTimeMillis(), "", "instance-ui", removing.toString());
                }
            } catch (Exception x) {
                String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                data.put("message", "Error while trying to remove Instance:" + input.key + "\n" + trace);
            }
        }
    }

    private void handleAdd(String user, InstancesPluginRegionInput input, Map<String, Object> data) {
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
            if (input.instanceId.isEmpty() || !StringUtils.isNumeric(input.instanceId)) {
                data.put("message", "instanceId:" + input.instanceId + " is invalid.");
                valid = false;
            }

            if (valid) {
                Instance newInstance = new Instance(
                    new ClusterKey(input.clusterKey),
                    new HostKey(input.hostKey),
                    new ServiceKey(input.serviceKey),
                    new ReleaseGroupKey(input.releaseKey),
                    Integer.parseInt(input.instanceId),
                    input.enabled,
                    false,
                    null,
                    System.currentTimeMillis(),
                    new HashMap<>()
                );
                upenaStore.instances.update(null, newInstance);
                upenaStore.record(user, "added", System.currentTimeMillis(), "", "instance-ui",
                    instanceToHumanReadableString(newInstance) + "\n" + newInstance
                    .toString());

                data.put("message", "Created Instance.");
            }
        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error while trying to add Instance.\n" + trace);
        }
    }

    public String add(String user, String clusterKey, List<String> hostKeys, String serviceKey, String releaseKey) {
        for (String hostKey : hostKeys) {
            try {
                Cluster cluster = upenaStore.clusters.get(new ClusterKey(clusterKey));
                if (cluster == null) {
                    return "Cluster key:" + clusterKey + " is invalid.";
                }
                Host host = upenaStore.hosts.get(new HostKey(hostKey));
                if (host == null) {
                    return "Host key:" + hostKey + " is invalid.";
                }
                Service service = upenaStore.services.get(new ServiceKey(serviceKey));
                if (service == null) {
                    return "Service key:" + serviceKey + " is invalid.";
                }
                ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(new ReleaseGroupKey(releaseKey));
                if (releaseGroup == null) {
                    return "ReleaseGroup key:" + releaseKey + " is invalid.";
                }

                InstanceFilter filter = new InstanceFilter(
                    new ClusterKey(clusterKey),
                    null,
                    new ServiceKey(serviceKey),
                    new ReleaseGroupKey(releaseKey),
                    null,
                    0, 100_000);

                Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(false, filter);
                Set<Integer> instanceIds = new HashSet<>();
                for (TimestampedValue<Instance> ti : found.values()) {
                    if (!ti.getTombstoned()) {
                        instanceIds.add(ti.getValue().instanceId);
                    }
                }
                int firstFreeId = -1;
                for (int i = 1; i < Short.MAX_VALUE; i++) {
                    if (!instanceIds.contains(i)) {
                        firstFreeId = i;
                        break;
                    }
                }
                if (firstFreeId == -1) {
                    return "failed to find free instanceId";
                }

                Instance newInstance = new Instance(
                    new ClusterKey(clusterKey),
                    new HostKey(hostKey),
                    new ServiceKey(serviceKey),
                    new ReleaseGroupKey(releaseKey),
                    firstFreeId,
                    false,
                    false,
                    null,
                    System.currentTimeMillis(),
                    new HashMap<>()
                );
                upenaStore.instances.update(null, newInstance);
                upenaStore.record(user, "added", System.currentTimeMillis(), "", "instance-ui",
                    instanceToHumanReadableString(newInstance) + "\n" + newInstance
                    .toString());

            } catch (Exception x) {
                String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                return "Error while trying to add Instance.\n" + trace;
            }
        }
        return "Added new instance/s";
    }

    private void handleRestart(String user, InstancesPluginRegionInput input, Map<String, Object> data) {
        try {
            Instance instance = upenaStore.instances.get(new InstanceKey(input.key));
            if (instance == null) {
                data.put("message", "Could not update. No existing instance. Someone may have removed it since your last refresh.");
            } else if (instance.enabled) {
                InstanceKey key = new InstanceKey(input.key);
                instance.restartTimestampGMTMillis = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
                upenaStore.instances.update(key, instance);
                data.put("message", "Instance will be restarted momentarily.");
                upenaStore.record(user, "restart", System.currentTimeMillis(), "", "instance-ui", instanceToHumanReadableString(instance));
            }

        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error while trying to update Instance:" + input.key + "\n" + trace);
        }
    }

    private void handleUpdate(String user, InstancesPluginRegionInput input, Map<String, Object> data) {
        try {
            Instance instance = upenaStore.instances.get(new InstanceKey(input.key));
            if (instance == null) {
                data.put("message", "Could not update. No existing instance. Someone may have removed it since your last refresh.");
            } else {
                boolean valid = true;
                ClusterKey clusterKey = new ClusterKey(input.clusterKey);
                Cluster cluster = upenaStore.clusters.get(clusterKey);
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
                if (input.instanceId.isEmpty() || !StringUtils.isNumeric(input.instanceId)) {
                    data.put("message", "instanceId:" + input.instanceId + " is invalid.");
                    valid = false;
                }

                if (valid) {
                    Map<ServiceKey, ReleaseGroupKey> defaultReleaseGroups = cluster.defaultReleaseGroups;
                    if (!defaultReleaseGroups.containsKey(new ServiceKey(input.serviceKey))) {
                        defaultReleaseGroups.put(new ServiceKey(input.serviceKey), new ReleaseGroupKey(input.releaseKey));
                        upenaStore.clusters.update(clusterKey, cluster);
                    }
                    Instance updatedInstance = new Instance(
                        new ClusterKey(input.clusterKey),
                        new HostKey(input.hostKey),
                        new ServiceKey(input.serviceKey),
                        new ReleaseGroupKey(input.releaseKey),
                        Integer.parseInt(input.instanceId),
                        input.enabled,
                        false,
                        instance.publicKey,
                        System.currentTimeMillis(),
                        instance.ports);

                    // Good god really!?
                    updatedInstance.ports.get("main").sslEnabled = input.sslEnabled;
                    updatedInstance.ports.get("manage").sslEnabled = input.sslEnabled;

                    updatedInstance.ports.get("main").serviceAuthEnabled = input.serviceAuthEnabled;
                    updatedInstance.ports.get("manage").serviceAuthEnabled = input.serviceAuthEnabled;

                    upenaStore.instances.update(new InstanceKey(input.key), updatedInstance);

                    upenaStore.record(user, "updated", System.currentTimeMillis(), "", "instance-ui", instanceToHumanReadableString(instance) + "\n"
                        + updatedInstance.toString()
                    );
                    data.put("message", "Updated Instance:" + input.key);
                }
            }

        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error while trying to update Instance:" + input.key + "\n" + trace);
        }
    }

    private UpenaHealth.NannyHealth nannyHealth(String instanceId) throws Exception {
        UpenaHealth.NannyHealth health = null;
        Collection<UpenaHealth.NodeHealth> nodeHealths = healthPluginRegion.buildClusterHealth().values();
        for (UpenaHealth.NodeHealth nodeHealth : nodeHealths) {
            for (UpenaHealth.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                if (nannyHealth.instanceDescriptor.instanceKey.equals(instanceId)) {
                    health = nannyHealth;
                    break;
                }
            }
        }
        return health;
    }

    private Map<String, Object> clusterToMap(InstanceKey key,
        TimestampedValue<Instance> timestampedValue,
        Map<ServiceKey, String> serviceColor) throws Exception {

        Instance value = timestampedValue.getValue();
        Map<String, Object> map = new HashMap<>();
        map.put("key", key.getKey());
        long now = System.currentTimeMillis();

        UpenaHealth.NannyHealth nannyHealth = nannyHealth(key.getKey());
        String color = "#404040";
        double h = 0d;
        if (timestampedValue.getValue().enabled) {
            if (nannyHealth != null) {
                color = "#" + healthPluginRegion.getHEXTrafficlightColor(nannyHealth.serviceHealth.health, 1f);
                h = nannyHealth.serviceHealth.health;
            }
        }

        map.put("health", String.valueOf((int) (100 * Math.max(0d, Math.min(1d, h)))));
        map.put("healthColor", color);

        if (value.restartTimestampGMTMillis > 0 && now < value.restartTimestampGMTMillis) {
            map.put("status", "Will restart in:" + DurationFormatUtils.formatDurationHMS(value.restartTimestampGMTMillis - now));
        } else {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            long snowflakeTime = timestampedValue.getTimestamp();
            long time = JiveEpochTimestampProvider.JIVE_EPOCH + new SnowflakeIdPacker().unpack(snowflakeTime)[0];
            map.put("status", "Modified:" + DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - time) + " ago.");
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

        map.put("sslEnabled", value.ports.get("main").sslEnabled);
        map.put("serviceAuthEnabled", value.ports.get("main").serviceAuthEnabled);

        Cluster cluster = upenaStore.clusters.get(value.clusterKey);
        Host host = upenaStore.hosts.get(value.hostKey);
        Service service = upenaStore.services.get(value.serviceKey);
        ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(value.releaseGroupKey);

        map.put("cluster", ImmutableMap.of(
            "key", value.clusterKey.getKey(),
            "name", cluster != null ? cluster.name : "unknownCluster"));

        String name = "unknownHost";
        if (host != null) {
            name = host.hostName + "/" + host.name;
            if (host.name.equals(host.hostName)) {
                name = host.hostName;
            }
        }

        map.put("host", ImmutableMap.of(
            "key", value.hostKey.getKey(),
            "name", name));
        map.put("service", ImmutableMap.of(
            "color", serviceColor.get(value.serviceKey),
            "key", value.serviceKey.getKey(),
            "name", service != null ? service.name : "unknownService"));
        map.put("instanceId", String.valueOf(value.instanceId));
        map.put("release", ImmutableMap.of(
            "key", value.releaseGroupKey.getKey(),
            "name", releaseGroup != null ? releaseGroup.name : "unknownRelease"));
        return map;
    }

    public String instanceToHumanReadableString(Instance instance) throws Exception {
        Cluster cluster = upenaStore.clusters.get(instance.clusterKey);
        Host host = upenaStore.hosts.get(instance.hostKey);
        Service service = upenaStore.services.get(instance.serviceKey);
        ReleaseGroup release = upenaStore.releaseGroups.get(instance.releaseGroupKey);
        return ((cluster == null) ? "unknownCluster" : cluster.name) + "/"
            + ((host == null) ? "unknownHost" : host.name) + "/"
            + ((service == null) ? "unknownService" : service.name) + "/"
            + String.valueOf(instance.instanceId) + "/"
            + ((release == null) ? "unknownRelease" : release.name);
    }

    public static class PortUpdate {

        public String instanceId;
        public String portName;
        public boolean sslEnabled;
        public boolean serviceAuthEnabled;
        public int port;
        public String propertyName;
        public String propertyValue;

        public PortUpdate() {
        }

        public PortUpdate(String instanceId, String portName, boolean sslEnabled, boolean serviceAuthEnabled, int port, String propertyName,
            String propertyValue) {
            this.instanceId = instanceId;
            this.portName = portName;
            this.sslEnabled = sslEnabled;
            this.serviceAuthEnabled = serviceAuthEnabled;
            this.port = port;
            this.propertyName = propertyName;
            this.propertyValue = propertyValue;
        }

        @Override
        public String toString() {
            return "PortUpdate{" + "instanceId=" + instanceId + ", portName=" + portName + ", sslEnabled=" + sslEnabled
                + ", serviceAuthEnabled=" + serviceAuthEnabled + ", port=" + port + ", propertyName=" + propertyName + ", propertyValue=" + propertyValue + '}';
        }

    }

    public void add(String user, PortUpdate update) throws Exception {
        InstanceKey instanceKey = new InstanceKey(update.instanceId);
        Instance instance = upenaStore.instances.get(instanceKey);

        Port port = instance.ports.get(update.portName);
        if (port == null) {
            port = new Port(update.sslEnabled, update.serviceAuthEnabled, update.port, new HashMap<>());
            if (update.propertyName != null && !update.propertyName.isEmpty()) {
                port.properties.put(update.propertyName, update.propertyValue);
            }
            instance.ports.put(update.portName, port);
        } else if (update.propertyName != null && !update.propertyName.isEmpty()) {
            port.properties.put(update.propertyName, update.propertyValue);
        } else {
            port.port = update.port;
        }

        upenaStore.instances.update(instanceKey, instance);
        upenaStore.record(user, "updated", System.currentTimeMillis(), "", "instance-ui", instanceToHumanReadableString(instance) + "\n" + instance
            .toString());
    }

    public void remove(String user, PortUpdate update) throws Exception {
        InstanceKey instanceKey = new InstanceKey(update.instanceId);
        Instance instance = upenaStore.instances.get(instanceKey);
        if (update.propertyName != null && !update.propertyName.isEmpty()) {
            Port port = instance.ports.get(update.portName);
            if (port != null) {
                port.properties.remove(update.propertyName);
                upenaStore.instances.update(instanceKey, instance);
                upenaStore.record(user, "updated", System.currentTimeMillis(), "", "instance-ui", instanceToHumanReadableString(instance) + "\n" + instance
                    .toString());
            }
        } else {
            instance.ports.remove(update.portName);
            upenaStore.instances.update(instanceKey, instance);
            upenaStore.record(user, "updated", System.currentTimeMillis(), "", "instance-ui", instanceToHumanReadableString(instance) + "\n" + instance
                .toString());
        }
    }

    @Override
    public String getTitle() {
        return "Upena Instances";
    }
}
