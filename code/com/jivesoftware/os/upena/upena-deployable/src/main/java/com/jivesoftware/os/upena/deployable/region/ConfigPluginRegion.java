package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.amza.shared.RingHost;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.config.UpenaConfigStore;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import com.jivesoftware.os.upena.uba.service.UbaService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 *
 */
// soy.page.configPluginRegion
public class ConfigPluginRegion implements PageRegion<Optional<ConfigPluginRegion.ConfigPluginRegionInput>> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final UpenaService upenaService;
    private final UbaService ubaService;
    private final RingHost ringHost;
    private final UpenaConfigStore configStore;

    public ConfigPluginRegion(String template,
        SoyRenderer renderer,
        AmzaInstance amzaInstance,
        UpenaStore upenaStore,
        UpenaService upenaService,
        UbaService ubaService,
        RingHost ringHost,
        UpenaConfigStore configStore) {
        this.template = template;
        this.renderer = renderer;
        this.amzaInstance = amzaInstance;
        this.upenaStore = upenaStore;
        this.upenaService = upenaService;
        this.ubaService = ubaService;
        this.ringHost = ringHost;
        this.configStore = configStore;
    }

    public void modified(Map<String, Map<String, String>> propertyMap) throws Exception {

        Set<String> instanceKeys = new HashSet<>();
        for (Map.Entry<String, Map<String, String>> propEntry : propertyMap.entrySet()) {
            for (Map.Entry<String, String> instanceKeyEntry : propEntry.getValue().entrySet()) {
                instanceKeys.add(instanceKeyEntry.getKey());
                System.out.println(String.format("%s: %s -> %s", instanceKeyEntry.getKey(), propEntry.getKey(), instanceKeyEntry.getValue()));
            }
        }

        for (String instanceKey : instanceKeys) {
            Instance instance = upenaStore.instances.get(new InstanceKey(instanceKey));
            if (instance != null) {
                Map<String, String> defaults = configStore.get(instanceKey, "default", null);
                Map<String, String> overridden = configStore.get(instanceKey, "override", null);
                boolean modified = false;
                for (Map.Entry<String, Map<String, String>> propEntry : propertyMap.entrySet()) {
                    if (propEntry.getValue().containsKey(instanceKey)) {
                        String property = propEntry.getKey();
                        String value = propEntry.getValue().get(instanceKey);
                        if (value == null || value.isEmpty() || value.equals(defaults.get(property))) {
                            overridden.remove(property);
                            modified = true;
                            log.info("Reverting to default for property:" + property + " for instance:" + instanceKey);
                        } else {
                            overridden.put(property, value);
                            modified = true;
                            log.info("Setting property:" + property + "=" + value + " for instance:" + instanceKey);
                        }
                    }
                }
                if (modified) {
                    configStore.set(instanceKey, "override", overridden);
                }

            } else {
                log.warn("Faied to load instance for key:" + instanceKey + " when trying to modify properties.");
            }
        }
    }

    public static class ConfigPluginRegionInput {

        final String aClusterKey;
        final String aCluster;
        final String aHostKey;
        final String aHost;
        final String aServiceKey;
        final String aService;
        final String aInstance;
        final String aReleaseKey;
        final String aRelease;

        final String bClusterKey;
        final String bCluster;
        final String bHostKey;
        final String bHost;
        final String bServiceKey;
        final String bService;
        final String bInstance;
        final String bReleaseKey;
        final String bRelease;

        final String property;
        final String value;
        final String overridden;

        public ConfigPluginRegionInput(
            String aClusterKey, String aCluster, String aHostKey, String aHost, String aServiceKey, String aService, String aInstance,
            String aReleaseKey, String aRelease,
            String bClusterKey, String bCluster, String bHostKey, String bHost, String bServiceKey, String bService,
            String bInstance, String bReleaseKey, String bRelease,
            String property, String value, String overridden) {
            this.aClusterKey = aClusterKey;
            this.aCluster = aCluster;
            this.aHostKey = aHostKey;
            this.aHost = aHost;
            this.aServiceKey = aServiceKey;
            this.aService = aService;
            this.aInstance = aInstance;
            this.aReleaseKey = aReleaseKey;
            this.aRelease = aRelease;
            this.bClusterKey = bClusterKey;
            this.bCluster = bCluster;
            this.bHostKey = bHostKey;
            this.bHost = bHost;
            this.bServiceKey = bServiceKey;
            this.bService = bService;
            this.bInstance = bInstance;
            this.bReleaseKey = bReleaseKey;
            this.bRelease = bRelease;
            this.property = property;
            this.value = value;
            this.overridden = overridden;
        }

    }

    @Override
    public String render(Optional<ConfigPluginRegionInput> optionalInput) {
        Map<String, Object> data = Maps.newHashMap();

        try {
            if (optionalInput.isPresent()) {
                ConfigPluginRegionInput input = optionalInput.get();

                Map<String, String> aFilters = new HashMap<>();
                aFilters.put("clusterKey", input.aClusterKey);
                aFilters.put("cluster", input.aCluster);
                aFilters.put("hostKey", input.aHostKey);
                aFilters.put("host", input.aHost);
                aFilters.put("serviceKey", input.aServiceKey);
                aFilters.put("service", input.aService);
                aFilters.put("instance", input.aInstance);
                aFilters.put("releaseKey", input.aReleaseKey);
                aFilters.put("release", input.aRelease);
                data.put("aFilters", aFilters);

                Map<String, String> bFilters = new HashMap<>();
                bFilters.put("clusterKey", input.bClusterKey);
                bFilters.put("cluster", input.bCluster);
                bFilters.put("hostKey", input.bHostKey);
                bFilters.put("host", input.bHost);
                bFilters.put("serviceKey", input.bServiceKey);
                bFilters.put("service", input.bService);
                bFilters.put("instance", input.bInstance);
                bFilters.put("releaseKey", input.bReleaseKey);
                bFilters.put("release", input.bRelease);
                data.put("bFilters", bFilters);

                data.put("property", input.property);
                data.put("value", input.value);
                data.put("overridden", input.overridden);

                ConcurrentSkipListMap<String, List<Map<String, String>>> as = packProperties(input.aClusterKey,
                    input.aHostKey, input.aServiceKey, input.aInstance, input.aReleaseKey, input.property, input.value, input.overridden);

                ConcurrentSkipListMap<String, List<Map<String, String>>> bs = packProperties(input.bClusterKey,
                    input.bHostKey, input.bServiceKey, input.bInstance, input.bReleaseKey, input.property, input.value, input.overridden);

                Set<String> allProperties = Collections.newSetFromMap(new ConcurrentSkipListMap<String, Boolean>());
                allProperties.addAll(as.keySet());
                allProperties.addAll(bs.keySet());

                String[] ks = new String[]{"instanceKey", "cluster", "host", "service", "instance", "override", "default"};
                List<Map<String, Object>> rows = new ArrayList<>();
                for (String property : allProperties) {

                    Map<String, Object> propertyAndOccurrences = new HashMap<>();
                    propertyAndOccurrences.put("property", property);

                    List<Map<String, String>> al = as.get(property);
                    List<Map<String, String>> bl = bs.get(property);

                    List<Map<String, String>> hasProperty = new ArrayList<>();
                    int s = Math.max((al == null) ? 0 : al.size(), (bl == null) ? 0 : bl.size());
                    for (int i = 0; i < s; i++) {
                        Map<String, String> has = new HashMap<>();

                        if (al != null && i < al.size()) {
                            Map<String, String> a = al.get(i);
                            for (String k : ks) {
                                has.put("a" + k, a.get(k));
                            }
                        } else {
                            for (String k : ks) {
                                has.put("a" + k, null);
                            }
                        }
                        if (bl != null && i < bl.size()) {
                            Map<String, String> b = bl.get(i);
                            for (String k : ks) {
                                has.put("b" + k, b.get(k));
                            }
                        } else {
                            for (String k : ks) {
                                has.put("b" + k, null);
                            }
                        }
                        hasProperty.add(has);
                    }
                    propertyAndOccurrences.put("has", hasProperty);

                    rows.add(propertyAndOccurrences);
                }

                data.put("properties", rows);

            }
        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    private ConcurrentSkipListMap<String, List<Map<String, String>>> packProperties(String clusterKey,
        String hostKey, String serviceKey, String instance, String releaseKey, String propertyContains,
        String valueContains, String overridden) throws Exception {

        boolean isOverridden = Boolean.parseBoolean(overridden);

        ConcurrentSkipListMap<String, List<Map<String, String>>> properties = new ConcurrentSkipListMap<>();
        InstanceFilter filter = new InstanceFilter(
            clusterKey.isEmpty() ? null : new ClusterKey(clusterKey),
            hostKey.isEmpty() ? null : new HostKey(hostKey),
            serviceKey.isEmpty() ? null : new ServiceKey(serviceKey),
            releaseKey.isEmpty() ? null : new ReleaseGroupKey(releaseKey),
            instance.isEmpty() ? null : Integer.parseInt(instance),
            0,
            10000);
        if (filter.clusterKey != null
            || filter.hostKey != null
            || filter.serviceKey != null
            || filter.releaseGroupKey != null
            || filter.logicalInstanceId != null) {

            Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
            for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
                InstanceKey key = entrySet.getKey();
                TimestampedValue<Instance> timestampedValue = entrySet.getValue();
                Instance i = timestampedValue.getValue();

                Map<String, String> defaultMaps = configStore.get(key.getKey(), "default", null);
                Map<String, String> overriddenMap = configStore.get(key.getKey(), "override", null);

                for (String property : defaultMaps.keySet()) {
                    if (!propertyContains.isEmpty() && !property.contains(propertyContains)) {
                        continue;
                    }

                    List<Map<String, String>> occurences = properties.get(property);
                    if (occurences == null) {
                        occurences = new ArrayList<>();
                        if (!valueContains.isEmpty()) {
                            List<Map<String, String>> occurencesCopy = new ArrayList<>();
                            for (Map<String, String> o : occurences) {
                                Map<String, String> valuesCopy = new ConcurrentHashMap<>(o);
                                for (Entry<String, String> e : valuesCopy.entrySet()) {
                                    if (!e.getValue().contains(valueContains)) {
                                        valuesCopy.remove(e.getKey());
                                    }
                                }
                                if (!valuesCopy.isEmpty()) {
                                    occurencesCopy.add(valuesCopy);
                                }
                            }
                            if (!occurencesCopy.isEmpty()) {
                                properties.put(property, occurencesCopy);
                            }
                        } else {
                            properties.put(property, occurences);
                        }
                    }
                    Map<String, String> occurence = new HashMap<>();
                    occurence.put("instanceKey", key.getKey());
                    occurence.put("clusterKey", i.clusterKey.getKey());
                    occurence.put("cluster", upenaStore.clusters.get(i.clusterKey).name);
                    occurence.put("hostKey", i.hostKey.getKey());
                    occurence.put("host", upenaStore.hosts.get(i.hostKey).name);
                    occurence.put("serviceKey", i.serviceKey.getKey());
                    occurence.put("service", upenaStore.services.get(i.serviceKey).name);
                    occurence.put("instance", String.valueOf(i.instanceId));
                    occurence.put("override", overriddenMap.get(property));
                    occurence.put("default", defaultMaps.get(property));

                    if (isOverridden) {
                        if (overriddenMap.containsValue(property)) {
                            occurences.add(occurence);
                        }
                    } else {
                        occurences.add(occurence);
                    }
                }
            }
        }
        return properties;
    }

    @Override
    public String getTitle() {
        return "Instance Config";
    }

}
