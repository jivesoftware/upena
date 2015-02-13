package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
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

        final String key;
        final String name;
        final String description;
        final String action;

        public ServicesPluginRegionInput(String key, String name, String description, String action) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.action = action;
        }

    }

    @Override
    public String render(Optional<ServicesPluginRegionInput> optionalInput) {
        Map<String, Object> data = Maps.newHashMap();

        try {
            if (optionalInput.isPresent()) {
                ServicesPluginRegionInput input = optionalInput.get();

                 Map<String, String> filters = new HashMap<>();
                filters.put("name", input.name);
                filters.put("description", input.description);
                data.put("filters", filters);

                ServiceFilter filter = new ServiceFilter(null, null, 0, 10000);
                if (input.action != null) {
                    if (input.action.equals("filter")) {
                        filter = new ServiceFilter(
                            input.name.isEmpty() ? null : input.name,
                            input.description.isEmpty() ? null : input.description,
                            0, 10000);
                        data.put("message", "Filtering: name.contains '" + input.name + "' description.contains '" + input.description + "'");
                    } else if (input.action.equals("add")) {
                        filters.clear();
                        try {
                            upenaStore.services.update(null, new Service(input.name, input.description));

                            data.put("message", "Created Service:" + input.name);
                        } catch (Exception x) {
                            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                            data.put("message", "Error while trying to add Service:" + input.name + "\n" + trace);
                        }
                    } else if (input.action.equals("update")) {
                        filters.clear();
                        try {
                            Service service = upenaStore.services.get(new ServiceKey(input.key));
                            if (service == null) {
                                data.put("message", "Couldn't update no existent Service. Someone else likely just removed it since your last refresh.");
                            } else {
                                upenaStore.services.update(new ServiceKey(input.key), new Service(input.name, input.description));
                                data.put("message", "Service Cluster:" + input.name);
                            }

                        } catch (Exception x) {
                            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                            data.put("message", "Error while trying to add Service:" + input.name + "\n" + trace);
                        }
                    } else if (input.action.equals("remove")) {
                        if (input.key.isEmpty()) {
                            data.put("message", "Failed to remove Service:" + input.name);
                        } else {
                            try {
                                upenaStore.services.remove(new ServiceKey(input.key));
                            } catch (Exception x) {
                                String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                                data.put("message", "Error while trying to remove Service:" + input.name + "\n" + trace);
                            }
                        }
                    }
                }

                List<Map<String, String>> rows = new ArrayList<>();
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
