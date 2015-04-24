package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jivesoftware.os.upena.deployable.SARInvoker;
import com.jivesoftware.os.upena.deployable.region.HomeRegion.HomeInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class HomeRegion implements PageRegion<HomeInput> {

    private final String template;
    private final SoyRenderer renderer;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public HomeRegion(String template, SoyRenderer renderer) {
        this.template = template;
        this.renderer = renderer;
    }

    public static class HomeInput {

        final String wgetURL;
        final String upenaClusterName;

        public HomeInput(String wgetURL, String upenaClusterName) {
            this.wgetURL = wgetURL;
            this.upenaClusterName = upenaClusterName;
        }

    }

    @Override
    public String render(String user, HomeInput input) {
        Map<String, Object> data = Maps.newHashMap();
        data.put("wgetURL", input.wgetURL);
        data.put("upenaClusterName", input.upenaClusterName);

        List<Map<String, Object>> sarData = new ArrayList<>();
        data.put("sarData", sarData);

        SARInvoker sarInvoker = new SARInvoker(executor);
        sarInvoker.invoke(new String[]{"-q", "1", "1"}, new CaptureSAR(sarData, "Load"));
        sarInvoker.invoke(new String[]{"-u", "1", "1"}, new CaptureSAR(sarData, "CPU"));
        sarInvoker.invoke(new String[]{"-d", "1", "1"}, new CaptureSAR(sarData, "I/O"));
        sarInvoker.invoke(new String[]{"-r", "1", "1"}, new CaptureSAR(sarData, "Memory"));
        sarInvoker.invoke(new String[]{"-w", "1", "1"}, new CaptureSAR(sarData, "Context Switch"));
        sarInvoker.invoke(new String[]{"-n", "DEV", "1", "1"}, new CaptureSAR(sarData, "Network"));

        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Home";
    }

    private final class CaptureSAR implements SARInvoker.SAROutput {

        private final List<Map<String, Object>> data;
        private final String title;
        private final List<List<String>> lines = new ArrayList<>();

        public CaptureSAR(List<Map<String, Object>> data, String title) {
            this.data = data;
            this.title = title;
        }

        @Override
        public void line(String line) {
            if (!line.isEmpty() && !line.startsWith("Average")) {
                lines.add(Arrays.asList(line.split("\\s+")));
            }
        }

        @Override
        public void error(Throwable t) {
            data.add(ImmutableMap.<String, Object>of("title", title, "error", t.toString(), "lines", new ArrayList<String>()));
        }

        @Override
        public void success(boolean success) {
            data.add(ImmutableMap.<String, Object>of("title", title, "error", "", "lines", lines.subList(1, lines.size())));
        }

    }
}
