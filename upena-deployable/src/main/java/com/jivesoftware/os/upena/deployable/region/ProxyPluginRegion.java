package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.UpenaProxy;
import com.jivesoftware.os.upena.deployable.region.ProxyPluginRegion.ProxyInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.shiro.SecurityUtils;

/**
 *
 */
// soy.page.upenaRingPluginRegion
public class ProxyPluginRegion implements PageRegion<ProxyInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;

    private final Map<String, UpenaProxy> proxies = new ConcurrentHashMap<>();

    public ProxyPluginRegion(String template,
        SoyRenderer renderer) {
        this.template = template;
        this.renderer = renderer;
    }

    @Override
    public String getRootPath() {
        return "/ui/proxy";
    }

    public static class ProxyInput implements PluginInput {

        private final int localPort;
        private final String remoteHost;
        private final int remotePort;
        private final String action;

        public ProxyInput(int localPort, String remoteHost, int remotePort, String action) {
            this.localPort = localPort;
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            this.action = action;
        }

        @Override
        public String name() {
            return "Proxy";
        }
    }

    private String key(String remoteHost, int remotePort) {
        return remoteHost + ":" + remotePort;
    }

    public UpenaProxy redirect(String remoteHost, int remotePort) throws IOException {
        UpenaProxy p = proxies.computeIfAbsent(key(remoteHost, remotePort), (String t) -> {

            int localPort = -1;
            for (int port = 20_000; port < 30_000; port++) { // Barf
                try (ServerSocket ss = new ServerSocket(port)) {
                    localPort = port;
                    break;
                } catch (IOException ex) {
                    continue; // try next port
                }
            }

            return new UpenaProxy(localPort, remoteHost, remotePort);
        });
        p.start();
        return p;
    }

    @Override
    public String render(String user, ProxyInput input) {
        SecurityUtils.getSubject().checkRole("readwrite");
        Map<String, Object> data = Maps.newHashMap();

        try {
            if (input.action.equals("add")) {
                UpenaProxy p = proxies.computeIfAbsent(key(input.remoteHost, input.remotePort), (String t) -> {
                    return new UpenaProxy(input.localPort, input.remoteHost, input.remotePort);
                });
                p.start();
            } else if (input.action.equals("remove")) {
                UpenaProxy p = proxies.remove(key(input.remoteHost, input.remotePort));
                if (p != null) {
                    p.stop();
                }
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (UpenaProxy p : proxies.values()) {

                Map<String, String> row = new HashMap<>();
                row.put("localPort", String.valueOf(p.getLocalPort()));
                row.put("remoteHost", p.getRemoteHost());
                row.put("remotePort", String.valueOf(p.getRemotePort()));
                row.put("running", String.valueOf(p.isRunnig()));
                row.put("proxied", String.valueOf(p.getProxied()));
                rows.add(row);
            }

            data.put("proxies", rows);

        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Proxy";
    }

}
