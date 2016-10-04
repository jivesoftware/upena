package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.Maps;
import com.jivesoftware.os.upena.amza.shared.AmzaInstance;
import com.jivesoftware.os.upena.amza.shared.RingHost;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.UpenaRingPluginRegion.UpenaRingPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;

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

    public UpenaRingPluginRegion(String template,
        SoyRenderer renderer,
        AmzaInstance amzaInstance) {
        this.template = template;
        this.renderer = renderer;
        this.amzaInstance = amzaInstance;
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
