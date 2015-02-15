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
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import com.jivesoftware.os.upena.uba.service.UbaService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public static class ConfigPluginRegionInput {

        final String aCluster;
        final String aHost;
        final String aService;
        final String aInstance;
        final String aRelease;

        final String bCluster;
        final String bHost;
        final String bService;
        final String bInstance;
        final String bRelease;

        final String property;
        final String overriden;

        public ConfigPluginRegionInput(String aCluster, String aHost, String aService, String aInstance, String aRelease,
            String bCluster, String bHost, String bService, String bInstance, String bRelease,
            String property, String overriden) {
            this.aCluster = aCluster;
            this.aHost = aHost;
            this.aService = aService;
            this.aInstance = aInstance;
            this.aRelease = aRelease;
            this.bCluster = bCluster;
            this.bHost = bHost;
            this.bService = bService;
            this.bInstance = bInstance;
            this.bRelease = bRelease;
            this.property = property;
            this.overriden = overriden;
        }
    }

    @Override
    public String render(Optional<ConfigPluginRegionInput> optionalInput) {
        Map<String, Object> data = Maps.newHashMap();

        try {
            if (optionalInput.isPresent()) {
                ConfigPluginRegionInput input = optionalInput.get();

                Map<String, String> aFilters = new HashMap<>();
                aFilters.put("aCluster", input.aCluster);
                aFilters.put("aHost", input.aHost);
                aFilters.put("aService", input.aService);
                aFilters.put("aInstance", input.aInstance);
                aFilters.put("aRelease", input.aRelease);
                data.put("aFilters", aFilters);

                Map<String, String> bFilters = new HashMap<>();
                bFilters.put("bCluster", input.bCluster);
                bFilters.put("bHost", input.bHost);
                bFilters.put("bService", input.bService);
                bFilters.put("bInstance", input.bInstance);
                bFilters.put("bRelease", input.bRelease);
                data.put("bFilters", bFilters);

                data.put("property", input.property);

                Map<String, String> hack = new HashMap<>();
                hack.put("cluster", "c");
                hack.put("host", "h");
                hack.put("service", "s");
                hack.put("instance", "i");
                hack.put("override", "o");
                hack.put("default", "d");

                ConcurrentSkipListMap<String, List<Map<String, String>>> as = packProperties(input.aCluster,
                    input.aHost, input.aService, input.aInstance, input.aRelease, input.property);

                ConcurrentSkipListMap<String, List<Map<String, String>>> bs = packProperties(input.bCluster,
                    input.bHost, input.bService, input.bInstance, input.bRelease, input.property);

                as.put("hack1", Arrays.asList(hack));
                as.put("hack2", Arrays.asList(hack));
                as.put("hack4", Arrays.asList(hack));
                bs.put("hack1", Arrays.asList(hack));
                bs.put("hack3", Arrays.asList(hack));

                Set<String> allProperties = Collections.newSetFromMap(new ConcurrentSkipListMap<String, Boolean>());
                allProperties.addAll(as.keySet());
                allProperties.addAll(bs.keySet());

                String[] ks = new String[]{"cluster", "host", "service", "instance", "override", "default"};
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

    private ConcurrentSkipListMap<String, List<Map<String, String>>> packProperties(String cluster,
        String host, String service, String instance, String release, String propertyContains) throws Exception {

        InstanceFilter filter = new InstanceFilter(null, null, null, null, null, 0, 10000);
        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
        ConcurrentSkipListMap<String, List<Map<String, String>>> properties = new ConcurrentSkipListMap<>();
        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
            InstanceKey key = entrySet.getKey();
            TimestampedValue<Instance> timestampedValue = entrySet.getValue();
            Instance i = timestampedValue.getValue();

            Map<String, String> defaults = configStore.get(key.getKey(), "default", null);
            Map<String, String> overriden = configStore.get(key.getKey(), "override", null);

            for (String property : defaults.keySet()) {
                if (!propertyContains.isEmpty() && !property.contains(propertyContains)) {
                    continue;
                }
                List<Map<String, String>> occurences = properties.get(property);
                if (occurences == null) {
                    occurences = new ArrayList<>();
                    properties.put(property, occurences);
                }
                Map<String, String> occurence = new HashMap<>();
                if (cluster.isEmpty()) {
                    occurence.put("cluster", upenaStore.clusters.get(i.clusterKey).name);
                }
                if (host.isEmpty()) {
                    occurence.put("host", upenaStore.hosts.get(i.hostKey).name);
                }
                if (service.isEmpty()) {
                    occurence.put("service", upenaStore.services.get(i.serviceKey).name);
                }
                if (instance.isEmpty()) {
                    occurence.put("instance", String.valueOf(i.instanceId));
                }
                occurence.put("override", overriden.get(property));
                occurence.put("default", defaults.get(property));
                occurences.add(occurence);
            }
        }
        return properties;
    }

    @Override
    public String getTitle() {
        return "Instance Config";
    }

}
