package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.JVMAttachAPI;
import com.jivesoftware.os.upena.deployable.region.JVMPluginRegion.JVMPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 */
// soy.page.upenaRingPluginRegion
public class JVMPluginRegion implements PageRegion<JVMPluginRegionInput> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final AmzaInstance amzaInstance;
    private final JVMAttachAPI jvm;

    public JVMPluginRegion(String template,
        SoyRenderer renderer,
        AmzaInstance amzaInstance,
        JVMAttachAPI jvm) {
        this.template = template;
        this.renderer = renderer;
        this.amzaInstance = amzaInstance;
        this.jvm = jvm;
    }

    @Override
    public String getRootPath() {
        return "/ui/jvm";
    }

    public static class JVMPluginRegionInput implements PluginInput {

        final String host;
        final String port;
        final String action;

        public JVMPluginRegionInput(String host, String port, String action) {
            this.host = host;
            this.port = port;
            this.action = action;
        }

        @Override
        public String name() {
            return "JVM";
        }

    }

    @Override
    public String render(String user, JVMPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();
        data.put("host", input.host);
        data.put("port", input.port);
        try {

            if (input.action.equals("memoryHisto")) {
                List<Map<String, String>> instanceCounts = new ArrayList<>();
                jvm.memoryHisto(input.host, Integer.parseInt(input.port), (String name, long count) -> {
                    instanceCounts.add(ImmutableMap.of("name", name, "count", String.valueOf(count)));
                    return true;
                });
                Collections.sort(instanceCounts, (Map<String, String> o1, Map<String, String> o2) -> {
                    int c = Long.compare(Long.parseLong(o1.get("count")), Long.parseLong(o2.get("count")));
                    if (c != 0) {
                        return -c;
                    }
                    return o1.get("name").compareTo(o2.get("name"));
                });
                data.put("instanceCounts", instanceCounts);
            }

            if (input.action.equals("threadDump")) {
                List<Map<String, String>> threadDump = new ArrayList<>();
                jvm.threadDump(input.host, Integer.parseInt(input.port), (String type, String value) -> {
                    threadDump.add(ImmutableMap.of("type", type, "value", value));
                    return true;
                });
                data.put("threadDump", threadDump);
            }

        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }
        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "JVM";
    }

}
