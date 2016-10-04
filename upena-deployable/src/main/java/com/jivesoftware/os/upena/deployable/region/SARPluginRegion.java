package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jivesoftware.os.upena.amza.shared.AmzaInstance;
import com.jivesoftware.os.upena.amza.shared.RingHost;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.SARInvoker;
import com.jivesoftware.os.upena.deployable.region.SARPluginRegion.SARInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class SARPluginRegion implements PageRegion<SARInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final SARInvoker sarInvoker = new SARInvoker(executor);
    private final Map<String, CaptureSAR> latestSAR = new ConcurrentHashMap<>();
    private final AmzaInstance amzaInstance;
    private final RingHost ringHost;

    public SARPluginRegion(String template, SoyRenderer renderer, AmzaInstance amzaInstance, RingHost ringHost) {
        this.template = template;
        this.renderer = renderer;
        this.amzaInstance = amzaInstance;
        this.ringHost = ringHost;
    }

    @Override
    public String getRootPath() {
        return "/ui/sar";
    }

    public static class SARInput implements PluginInput {

        public SARInput() {
        }

        @Override
        public String name() {
            return "SAR";
        }

    }

    @Override
    public String render(String user, SARInput input) {
        Map<String, Object> data = Maps.newHashMap();
        data.put("currentHost", ringHost.getHost() + ":" + ringHost.getPort());

        try {
            List<RingHost> ring = amzaInstance.getRing("master");
            int hostIndex = ring.indexOf(ringHost);
            if (hostIndex != -1) {
                RingHost priorHost = ring.get(hostIndex - 1 < 0 ? ring.size() - 1 : hostIndex - 1);
                data.put("priorHost", priorHost.getHost() + ":" + priorHost.getPort());
                RingHost nextHost = ring.get(hostIndex + 1 >= ring.size() ? 0 : hostIndex + 1);
                data.put("nextHost", nextHost.getHost() + ":" + nextHost.getPort());
            } else {
                data.put("nextHost", "invalid");
                data.put("priorHost", "invalid");
            }
        } catch (Exception x) {
            LOG.warn("Failed getting master ring.", x);
            data.put("nextHost", "unknown");
            data.put("priorHost", "unknown");
        }

        List<Map<String, Object>> sarData = new ArrayList<>();

        Map<String, Object> captureLoad = capture(new String[]{"-q"}, "Load");
        sarData.add(pack(captureLoad, 2, new int[]{4, 5, 6}, "load")); // "1", "1"

        sarData.add(pack(capture(new String[]{"-u"}, "CPU"), 3, new int[]{3, 4, 5, 6, 7, 8}, "cpu"));
        sarData.add(pack(capture(new String[]{"-d"}, "I/O"), 3, new int[]{3, 4, 5, 6, 7, 8, 9, 10}, "io"));

        Map<String, Object> captureMemory = capture(new String[]{"-r"}, "Memory");
        sarData.add(pack(captureMemory, 2, new int[]{2, 3, 5, 6, 7}, "mem"));
        sarData.add(pack(captureMemory, 2, new int[]{4, 8}, "memPercent"));

        sarData.add(pack(capture(new String[]{"-B"}, "Paging"), 2, new int[]{2, 3, 4, 6, 7, 8, 9, 10}, "page"));

        Map<String, Object> captureContextSwitches = capture(new String[]{"-w"}, "Context Switch");
        sarData.add(pack(captureContextSwitches, 1, new int[]{2}, "contextProcs"));
        sarData.add(pack(captureContextSwitches, 1, new int[]{3}, "contextCswch"));
        sarData.add(pack(capture(new String[]{"-n", "DEV"}, "Network"), 3, new int[]{3, 4, 5, 6, 7, 8, 9}, "net"));

        data.put("sarData", sarData);

        return renderer.render(template, data);
    }

    Map<String, Object> pack(Map<String, Object> capture, int labelColumnCount, int[] valueColumnIndex, String id) {
        List<String> labels = new ArrayList<>();
        List<Map<String, Object>> valueDatasets = new ArrayList<>();

        String title = (String) capture.get("title");
        String error = (String) capture.get("error");
        List<List<String>> lines = (List<List<String>>) capture.get("lines");

        if (lines.size() > 1) {
            int step = lines.size() / 64;
            for (int vi = 0; vi < valueColumnIndex.length; vi++) {
                if (lines.get(0).size() > valueColumnIndex[vi]) {
                    List<String> values = new ArrayList<>();
                    for (int i = 1; i < lines.size(); i++) {
                        double v = 0;
                        int s = 0;
                        for (; s < step && i + s < lines.size(); s++) {
                            v += Double.valueOf(lines.get(i + s).get(valueColumnIndex[vi]));
                        }
                        v /= s;
                        values.add(lines.get(i).get(valueColumnIndex[vi]));
                    }
                    valueDatasets.add(
                        waveform(lines.get(0).get(valueColumnIndex[vi]), getIndexColor((double) vi / (double) valueColumnIndex.length, 1f), 1f, values));
                } else {
                    LOG.warn("Invalid value index:{} for id:{}", valueColumnIndex[vi], id);
                }
            }

            for (int i = 1; i < lines.size(); i += step + 1) {
                labels.add("\"" + Joiner.on(" ").join(lines.get(i).subList(0, labelColumnCount)) + "\"");
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("lines", lines);
        map.put("error", error);
        map.put("width", String.valueOf(labels.size() * 32));
        map.put("id", "sar" + id);
        map.put("graphType", "Line");
        map.put("waveform", ImmutableMap.of("labels", labels, "datasets", valueDatasets));
        return map;
    }

    Color getIndexColor(double value, float sat) {
        float hue = (float) value / 3f;
        hue = (1f / 3f) + (hue * 2);
        return new Color(Color.HSBtoRGB(hue, sat, 1f));
    }

    public Map<String, Object> waveform(String label, Color color, float alpha, List<String> values) {
        Map<String, Object> waveform = new HashMap<>();
        waveform.put("label", "\"" + label + "\"");
        waveform.put("fillColor", "\"rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + String.valueOf(alpha) + ")\"");
        waveform.put("strokeColor", "\"rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)\"");
        waveform.put("pointColor", "\"rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)\"");
        waveform.put("pointStrokeColor", "\"rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)\"");
        waveform.put("pointHighlightFill", "\"rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)\"");
        waveform.put("pointHighlightStroke", "\"rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)\"");
        waveform.put("data", values);
        return waveform;
    }

    private Map<String, Object> capture(String[] args, String key) {
        CaptureSAR sar = latestSAR.get(key);
        if (sar == null) {
            sar = new CaptureSAR(sarInvoker, args, key);
            latestSAR.put(key, sar);
        }
        executor.submit(sar);
        return sar.captured.get();

    }

    @Override
    public String getTitle() {
        return "Home";
    }

    private final class CaptureSAR implements SARInvoker.SAROutput, Runnable {

        private final SARInvoker sarInvoker;
        private final String[] args;
        private final String title;
        private final List<List<String>> lines = new ArrayList<>();
        private final AtomicReference<Map<String, Object>> captured = new AtomicReference<>();
        private final AtomicBoolean running = new AtomicBoolean(false);

        public CaptureSAR(SARInvoker sarInvoker, String[] args, String title) {
            this.sarInvoker = sarInvoker;
            this.args = args;
            this.title = title;
            captured.set(ImmutableMap.<String, Object>of("title", title,
                "error", "Loading",
                "lines", new ArrayList<>()));

        }

        String lastTimestamp = null;

        @Override
        public void line(String line) {
            if (!line.isEmpty() && !line.startsWith("Average")) {
                String[] parts = line.split("\\s+");
                if (parts.length > 0) {
                    if (lastTimestamp != null && lastTimestamp.equals(parts[0])) {
                        return;
                    }
                    lastTimestamp = parts[0];
                    lines.add(Arrays.asList(parts));
                }
            }
        }

        @Override
        public void error(Throwable t) {
            captured.set(ImmutableMap.<String, Object>of("title", title,
                "error", t.toString(),
                "lines", new ArrayList<>()
            ));
            lines.clear();
            running.set(false);
        }

        @Override
        public void success(boolean success) {
            captured.set(ImmutableMap.<String, Object>of("title", title,
                "error", "",
                "lines", new ArrayList<>(lines.subList(1, lines.size()))));
            lines.clear();
            running.set(false);
        }

        @Override
        public void run() {
            if (running.compareAndSet(false, true)) {
                lines.clear();
                sarInvoker.invoke(args, this);
            }
        }
    }
}
