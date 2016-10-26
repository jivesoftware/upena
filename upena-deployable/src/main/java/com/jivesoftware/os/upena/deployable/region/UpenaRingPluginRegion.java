package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.amza.shared.AmzaInstance;
import com.jivesoftware.os.upena.amza.shared.RingHost;
import com.jivesoftware.os.upena.deployable.region.UpenaRingPluginRegion.UpenaRingPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
// soy.page.upenaRingPluginRegion
public class UpenaRingPluginRegion implements PageRegion<UpenaRingPluginRegionInput> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;

    public UpenaRingPluginRegion(String template,
        SoyRenderer renderer,
        AmzaInstance amzaInstance,
        UpenaStore upenaStore) {
        this.template = template;
        this.renderer = renderer;
        this.amzaInstance = amzaInstance;
        this.upenaStore = upenaStore;
    }

    @Override
    public String getRootPath() {
        return "/ui/ring";
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
            log.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Upena Ring";
    }

}
