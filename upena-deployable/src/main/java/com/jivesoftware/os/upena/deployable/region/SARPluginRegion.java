package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.amza.shared.RingHost;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.SARInvoker;
import com.jivesoftware.os.upena.deployable.region.SARPluginRegion.SARInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import java.util.ArrayList;
import java.util.Arrays;
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
        data.put("sarData", sarData);

        sarData.add(capture(new String[]{"-q", "1", "1"}, "Load"));
        sarData.add(capture(new String[]{"-u", "1", "1"}, "CPU"));
        sarData.add(capture(new String[]{"-d", "1", "1"}, "I/O"));
        sarData.add(capture(new String[]{"-r", "1", "1"}, "Memory"));
        sarData.add(capture(new String[]{"-B", "1", "1"}, "Paging"));
        sarData.add(capture(new String[]{"-w", "1", "1"}, "Context Switch"));
        sarData.add(capture(new String[]{"-n", "DEV", "1", "1"}, "Network"));

        return renderer.render(template, data);
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
            captured.set(ImmutableMap.<String, Object>of("title", title, "error", "Loading", "lines", new ArrayList<String>()));

        }

        @Override
        public void line(String line) {
            if (!line.isEmpty() && !line.startsWith("Average")) {
                lines.add(Arrays.asList(line.split("\\s+")));
            }
        }

        @Override
        public void error(Throwable t) {
            captured.set(ImmutableMap.<String, Object>of("title", title, "error", t.toString(), "lines", new ArrayList<String>()));
            lines.clear();
            running.set(false);
        }

        @Override
        public void success(boolean success) {
            captured.set(ImmutableMap.<String, Object>of("title", title, "error", "", "lines", new ArrayList<>(lines.subList(1, lines.size()))));
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
