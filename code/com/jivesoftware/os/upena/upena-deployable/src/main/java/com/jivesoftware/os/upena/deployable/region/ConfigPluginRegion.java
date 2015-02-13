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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        final String foo;

        public ConfigPluginRegionInput(String foo) {

            this.foo = foo;
        }

    }

    @Override
    public String render(Optional<ConfigPluginRegionInput> optionalInput) {
        Map<String, Object> data = Maps.newHashMap();

        try {
            if (optionalInput.isPresent()) {
                ConfigPluginRegionInput input = optionalInput.get();

                Map<String, String> filters = new HashMap<>();
                filters.put("cluster", "");
                filters.put("host", "");
                filters.put("service", "");
                filters.put("instance", "");
                filters.put("version", "");
                data.put("filter", filters);

                List<Map<String, String>> rows = new ArrayList<>();

                InstanceFilter filter = new InstanceFilter(null, null, null, null, null, 0, 10000);
                Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
                for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
                    InstanceKey key = entrySet.getKey();
                    TimestampedValue<Instance> timestampedValue = entrySet.getValue();
                    Instance value = timestampedValue.getValue();

                    Map<String, String> map = new HashMap<>();
                    Map<String, String> defaults = configStore.get(key.getKey(), "default", null);
                    Map<String, String> overriden = configStore.get(key.getKey(), "override", null);

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
        return "Instance Config";
    }

}
