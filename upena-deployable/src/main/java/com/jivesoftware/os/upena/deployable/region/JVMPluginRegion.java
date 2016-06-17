package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.JDIAPI;
import com.jivesoftware.os.upena.deployable.JDIAPI.ThreadDumpLineType;
import com.jivesoftware.os.upena.deployable.region.JVMPluginRegion.JVMPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import java.util.ArrayList;
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
    private final UpenaStore upenaStore;
    private final JDIAPI jvm;

    public JVMPluginRegion(String template,
        SoyRenderer renderer,
        UpenaStore upenaStore,
        JDIAPI jvm) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
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
                List<Map<String, String>> lines = new ArrayList<>();
                jvm.memoryHisto(input.host, Integer.parseInt(input.port), (String name) -> {
                    lines.add(ImmutableMap.of("line", name));
                    return true;
                });
                data.put("instanceCounts", lines);
            }

            if (input.action.equals("threadDump")) {
                List<List<Map<String, String>>> threadDumps = new ArrayList<>();

                List<Map<String, String>> threadDump = new ArrayList<>();
                jvm.threadDump(input.host, Integer.parseInt(input.port), (ThreadDumpLineType type, String value) -> {

                    if (type == ThreadDumpLineType.eod) {
                        threadDumps.add(new ArrayList<>(threadDump));
                        threadDump.clear();
                    } else {
                        threadDump.add(ImmutableMap.of("type", type.toString(), "value", value));
                    }
                    return true;
                });
                data.put("threadDumps", threadDumps);
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
