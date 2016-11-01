package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.endpoints.base.HasUI;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelperUtils;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaDeployableLoopbackProxyEndpoints.LoopbackGet;
import com.jivesoftware.os.upena.deployable.region.ProbeJavaDeployablePluginRegion.ProbeJavaDeployablePluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;

/**
 *
 */
// soy.page.upenaRingPluginRegion
public class ProbeJavaDeployablePluginRegion implements PageRegion<ProbeJavaDeployablePluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final HostKey hostKey;
    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    public ProbeJavaDeployablePluginRegion(HostKey hostKey,
        String template,
        SoyRenderer renderer,
        UpenaStore upenaStore
    ) {
        this.hostKey = hostKey;
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
    }

    @Override
    public String getRootPath() {
        return "/ui/java/deployable";
    }

    public static class ProbeJavaDeployablePluginRegionInput implements PluginInput {

        final String instanceKey;
        final String action;

        public ProbeJavaDeployablePluginRegionInput(String instanceKey, String action) {
            this.instanceKey = instanceKey;
            this.action = action;
        }

        @Override
        public String name() {
            return "Java Deployable";
        }

    }

    @Override
    public String render(String user, ProbeJavaDeployablePluginRegionInput input) {
        SecurityUtils.getSubject().checkPermission("debug");

        Map<String, Object> data = Maps.newHashMap();
        data.put("instanceKey", input.instanceKey);
        data.put("instance", "TODO");
        data.put("action", input.action);

        try {
            Instance instance = upenaStore.instances.get(new InstanceKey(input.instanceKey));
            ProxyAsNeeded proxy = null;
            Host host = null;
            if (instance == null) {
                data.put("result", Arrays.asList(new String[]{"There is no instance for key:" + input.instanceKey}));
            } else {
                host = upenaStore.hosts.get(instance.hostKey);
                if (host == null) {
                    data.put("result", Arrays.asList(new String[]{"There is no host for key:" + instance.hostKey}));
                } else if (instance.hostKey.equals(hostKey)) {
                    proxy = new Local(instance);
                } else {
                    proxy = new Proxied(input.instanceKey, host.hostName);
                }
            }

            if (proxy != null) {
                Instance.Port managePort = instance.ports.get("manage");

                HasUI hasUI = proxy.get("/manage/hasUI", HasUI.class, null);
                List<Map<String, String>> uis = Lists.newArrayList();
                for (HasUI.UI ui : hasUI.uis) {
                    Instance.Port port = instance.ports.get(ui.portName);
                    if (port != null) {
                        if (managePort != null && managePort.port != port.port) {

                            Map<String, String> u = new HashMap<>();
                            u.put("name", ui.name);
                            u.put("scheme", (port.sslEnabled) ? "https" : "http");
                            u.put("host", host.hostName);
                            u.put("port", String.valueOf(port.port));
                            u.put("url", ui.url);
                            uis.add(u);
                        }
                    }
                }
                data.put("uis", uis);

                if (input.action.equals("health")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/health/ui");
                    data.put("result", r == null ? "" : r);
                } else if (input.action.equals("metrics")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/metrics/ui");
                    data.put("result", r == null ? "" : r);
                } else if (input.action.equals("tail")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/tail");
                    data.put("result", r == null ? "" : r);
                } else if (input.action.equals("threadDump")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/threadDump");
                    data.put("result", r == null ? "" : r);
                } else if (input.action.equals("forceGC")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/forceGC");
                    data.put("result", r == null ? "" : r);
                } else if (input.action.equals("resetErrors")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/resetErrors");
                    data.put("result", r == null ? "" : r);
                } else if (input.action.equals("resetThrown")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/resetThrown");
                    data.put("result", r == null ? "" : r);
                } else if (input.action.equals("resetHealth")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/resetHealth");
                    data.put("result", r == null ? "" : r);
                } else {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/health/ui");
                    data.put("result", r == null ? "" : r);
                }
            }
        } catch (AuthorizationException x) {
            throw x;
        } catch (Exception x) {
            LOG.error("Unable to retrieve data", x);
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("result", "Error while trying to " + input.action + "\n" + trace);

        }
        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Java Deployable";
    }

    static class Local implements ProxyAsNeeded {

        private final Instance instance;
        HttpRequestHelper requestHelper;

        public Local(Instance instance) throws Exception {
            this.instance = instance;
            Instance.Port manage = instance.ports.get("manage");
            this.requestHelper = HttpRequestHelperUtils.buildRequestHelper(manage.sslEnabled, true, null, "localhost", manage.port);
        }

        @Override
        public String get(String path) {
            byte[] r = requestHelper.executeGet(path);
            return r == null ? "" : new String(r);
        }

        @Override
        public <J> J get(String path, Class<J> c, J d) {
            return requestHelper.executeGetRequest(path, c, d);
        }

    }

    static class Proxied implements ProxyAsNeeded {

        String instanceKey;
        HttpRequestHelper requestHelper;

        public Proxied(String instanceKey, String host) throws Exception {
            this.instanceKey = instanceKey;
            boolean sslEnable = true;// fix
            boolean allowSelfSigendCerts = true; // fix
            int upenaPort = 1175; // fix

            this.requestHelper = HttpRequestHelperUtils.buildRequestHelper(sslEnable, allowSelfSigendCerts, null, host, upenaPort);
        }

        @Override
        public String get(String path) {
            byte[] r = requestHelper.executeRequest(new LoopbackGet(path), "/deployable/loopback/" + instanceKey, null);
            if (r == null) {
                return null;
            }
            return new String(r);
        }

        @Override
        public <J> J get(String path, Class<J> c, J d) {
            return requestHelper.executeRequest(new LoopbackGet(path), "/deployable/loopback/" + instanceKey, c, d);
        }
    }

    static interface ProxyAsNeeded {

        String get(String path);

        <J> J get(String path, Class<J> c, J d);
    }
}
