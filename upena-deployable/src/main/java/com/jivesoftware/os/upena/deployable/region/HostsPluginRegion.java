package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostFilter;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
// soy.page.hostsPluginRegion
public class HostsPluginRegion implements PageRegion<Optional<HostsPluginRegion.HostsPluginRegionInput>> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    public HostsPluginRegion(String template,
        SoyRenderer renderer,
        UpenaStore upenaStore) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
    }

    public static class HostsPluginRegionInput {

        final String key;
        final String name;
        final String host;
        final String port;
        final String workingDirectory;
        final String action;

        public HostsPluginRegionInput(String key, String name, String host, String port, String workingDirectory, String action) {
            this.key = key;
            this.name = name;
            this.host = host;
            this.port = port;
            this.workingDirectory = workingDirectory;
            this.action = action;
        }

    }

    @Override
    public String render(String user, Optional<HostsPluginRegionInput> optionalInput) {
        Map<String, Object> data = Maps.newHashMap();

        try {
            if (optionalInput.isPresent()) {
                HostsPluginRegionInput input = optionalInput.get();

                Map<String, String> filters = new HashMap<>();
                filters.put("name", input.name);
                filters.put("host", input.host);
                filters.put("port", String.valueOf(input.port));
                filters.put("workingDirectory", input.workingDirectory);

                HostFilter filter = new HostFilter(null, null, null, null, null, 0, 10000);
                if (input.action != null) {
                    if (input.action.equals("filter")) {
                        filter = new HostFilter(
                            input.name.isEmpty() ? null : input.name,
                            input.host.isEmpty() ? null : input.host,
                            input.port.isEmpty() ? null : Integer.valueOf(input.port),
                            input.workingDirectory.isEmpty() ? null : input.workingDirectory,
                            null,
                            0, 10000);
                        data.put("message", "Filtering: "
                            + "name.contains '" + input.name + "' "
                            + "host.contains '" + input.host + "' "
                            + "port.contains '" + input.port + "' "
                            + "workingDirectory.contains '" + input.workingDirectory + "'"
                        );
                    } else if (input.action.equals("add")) {
                        filters.clear();
                        try {
                            Host newHost = new Host(input.name,
                                input.host,
                                Integer.parseInt(input.port),
                                input.workingDirectory,
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
                        filters.clear();
                        try {
                            Host host = upenaStore.hosts.get(new HostKey(input.key));
                            if (host == null) {
                                data.put("message", "Couldn't update no existent Host. Someone else likely just removed it since your last refresh.");
                            } else {
                                Host updatedHost = new Host(input.name,
                                    input.host,
                                    Integer.parseInt(input.port),
                                    input.workingDirectory,
                                    null);
                                upenaStore.hosts.update(new HostKey(input.key), updatedHost);
                                upenaStore.record(user, "updated", System.currentTimeMillis(), "", "host-ui", updatedHost.toString());
                                data.put("message", "Updated Release:" + input.name);
                            }

                        } catch (Exception x) {
                            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                            data.put("message", "Error while trying to add Host:" + input.name + "\n" + trace);
                        }
                    } else if (input.action.equals("remove")) {
                        if (input.key.isEmpty()) {
                            data.put("message", "Failed to remove Host:" + input.name);
                        } else {
                            try {
                                HostKey hostKey = new HostKey(input.key);
                                Host removing = upenaStore.hosts.get(hostKey);
                                if (removing != null) {
                                    upenaStore.hosts.remove(new HostKey(input.key));
                                    upenaStore.record(user, "removed", System.currentTimeMillis(), "", "host-ui", removing.toString());
                                }
                            } catch (Exception x) {
                                String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                                data.put("message", "Error while trying to remove Host:" + input.name + "\n" + trace);
                            }
                        }
                    }
                }

                List<Map<String, String>> rows = new ArrayList<>();

                Map<HostKey, TimestampedValue<Host>> found = upenaStore.hosts.find(filter);
                for (Map.Entry<HostKey, TimestampedValue<Host>> entrySet : found.entrySet()) {

                    HostKey key = entrySet.getKey();
                    TimestampedValue<Host> timestampedValue = entrySet.getValue();
                    Host value = timestampedValue.getValue();

                    Map<String, String> row = new HashMap<>();
                    row.put("key", key.getKey());
                    row.put("host", value.hostName);
                    row.put("port", String.valueOf(value.port));
                    row.put("workingDirectory", value.workingDirectory);
                    row.put("name", value.name);
                    rows.add(row);
                }
                data.put("hosts", rows);

            }
        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Upena Hosts";

    }

    static class ServiceStatus {

    }
}
