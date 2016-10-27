package com.jivesoftware.os.upena.deployable.region;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.amza.shared.AmzaInstance;
import com.jivesoftware.os.upena.amza.shared.RingHost;
import com.jivesoftware.os.upena.config.UpenaConfigStore;
import com.jivesoftware.os.upena.deployable.region.UpenaRingPluginRegion.UpenaRingPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.shiro.SecurityUtils;

/**
 *
 */
// soy.page.upenaRingPluginRegion
public class UpenaRingPluginRegion implements PageRegion<UpenaRingPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final ObjectMapper mapper;
    private final String template;
    private final SoyRenderer renderer;
    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final UpenaConfigStore configStore;

    public UpenaRingPluginRegion(ObjectMapper mapper,
        String template,
        SoyRenderer renderer,
        AmzaInstance amzaInstance,
        UpenaStore upenaStore,
        UpenaConfigStore configStore) {

        this.mapper = mapper;
        this.template = template;
        this.renderer = renderer;
        this.amzaInstance = amzaInstance;
        this.upenaStore = upenaStore;
        this.configStore = configStore;
    }

    @Override
    public String getRootPath() {
        return "/ui/ring";
    }

    // TODO Would be nice if this was stream based
    public String doExport(String remoteUser) throws Exception {
        Export export = new Export();
        upenaStore.clusters.scan((key, value) -> {
            export.clustersExport.put(key, value);
            return true;
        });

        upenaStore.hosts.scan((key, value) -> {
            export.hostsExport.put(key, value);
            return true;
        });

        upenaStore.services.scan((key, value) -> {
            export.servicesExport.put(key, value);
            return true;
        });

        upenaStore.releaseGroups.scan((key, value) -> {
            export.releasesExport.put(key, value);
            return true;
        });
        upenaStore.instances.scan((key, value) -> {
            export.instancesExport.put(key, value);

            export.instancesConfigOverridesExport.put(key, configStore.get(key.getKey(), "override", null, false));
            export.instancesConfigHealthOverridesExport.put(key, configStore.get(key.getKey(), "override-health", null, false));
            return true;
        });

        return mapper.writeValueAsString(export);
    }

    static class Export {

        public Map<ClusterKey, Cluster> clustersExport = new HashMap<>();
        public Map<HostKey, Host> hostsExport = new HashMap<>();
        public Map<ServiceKey, Service> servicesExport = new HashMap<>();
        public Map<ReleaseGroupKey, ReleaseGroup> releasesExport = new HashMap<>();
        public Map<InstanceKey, Instance> instancesExport = new HashMap<>();
        public Map<InstanceKey, Map<String, String>> instancesConfigOverridesExport = new HashMap<>();
        public Map<InstanceKey, Map<String, String>> instancesConfigHealthOverridesExport = new HashMap<>();
    }

    // TODO Would be nice if this was stream based
    public void doImport(String json, String remoteUser) throws Exception {
        Export export = mapper.readValue(json, Export.class);

        for (Map.Entry<ClusterKey, Cluster> entry : export.clustersExport.entrySet()) {
            upenaStore.clusters.putIfAbsent(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<HostKey, Host> entry : export.hostsExport.entrySet()) {
            upenaStore.hosts.putIfAbsent(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<ServiceKey, Service> entry : export.servicesExport.entrySet()) {
            upenaStore.services.putIfAbsent(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<ReleaseGroupKey, ReleaseGroup> entry : export.releasesExport.entrySet()) {
            upenaStore.releaseGroups.putIfAbsent(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<InstanceKey, Map<String, String>> entry : export.instancesConfigOverridesExport.entrySet()) {
            configStore.putAll(entry.getKey().getKey(), "override", entry.getValue());
        }

        for (Map.Entry<InstanceKey, Map<String, String>> entry : export.instancesConfigHealthOverridesExport.entrySet()) {
            configStore.putAll(entry.getKey().getKey(), "override-health", entry.getValue());
        }

        for (Map.Entry<InstanceKey, Instance> entry : export.instancesExport.entrySet()) {
            upenaStore.instances.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    public static class UpenaRingPluginRegionInput implements PluginInput {

        final String host;
        final String port;
        final String action;

        public UpenaRingPluginRegionInput(String host, String port, String action) {
            this.host = host;
            this.port = port;
            this.action = action;
        }

        @Override
        public String name() {
            return "Upena Ring";
        }

    }

    @Override
    public String render(String user, UpenaRingPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();
        if (SecurityUtils.getSubject().hasRole("readWrite")) {
            data.put("readWrite", true);
        }
        try {
            if (input.action.equals("add")) {
                amzaInstance.addRingHost("master", new RingHost(input.host, Integer.parseInt(input.port)));
            } else if (input.action.equals("remove")) {
                amzaInstance.removeRingHost("master", new RingHost(input.host, Integer.parseInt(input.port)));
            } else if (input.action.equals("clearChangeLog")) {
                upenaStore.clearChangeLog();
            } else if (input.action.equals("removeBadKeys")) {
                upenaStore.clusters.find(true, null);
                upenaStore.hosts.find(true, null);
                upenaStore.services.find(true, null);
                upenaStore.releaseGroups.find(true, null);
                upenaStore.instances.find(true, null);
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (RingHost host : amzaInstance.getRing("master")) {
                Map<String, String> row = new HashMap<>();
                row.put("host", host.getHost());
                row.put("port", String.valueOf(host.getPort()));
                rows.add(row);
            }

            data.put("ring", rows);
        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Upena Ring";
    }

}
