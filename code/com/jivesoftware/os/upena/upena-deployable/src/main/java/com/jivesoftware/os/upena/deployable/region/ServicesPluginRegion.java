package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.amza.shared.RingHost;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceFilter;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import com.jivesoftware.os.upena.uba.service.UbaService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
// soy.page.servicesPluginRegion
public class ServicesPluginRegion implements PageRegion<Optional<ServicesPluginRegion.ServicesPluginRegionInput>> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final UpenaService upenaService;
    private final UbaService ubaService;
    private final RingHost ringHost;

    public ServicesPluginRegion(String template,
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

    public static class ServicesPluginRegionInput {

        final String foo;

        public ServicesPluginRegionInput(String foo) {

            this.foo = foo;
        }

    }

    @Override
    public String render(Optional<ServicesPluginRegionInput> optionalInput) {
        Map<String, Object> data = Maps.newHashMap();

        try {
            if (optionalInput.isPresent()) {
                ServicesPluginRegionInput input = optionalInput.get();

                List<Map<String, String>> rows = new ArrayList<>();
                ServiceFilter filter = new ServiceFilter(null, null, 0, 10000);
                Map<ServiceKey, TimestampedValue<Service>> found = upenaStore.services.find(filter);
                for (Map.Entry<ServiceKey, TimestampedValue<Service>> entrySet : found.entrySet()) {

                    ServiceKey key = entrySet.getKey();
                    TimestampedValue<Service> timestampedValue = entrySet.getValue();
                    Service value = timestampedValue.getValue();

                    Map<String, String> row = new HashMap<>();
                    row.put("key", key.getKey());
                    row.put("name", value.name);
                    row.put("description", value.description);
                    rows.add(row);
                }
                data.put("services", rows);

            }
        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Upena Services";
    }

}
