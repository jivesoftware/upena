package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.endpoints.base.HasUI;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelperUtils;
import com.jivesoftware.os.upena.deployable.UpenaSSLConfig;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaManagedDeployableEndpoints.LoopbackGet;
import com.jivesoftware.os.upena.deployable.region.ManagedDeployablePluginRegion.ManagedDeployablePluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.SessionStore;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceKey;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;

// soy.page.deployablePluginRegion
public class ManagedDeployablePluginRegion implements PageRegion<ManagedDeployablePluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SessionStore sessionStore;
    private final HostKey hostKey;
    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;
    private final UpenaSSLConfig upenaSSLConfig;
    private final int upenaPort;

    public ManagedDeployablePluginRegion(SessionStore sessionStore,
        HostKey hostKey,
        String template,
        SoyRenderer renderer,
        UpenaStore upenaStore,
        UpenaSSLConfig upenaSSLConfig,
        int upenaPort) {
        this.sessionStore = sessionStore;
        this.hostKey = hostKey;
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
        this.upenaSSLConfig = upenaSSLConfig;
        this.upenaPort = upenaPort;
    }

    @Override
    public String getRootPath() {
        return "/ui/deployable";
    }

    public static class ManagedDeployablePluginRegionInput implements PluginInput {

        final String instanceKey;
        final String action;

        public ManagedDeployablePluginRegionInput(String instanceKey, String action) {
            this.instanceKey = instanceKey;
            this.action = action;
        }

        @Override
        public String name() {
            return "Java Deployable";
        }

    }

    public URI redirectToUI(String instanceKey, String portName, String uiPath) throws Exception {
        ProxyAsNeeded proxy = buildProxy(instanceKey, new HashMap<>());
        if (proxy != null) {
            Host upenaHost = upenaStore.hosts.get(hostKey);

            Instance instance = upenaStore.instances.get(new InstanceKey(instanceKey));
            Host instanceHost = upenaStore.hosts.get(instance.hostKey);
            Instance.Port port = instance.ports.get(portName);

            if (port != null) {
                String token = proxy.allocateAccessToken();

                return URI.create((port.sslEnabled ? "https" : "http") + "://" + instanceHost.name + ":" + port.port
                    + (uiPath.startsWith("/") ? uiPath : "/" + uiPath)
                    + "?rb_access_token=" + token
                    + "&rb_access_redir_url=" + (upenaSSLConfig.sslEnable ? "https" : "http") + "://" + upenaHost.name + ":" + upenaHost.port
                );
            }
        }
        return null;
    }

    private ProxyAsNeeded buildProxy(String instanceKey, Map<String, Object> data) throws Exception {
        Instance instance = upenaStore.instances.get(new InstanceKey(instanceKey));
        ProxyAsNeeded proxy = null;
        if (instance == null) {
            data.put("result", Collections.singletonList("There is no instance for key:" + instanceKey));
        } else {
            Host host = upenaStore.hosts.get(instance.hostKey);
            if (host == null) {
                data.put("result", Collections.singletonList("There is no host for key:" + instance.hostKey));
            } else if (instance.hostKey.equals(hostKey)) {
                proxy = new Local(instanceKey, instance);
            } else {
                proxy = new Proxied(instanceKey, host.name);
            }
        }
        return proxy;
    }

    @Override
    public String render(String user, ManagedDeployablePluginRegionInput input) {
        SecurityUtils.getSubject().checkPermission("debug");

        Map<String, Object> data = Maps.newHashMap();
        data.put("instanceKey", input.instanceKey);
        data.put("action", input.action);

        try {
            ProxyAsNeeded proxy = buildProxy(input.instanceKey, data);

            if (proxy != null) {
                Instance instance = upenaStore.instances.get(new InstanceKey(input.instanceKey));
                Host host = upenaStore.hosts.get(instance.hostKey);
                Instance.Port managePort = instance.ports.get("manage");

                HasUI hasUI = proxy.get("/manage/hasUI", HasUI.class, null);
                List<Map<String, String>> uis = Lists.newArrayList();
                int uiId = 0;
                if (hasUI != null && hasUI.uis != null) {
                    for (HasUI.UI ui : hasUI.uis) {
                        Instance.Port port = instance.ports.get(ui.portName);
                        if (port != null) {
                            if (managePort != null && managePort.port != port.port) {
                                Map<String, String> u = new HashMap<>();
                                u.put("id", String.valueOf(uiId));
                                u.put("name", ui.name);
                                u.put("scheme", (port.sslEnabled) ? "https" : "http");
                                u.put("host", host.name);
                                u.put("port", String.valueOf(port.port));
                                u.put("url", ui.url);
                                u.put("uiPath", "/ui/deployable/redirect/" + input.instanceKey + "?portName=" + ui.portName + "&path=" + ui.url);
                                uis.add(u);
                            }
                        }
                        uiId++;
                    }
                }
                data.put("uis", uis);

                if (input.action.equals("health")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/health/ui");
                    return renderHtml(r);
                } else if (input.action.equals("metrics")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/metrics/ui");
                    return renderHtml(r);
                } else if (input.action.equals("tail")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/tail");
                    return renderText(r);
                } else if (input.action.equals("threadDump")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/threadDump");
                    StringBuilder sb = new StringBuilder();
                    if (r != null) {
                        String[] lines = r.split("\\r?\\n");
                        sb.append("<pre class=\"monospace\">");
                        for (String l : lines) {
                            if (l.trim().startsWith("at ")) {
                                sb.append(l);

                                try {
                                    String instanceKey = input.instanceKey;
                                    String[] split = l.trim().split("\\(");

                                    String className = split[0].substring(3, split[0].lastIndexOf('.'));
                                    if (!split[1].contains("Native")) {
                                        String lineNumber = split[1].substring(split[1].lastIndexOf(':') + 1, split[1].lastIndexOf(')'));

                                        sb.append("<form action=\"/ui/breakpoint\" style=\"display: inline;\" method=\"post\" >");
                                        sb.append("<input type=\"hidden\" name=\"instanceKey\" value=\"" + instanceKey + "\"/>");
                                        sb.append("<input type=\"hidden\" name=\"className\" value=\"" + className + "\"/>");
                                        sb.append("<input type=\"hidden\" name=\"lineNumber\" value=\"" + lineNumber + "\"/>");
                                        sb.append("<button style=\"display: inline;\" title=\"filter\" type=\"submit\" name=\"action\" value=\"addConnections\" " +
                                            "class=\"btn btn-success btn-xs ladda-button\" data-spinner-color=\"#222\" data-style=\"expand-right\">");
                                        sb.append("<span class=\"fa fa-bug\"></span>");
                                        sb.append("</button>");
                                        sb.append("</form>");
                                    }
                                } catch (Exception x) {
                                    LOG.error("Failed find expected class and line in " + l);
                                }

                                sb.append("\n");
                            } else {
                                sb.append(l);
                                sb.append("\n");
                            }
                        }
                        sb.append("</pre>");
                    }

                    return renderHtml(sb.toString());
                } else if (input.action.equals("forceGC")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/forceGC");
                    return renderText(r);
                } else if (input.action.equals("resetErrors")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/resetErrors");
                    return renderText(r);
                } else if (input.action.equals("resetThrown")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/resetThrown");
                    return renderText(r);
                } else if (input.action.equals("resetHealth")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/resetHealth");
                    return renderText(r);
                } else if (input.action.equals("routes")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/tenant/routing/report");
                    return renderText(r);
                } else if (input.action.equals("logLevel")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    String r = proxy.get("/manage/logLevels/" + input.instanceKey);
                    return renderHtml(r);
                }
            }
        } catch (AuthorizationException x) {
            throw x;
        } catch (Exception x) {
            LOG.error("Unable to retrieve data", x);
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("result", "Error while trying to " + input.action + "\n" + trace);
            return "<h3>Error in embedded probe. Check the logs.</h3>";
        }

        return renderer.render(template, data);
    }

    public String setLogLevel(String instanceKey, String loggerName, String loggerLevel) throws Exception {
        ProxyAsNeeded proxy = buildProxy(instanceKey, new HashMap<>());
        if (proxy != null) {
            SecurityUtils.getSubject().checkPermission("read");
            String r = proxy.get("/manage/logging/setLogLevel?logger=" + loggerName + "&level=" + loggerLevel);
            return renderText(r);
        }
        return null;
    }

    public String renderHtml(String html) {
        return renderer.render("soy.upena.page.deployablePluginRegionHtml", ImmutableMap.of("htmlResult", (html == null ? "" : html)));
    }

    public String renderText(String text) {
        return renderer.render("soy.upena.page.deployablePluginRegionText", ImmutableMap.of("textResult", (text == null ? "" : text)));
    }

    @Override
    public String getTitle() {
        return "Java Deployable";
    }

    class Local implements ProxyAsNeeded {
        private final String instanceKey;
        HttpRequestHelper requestHelper;

        Local(String instanceKey, Instance instance) throws Exception {
            this.instanceKey = instanceKey;
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

        @Override
        public String allocateAccessToken() {
            return sessionStore.generateAccessToken(instanceKey);
        }
    }

    class Proxied implements ProxyAsNeeded {
        private final String instanceKey;
        private final HttpRequestHelper requestHelper;

        Proxied(String instanceKey, String host) throws Exception {
            this.instanceKey = instanceKey;
            this.requestHelper = HttpRequestHelperUtils.buildRequestHelper(upenaSSLConfig.sslEnable,
                upenaSSLConfig.allowSelfSignedCerts,
                upenaSSLConfig.signer,
                host,
                upenaPort);
        }

        @Override
        public String get(String path) {
            byte[] r = requestHelper.executeRequest(new LoopbackGet(path), "/upena/deployable/loopback/" + instanceKey, null);
            return r == null ? null : new String(r);
        }

        @Override
        public <J> J get(String path, Class<J> c, J d) {
            return requestHelper.executeRequest(new LoopbackGet(path), "/upena/deployable/loopback/" + instanceKey, c, d);
        }

        @Override
        public String allocateAccessToken() {
            byte[] token = requestHelper.executeGet("/upena/deployable/accessToken/" + instanceKey);
            return (token != null) ? new String(token, StandardCharsets.UTF_8) : null;
        }
    }

    interface ProxyAsNeeded {
        String get(String path);

        <J> J get(String path, Class<J> c, J d);

        String allocateAccessToken();
    }

}
