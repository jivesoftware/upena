package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.HostsPluginRegion.HostsPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostFilter;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
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
import java.util.concurrent.ConcurrentNavigableMap;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;

/**
 *
 */
// soy.page.hostsPluginRegion
public class HostsPluginRegion implements PageRegion<HostsPluginRegionInput> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final String remapTemplate;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    public HostsPluginRegion(String template,
        String remapTemplate,
        SoyRenderer renderer,
        UpenaStore upenaStore) {
        this.template = template;
        this.remapTemplate = remapTemplate;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
    }

    @Override
    public String getRootPath() {
        return "/ui/hosts";
    }

    public static class HostsPluginRegionInput implements PluginInput {

        final String key;
        final String name;
        final String datacenter;
        final String rack;
        final String host;
        final String port;
        final String workingDirectory;
        final String instanceId;
        final String remapKey;
        final String action;

        public HostsPluginRegionInput(String key,
            String name,
            String datacenter,
            String rack,
            String host,
            String port,
            String workingDirectory,
            String instanceId,
            String remapKey,
            String action) {

            this.key = key;
            this.name = name;
            this.datacenter = datacenter;
            this.rack = rack;
            this.host = host;
            this.port = port;
            this.workingDirectory = workingDirectory;
            this.instanceId = instanceId;
            this.action = action;
            this.remapKey = remapKey;
        }

        @Override
        public String name() {
            return "Hosts";
        }

    }

    @Override
    public String render(String user, HostsPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();
        if (SecurityUtils.getSubject().isPermitted("write")) {
            data.put("readWrite", true);
        }
        try {
            Map<ServiceKey, String> serviceColor = ServiceColorUtil.serviceKeysColor(upenaStore);

            Map<String, String> filters = new HashMap<>();
            filters.put("name", input.name);
            filters.put("host", input.host);
            filters.put("port", String.valueOf(input.port));
            filters.put("workingDirectory", input.workingDirectory);
            filters.put("instanceId", input.instanceId);
            data.put("filters", filters);

            HostFilter filter = new HostFilter(null, null, null, null, null, 0, 100_000);
            if (input.action != null) {
                if (input.action.equals("filter")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    filter = new HostFilter(
                        input.name.isEmpty() ? null : input.name,
                        input.host.isEmpty() ? null : input.host,
                        input.port.isEmpty() ? null : Integer.valueOf(input.port),
                        input.workingDirectory.isEmpty() ? null : input.workingDirectory,
                        null,
                        0, 100_000);
                    data.put("message", "Filtering: "
                        + "name.contains '" + input.name + "' "
                        + "host.contains '" + input.host + "' "
                        + "port.contains '" + input.port + "' "
                        + "workingDirectory.contains '" + input.workingDirectory + "'"
                    );
                } else if (input.action.equals("add")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    filters.clear();
                    try {
                        Host newHost = new Host(input.name,
                            input.datacenter,
                            input.rack,
                            input.host,
                            Integer.parseInt(input.port),
                            input.workingDirectory,
                            input.instanceId,
                            null
                        );
                        upenaStore.hosts.update(null, newHost);

                        upenaStore.record(user, "added", System.currentTimeMillis(), "", "host-ui", newHost.toString());

                        data.put("message", "Created Host:" + input.name);
                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to add Host:" + input.name + "\n" + trace);
                    }
                } else if (input.action.equals("update")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    filters.clear();
                    try {
                        Host host = upenaStore.hosts.get(new HostKey(input.key));
                        if (host == null) {
                            data.put("message", "Couldn't update no existent Host. Someone else likely just removed it since your last refresh.");
                        } else {
                            Host updatedHost = new Host(input.name,
                                input.datacenter,
                                input.rack,
                                input.host,
                                Integer.parseInt(input.port),
                                input.workingDirectory,
                                input.instanceId,
                                null);
                            upenaStore.hosts.update(new HostKey(input.key), updatedHost);
                            upenaStore.record(user, "updated", System.currentTimeMillis(), "", "host-ui", updatedHost.toString());
                            data.put("message", "Updated Release:" + input.name);
                        }

                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to add Host:" + input.name + "\n" + trace);
                    }
                } else if (input.action.equals("remove") || input.action.equals("force-remove")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    if (input.key.isEmpty()) {
                        data.put("message", "Failed to remove Host:" + input.name);
                    } else {
                        try {
                            HostKey hostKey = new HostKey(input.key);
                            Host removing = upenaStore.hosts.get(hostKey);
                            if (removing != null) {

                                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(
                                    false, new InstanceFilter(
                                        null,
                                        hostKey,
                                        null,
                                        null,
                                        null,
                                        0,
                                        Integer.MAX_VALUE
                                    ));
                                if (found.isEmpty() || input.action.equals("force-remove")) {
                                    upenaStore.hosts.remove(new HostKey(input.key));
                                    upenaStore.record(user, "removed", System.currentTimeMillis(), "", "host-ui", removing.toString());
                                } else {
                                    data = Maps.newHashMap();
                                    if (SecurityUtils.getSubject().isPermitted("write")) {
                                        data.put("readWrite", true);
                                    }
                                    data.put("instanceCount", String.valueOf(found.size()));
                                    data.put("hostKey", hostKey.toString());
                                    data.put("host", removing.name + " " + removing.hostName);
                                    return renderer.render(remapTemplate, data);
                                }
                            }
                        } catch (Exception x) {
                            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                            data.put("message", "Error while trying to remove Host:" + input.name + "\n" + trace);
                        }
                    }
                } else if (input.action.equals("remapper")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    if (input.key.isEmpty()) {
                        data.put("message", "Failed to remapper Host:" + input.name);
                    } else {
                        HostKey hostKey = new HostKey(input.key);
                        Host removing = upenaStore.hosts.get(hostKey);
                        if (removing != null) {
                            ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(
                                false, new InstanceFilter(
                                    null,
                                    hostKey,
                                    null,
                                    null,
                                    null,
                                    0,
                                    Integer.MAX_VALUE
                                ));
                            data = Maps.newHashMap();
                            if (SecurityUtils.getSubject().isPermitted("write")) {
                                data.put("readWrite", true);
                            }
                            data.put("instanceCount", String.valueOf(found.size()));
                            data.put("hostKey", hostKey.toString());
                            data.put("host", removing.name + " " + removing.hostName);
                            return renderer.render(remapTemplate, data);
                        }
                    }
                } else if (input.action.equals("remap")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    if (input.key.isEmpty()) {
                        data.put("message", "Failed to remap Host:" + input.name);
                    } else {
                        try {
                            HostKey existingHostKey = new HostKey(input.key);

                            HostKey remapHostKey = new HostKey(input.remapKey);
                            Host remap = upenaStore.hosts.get(remapHostKey);
                            if (remap != null) {
                                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(
                                    false, new InstanceFilter(
                                        null,
                                        existingHostKey,
                                        null,
                                        null,
                                        null,
                                        0,
                                        Integer.MAX_VALUE
                                    ));
                                if (!found.isEmpty()) {
                                    for (Entry<InstanceKey, TimestampedValue<Instance>> entry : found.entrySet()) {
                                        if (!entry.getValue().getTombstoned()) {
                                            Instance instance = entry.getValue().getValue();
                                            Instance updatedInstance = new Instance(
                                                instance.clusterKey,
                                                remapHostKey,
                                                instance.serviceKey,
                                                instance.releaseGroupKey,
                                                instance.instanceId,
                                                instance.enabled,
                                                instance.locked,
                                                instance.publicKey,
                                                instance.restartTimestampGMTMillis,
                                                instance.ports);
                                            upenaStore.instances.update(entry.getKey(), updatedInstance);
                                        }
                                    }
                                    data.put("message", "Remapped "+found.size());
                                } else {
                                    data.put("message", "There was nothing to remap.");
                                }
                            } else {
                                data.put("message", "Error while trying to remap hosKey:" + input.key + " to hostKey" + input.remapKey);
                            }
                        } catch (Exception x) {
                            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                            data.put("message", "Error while trying to remap Host:" + input.name + "\n" + trace);
                        }
                    }
                }
            }

            List<Map<String, Object>> rows = new ArrayList<>();

            Map<HostKey, TimestampedValue<Host>> found = upenaStore.hosts.find(false, filter);
            for (Map.Entry<HostKey, TimestampedValue<Host>> entrySet : found.entrySet()) {

                HostKey key = entrySet.getKey();
                TimestampedValue<Host> timestampedValue = entrySet.getValue();
                Host value = timestampedValue.getValue();

                InstanceFilter instanceFilter = new InstanceFilter(
                    null,
                    key,
                    null,
                    null,
                    null,
                    0, 100_000);

                Map<InstanceKey, TimestampedValue<Instance>> instances = upenaStore.instances.find(false, instanceFilter);
                HashMultiset<ServiceKey> serviceKeyCount = HashMultiset.create();
                for (TimestampedValue<Instance> i : instances.values()) {
                    if (!i.getTombstoned()) {
                        serviceKeyCount.add(i.getValue().serviceKey);
                    }
                }

                List<Map<String, String>> instanceCounts = new ArrayList<>();
                for (ServiceKey sk : new HashSet<>(serviceKeyCount)) {
                    Service service = upenaStore.services.get(sk);
                    instanceCounts.add(ImmutableMap.of(
                        "count", String.valueOf(serviceKeyCount.count(sk)),
                        "color", serviceColor.get(sk),
                        "name", service == null ? "unknown" : service.name
                    ));
                }

                Map<String, Object> row = new HashMap<>();
                row.put("instanceCounts", instanceCounts);
                row.put("key", key.getKey());
                row.put("datacenter", value.datacenterName);
                row.put("rack", value.rackName);
                row.put("host", value.hostName);
                row.put("port", String.valueOf(value.port));
                row.put("workingDirectory", value.workingDirectory);
                row.put("instanceId", value.instanceId);

                row.put("name", value.name);
                rows.add(row);
            }

            Collections.sort(rows, (Map<String, Object> o1, Map<String, Object> o2) -> {
                String hostName1 = (String) o1.get("host");
                String hostName2 = (String) o2.get("host");

                int c = hostName1.compareTo(hostName2);
                if (c != 0) {
                    return c;
                }
                return c;
            });

            data.put("hosts", rows);

        } catch (AuthorizationException x) {
            throw x;
        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Upena Hosts";
    }

}
