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
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostFilter;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import com.jivesoftware.os.upena.uba.service.UbaService;
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
    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final UpenaService upenaService;
    private final UbaService ubaService;
    private final RingHost ringHost;

    public HostsPluginRegion(String template,
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

    public static class HostsPluginRegionInput {

        final String foo;

        public HostsPluginRegionInput(String foo) {

            this.foo = foo;
        }

    }

    @Override
    public String render(Optional<HostsPluginRegionInput> optionalInput) {
        Map<String, Object> data = Maps.newHashMap();

        try {
            if (optionalInput.isPresent()) {
                HostsPluginRegionInput input = optionalInput.get();

                List<Map<String, String>> rows = new ArrayList<>();
                HostFilter filter = new HostFilter(null, null, null, null, null, 0, 10000);
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
