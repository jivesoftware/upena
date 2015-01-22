/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.upena.deployable;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.amza.shared.RingHost;
import com.jivesoftware.os.jive.utils.http.client.HttpClient;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfig;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfiguration;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactory;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.jive.utils.http.client.rest.RequestHelper;
import com.jivesoftware.os.jive.utils.jaxrs.util.ResponseHelper;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterFilter;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostFilter;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceFilter;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import com.jivesoftware.os.upena.uba.service.Nanny;
import com.jivesoftware.os.upena.uba.service.UbaService;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.rendersnake.HtmlAttributes;
import org.rendersnake.HtmlAttributesFactory;
import org.rendersnake.HtmlCanvas;

/**
 *
 * @author jonathan.colt
 */
@Singleton
@Path("/")
public class UpenaHealthEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final UpenaService upenaService;
    private final UbaService ubaService;
    private final RingHost ringHost;

    public UpenaHealthEndpoints(@Context AmzaInstance amzaInstance,
        @Context UpenaStore upenaStore,
        @Context UpenaService upenaService,
        @Context UbaService ubaService,
        @Context RingHost ringHost) {
        this.amzaInstance = amzaInstance;
        this.upenaStore = upenaStore;
        this.upenaService = upenaService;
        this.ubaService = ubaService;
        this.ringHost = ringHost;
    }

    @GET
    @Consumes("application/json")
    @Path("/")
    public Response getHome(@Context UriInfo uriInfo) {
        try {
            final HtmlCanvas h = new HtmlCanvas();
            h.html();
            h.body(bodyStyle);

            addHeader(uriInfo, 0, h);
            addFooter(h);
            h._body();
            h._html();

            return Response.ok(h.toHtml(), MediaType.TEXT_HTML).build();
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    void addHeader(UriInfo uriInfo, int minus, HtmlCanvas h) throws IOException {
        List<PathSegment> segments = uriInfo.getPathSegments();
        String rootPath = Joiner.on("/").join(segments.subList(0, segments.size() + minus));
        h.button(HtmlAttributesFactory.onClick("location.href='" + rootPath + "health'")).content("Health");
        h.button(HtmlAttributesFactory.onClick("location.href='" + rootPath + "instances'")).content("Instances");
        h.button(HtmlAttributesFactory.onClick("location.href='" + rootPath + "clusters'")).content("Clusters");
        h.button(HtmlAttributesFactory.onClick("location.href='" + rootPath + "releases'")).content("Releases");
        h.button(HtmlAttributesFactory.onClick("location.href='" + rootPath + "hosts'")).content("Hosts");
        h.button(HtmlAttributesFactory.onClick("location.href='" + rootPath + "services'")).content("Services");
        h.button(HtmlAttributesFactory.onClick("location.href='" + rootPath + "config'")).content("Config");
        h.button(HtmlAttributesFactory.onClick("location.href='" + rootPath + "admin'")).content("Admin");
    }

    void addFooter(HtmlCanvas h) throws IOException {
        h.footer();
        h.button(HtmlAttributesFactory.onClick("location.href='https://github.com/jivesoftware/upena'")).content("About Upena");
        h._footer();
    }

    HtmlAttributes border = HtmlAttributesFactory.style("border: 1px solid  gray; margin: 8px; padding: 4px");

    @GET
    @Consumes("application/json")
    @Path("/admin")
    public Response getAdmin(@Context UriInfo uriInfo) {
        try {
            final HtmlCanvas h = new HtmlCanvas();
            h.html();

            h.body(bodyStyle);
            addHeader(uriInfo, -1, h);
            h.h1().content("Admin");

            h.table(border);
            h.tr(HtmlAttributesFactory.style("background-color:#bbbbbb;"));
            h.td(border).content(String.valueOf("Host"));
            h.td(border).content(String.valueOf("Port"));
            h.td(border).content(String.valueOf("Action"));
            h._tr();

            for (RingHost host : amzaInstance.getRing("master")) {
                h.tr();
                h.td(border).content(host.getHost());
                h.td(border).content(host.getPort());
                h.td(border);
                h.button(HtmlAttributesFactory.onClick("location.href='" + uriInfo.getAbsolutePath() + "services'")).content("Remove");
                h._td();
                h._tr();
            }

            h._table();

            h.br();

            h.form(HtmlAttributesFactory.action(uriInfo.getBaseUri().getPath() + "add/host").method("get").id("addHost-form"));
            h.fieldset();
            h.label(HtmlAttributesFactory.for_("hostname")).write("Username")._label()
                .input(HtmlAttributesFactory.id("hostname").name("hostname").value(""));
            h.label(HtmlAttributesFactory.for_("port")).write("port")._label()
                .input(HtmlAttributesFactory.id("port").name("port").value("1175"));
            h.input(HtmlAttributesFactory.type("submit").value("Add"))
                ._fieldset()
                ._form();

            h.br();
            addFooter(h);
            h._body();
            h._html();

            return Response.ok(h.toHtml(), MediaType.TEXT_HTML).build();
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/clusters")
    public Response getCluster(@Context UriInfo uriInfo) {
        try {
            final HtmlCanvas h = new HtmlCanvas();
            h.html();

            h.body(bodyStyle);
            addHeader(uriInfo, -1, h);
            h.h1().content("Clusters");

            ClusterFilter filter = new ClusterFilter(null, null, 0, 10000);

            h.table(border);
            h.tr(HtmlAttributesFactory.style("background-color:#bbbbbb;"));
            h.td(border).content(String.valueOf("Key"));
            h.td(border).content(String.valueOf("Name"));
            h.td(border).content(String.valueOf("Description"));
            h.td(border).content(String.valueOf("Action"));
            h._tr();

            Map<ClusterKey, TimestampedValue<Cluster>> found = upenaStore.clusters.find(filter);
            for (Entry<ClusterKey, TimestampedValue<Cluster>> entrySet : found.entrySet()) {
                ClusterKey key = entrySet.getKey();
                TimestampedValue<Cluster> timestampedValue = entrySet.getValue();
                Cluster value = timestampedValue.getValue();
                h.tr();
                h.td(HtmlAttributesFactory.style("background-color:#bbbbbb;")).content(key.getKey());
                h.td(border).content(value.name);
                h.td(border).content(value.description);
                h.td(border);
                h.button(HtmlAttributesFactory.onClick("location.href='" + uriInfo.getAbsolutePath() + "services'")).content("TODO");
                h._td();
                h._tr();
            }

            h._table();
            addFooter(h);
            h._body();
            h._html();

            return Response.ok(h.toHtml(), MediaType.TEXT_HTML).build();
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/services")
    public Response getServices(@Context UriInfo uriInfo) {
        try {
            final HtmlCanvas h = new HtmlCanvas();
            h.html();
            h.body(bodyStyle);

            addHeader(uriInfo, -1, h);
            h.h1().content("Services");

            h.table(border);
            h.tr(HtmlAttributesFactory.style("background-color:#bbbbbb;"));
            h.td(border).content(String.valueOf("Key"));
            h.td(border).content(String.valueOf("Name"));
            h.td(border).content(String.valueOf("Description"));
            h.td(border).content(String.valueOf("Action"));
            h._tr();

            ServiceFilter filter = new ServiceFilter(null, null, 0, 10000);
            Map<ServiceKey, TimestampedValue<Service>> found = upenaStore.services.find(filter);
            for (Entry<ServiceKey, TimestampedValue<Service>> entrySet : found.entrySet()) {
                ServiceKey key = entrySet.getKey();
                TimestampedValue<Service> timestampedValue = entrySet.getValue();
                Service value = timestampedValue.getValue();
                h.tr();
                h.td(HtmlAttributesFactory.style("background-color:#bbbbbb;")).content(key.getKey());
                h.td(border).content(value.name);
                h.td(border).content(value.description);
                h.td(border);
                h.button(HtmlAttributesFactory.onClick("location.href='" + uriInfo.getAbsolutePath() + "services'")).content("TODO");
                h._td();
                h._tr();
            }

            h._table();

            addFooter(h);
            h._body();
            h._html();

            return Response.ok(h.toHtml(), MediaType.TEXT_HTML).build();
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/releases")
    public Response getReleases(@Context UriInfo uriInfo) {
        try {
            final HtmlCanvas h = new HtmlCanvas();
            h.html();
            h.body(bodyStyle);

            addHeader(uriInfo, -1, h);
            h.h1().content("Releases");

            h.table(border);
            h.tr(HtmlAttributesFactory.style("background-color:#bbbbbb;"));
            h.td(border).content(String.valueOf("Key"));
            h.td(border).content(String.valueOf("Version"));
            h.td(border).content(String.valueOf("Name"));
            h.td(border).content(String.valueOf("Description"));
            h.td(border).content(String.valueOf("Repository"));
            h.td(border).content(String.valueOf("Email"));
            h.td(border).content(String.valueOf("Action"));
            h._tr();

            ReleaseGroupFilter filter = new ReleaseGroupFilter(null, null, null, null, null, 0, 10000);
            Map<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> found = upenaStore.releaseGroups.find(filter);
            for (Entry<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> entrySet : found.entrySet()) {
                ReleaseGroupKey key = entrySet.getKey();
                TimestampedValue<ReleaseGroup> timestampedValue = entrySet.getValue();
                ReleaseGroup value = timestampedValue.getValue();
                h.tr();
                h.td(HtmlAttributesFactory.style("background-color:#bbbbbb;")).content(key.getKey());
                h.td(border).content(value.version);
                h.td(border).content(value.name);
                h.td(border).content(value.description);
                h.td(border).content(value.repository);
                h.td(border).content(value.email);
                h.td(border);
                h.button(HtmlAttributesFactory.onClick("location.href='" + uriInfo.getAbsolutePath() + "services'")).content("TODO");
                h._td();
                h._tr();
            }

            h._table();

            addFooter(h);
            h._body();
            h._html();

            return Response.ok(h.toHtml(), MediaType.TEXT_HTML).build();
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/hosts")
    public Response getHosts(@Context UriInfo uriInfo) {
        try {
            final HtmlCanvas h = new HtmlCanvas();
            h.html();
            h.body(bodyStyle);

            addHeader(uriInfo, -1, h);
            h.h1().content("Hosts");

            h.table(border);
            h.tr(HtmlAttributesFactory.style("background-color:#bbbbbb;"));
            h.td(border).content(String.valueOf("Key"));
            h.td(border).content(String.valueOf("Hostname"));
            h.td(border).content(String.valueOf("Port"));
            h.td(border).content(String.valueOf("Dir"));
            h.td(border).content(String.valueOf("Name"));
            h.td(border).content(String.valueOf("Action"));
            h._tr();

            HostFilter filter = new HostFilter(null, null, null, null, null, 0, 10000);
            Map<HostKey, TimestampedValue<Host>> found = upenaStore.hosts.find(filter);
            for (Entry<HostKey, TimestampedValue<Host>> entrySet : found.entrySet()) {
                HostKey key = entrySet.getKey();
                TimestampedValue<Host> timestampedValue = entrySet.getValue();
                Host value = timestampedValue.getValue();
                h.tr();
                h.td(HtmlAttributesFactory.style("background-color:#bbbbbb;")).content(key.getKey());
                h.td(border).content(value.hostName);
                h.td(border).content(value.port);
                h.td(border).content(value.workingDirectory);
                h.td(border).content(value.name);
                h.td(border);
                h.button(HtmlAttributesFactory.onClick("location.href='" + uriInfo.getAbsolutePath() + "services'")).content("TODO");
                h._td();
                h._tr();
            }

            h._table();

            addFooter(h);
            h._body();
            h._html();

            return Response.ok(h.toHtml(), MediaType.TEXT_HTML).build();
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/instances")
    public Response getInstances(@Context UriInfo uriInfo) {
        try {
            final HtmlCanvas h = new HtmlCanvas();
            h.html();
            h.body(bodyStyle);

            addHeader(uriInfo, -1, h);
            h.h1().content("Instance");

            h.table(border);
            h.tr(HtmlAttributesFactory.style("background-color:#bbbbbb;"));
            h.td(border).content(String.valueOf("Key"));
            h.td(border).content(String.valueOf("Id"));
            h.td(border).content(String.valueOf("Cluster"));
            h.td(border).content(String.valueOf("Host"));
            h.td(border).content(String.valueOf("Service"));
            h.td(border).content(String.valueOf("Release"));
            h.td(border).content(String.valueOf("Action"));
            h._tr();

            InstanceFilter filter = new InstanceFilter(null, null, null, null, null, 0, 10000);
            Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
            for (Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
                InstanceKey key = entrySet.getKey();
                TimestampedValue<Instance> timestampedValue = entrySet.getValue();
                Instance value = timestampedValue.getValue();
                h.tr();
                h.td(HtmlAttributesFactory.style("background-color:#bbbbbb;")).content(key.getKey());
                h.td(border).content(value.instanceId);
                h.td(border).content(upenaStore.clusters.get(value.clusterKey).name);
                h.td(border).content(upenaStore.hosts.get(value.hostKey).name);
                h.td(border).content(upenaStore.services.get(value.serviceKey).name);
                h.td(border).content(upenaStore.releaseGroups.get(value.releaseGroupKey).name);
                h.td(border);
                h.button(HtmlAttributesFactory.onClick("location.href='" + uriInfo.getAbsolutePath() + "services'")).content("TODO");
                h._td();
                h._tr();
            }

            h._table();

            addFooter(h);
            h._body();
            h._html();

            return Response.ok(h.toHtml(), MediaType.TEXT_HTML).build();
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    private static final String roundBorder = "-moz-border-radius: 4px;"
        + "-webkit-border-radius: 4px;"
        + "border-radius: 4px;";

    private String coloredStopLightButton(double rank, float sat) {
        return roundBorder + "display: block;"
            //            + "  width: 100%;"
            //            + "  height: 25px;"
            + "  background-color: #" + getHEXTrafficlightColor(rank, sat) + ";"
            + "  padding: 3px;"
            + "  text-align: center;"
            + "  color: gray;"
            + "  border: 1px solid gray;";
    }

    @GET
    @Consumes("application/json")
    @Path("/health")
    public Response getHtml(@Context UriInfo uriInfo) {
        try {
            final HtmlCanvas h = new HtmlCanvas();
            h.html();
            h.body(bodyStyle);
            addHeader(uriInfo, -1, h);
            h.h1().content("Health");

            ClusterHealth clusterHealth = buildClusterHealth(uriInfo);
            h.table(HtmlAttributesFactory.style(roundBorder + "text-align:center;"
                + " border: 1px outset gray;"
                + "background-color: #" + getHEXTrafficlightColor(clusterHealth.health, 1.0F) + ";"
                + "padding: 5px;margin: 5px;"));

            int nodeCount = 0;
            h.tr();
            for (NodeHealth nodeHealth : clusterHealth.nodeHealths) {
                nodeCount++;
                if (nodeCount % 7 == 0) {
                    h._tr();
                    h.tr();
                }

                h.td(HtmlAttributesFactory.style("text-align:center; border: 1px solid gray;"
                    + "background-color: #" + getHEXTrafficlightColor(nodeHealth.health, 1f) + ";"
                    + "padding: 5px;margin: 5px;"));
                if (nodeHealth.nannyHealths.isEmpty()) {
                    h.table(HtmlAttributesFactory.style("text-align:center; border: 1px solid gray;"
                        + "background-color: #" + getHEXTrafficlightColor(nodeHealth.health, 1f) + ";"
                        + "padding: 5px;margin: 5px;"));
                    h._table();
                } else {
                    for (NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                        if (nannyHealth.serviceHealth != null) {
                            h.table(HtmlAttributesFactory.style("text-align:center; border: 1px solid gray;"
                                + "background-color: #" + getHEXTrafficlightColor(nannyHealth.serviceHealth.health, 0.95f) + ";"
                                + "padding: 5px;margin: 5px;"));

                            if (nannyHealth.serviceHealth.healthChecks.isEmpty()) {
                                h.tr();
                                h.td();
                                InstanceDescriptor id = nannyHealth.instanceDescriptor;
                                String title = "(" + nannyHealth.serviceHealth.health + ") for "
                                    + id.clusterName + " " + id.serviceName + " " + id.instanceName + " " + id.versionName;

                                h.div(HtmlAttributesFactory.style("text-align:center;vertical-align:middle;").title(title))
                                    .a(HtmlAttributesFactory
                                        .href("http://" + nodeHealth.host + ":" + nannyHealth.instanceDescriptor.ports.get("manage").port + "/manage/ui")
                                        .style(coloredStopLightButton(nannyHealth.serviceHealth.health, 1.0F)))
                                    .content(nannyHealth.instanceDescriptor.serviceName).content("");
                                h._td();
                                h._tr();
                            } else {
                                for (Health health : nannyHealth.serviceHealth.healthChecks) {
                                    h.tr();
                                    h.td();
                                    InstanceDescriptor id = nannyHealth.instanceDescriptor;
                                    String title = "(" + health.health + ") " + health.name + " for "
                                        + id.clusterName + " " + id.serviceName + " " + id.instanceName + " " + id.versionName;

                                    h.div(HtmlAttributesFactory.style("text-align:center;vertical-align:middle;").title(title))
                                        .a(HtmlAttributesFactory
                                            .href("http://" + nodeHealth.host + ":" + nannyHealth.instanceDescriptor.ports.get("manage").port + "/manage/ui")
                                            .style(coloredStopLightButton(health.health, 1.0F))).content(health.name).content("");
                                    h._td();
                                    h._tr();
                                }
                            }
                            h._table();
                        }
                    }
                }
                h._td();

            }
            h._tr();

            h._table();
            h.br();

            h.div()
                .div(colorStyle("background-color", clusterHealth.health))
                .content(String.valueOf(clusterHealth.health))
                .h1()
                .content("Cluster")
                .content("");

            h.ol(HtmlAttributesFactory.style("list-style-type:none;"));

            for (NodeHealth nodeHealth : clusterHealth.nodeHealths) {
                h.li();
                addNodeHealth(h, nodeHealth);
                h._li();
            }
            h._ol();
            addFooter(h);
            h._body();
            h._html();

            return Response.ok(h.toHtml(), MediaType.TEXT_HTML).build();
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }
    private final HtmlAttributes bodyStyle = HtmlAttributesFactory
        .style("background:#ffffff "
            //            + "url(http://www.wallchan.com/images/sandbox/1305883948-water-wave-wallpaper.jpg) no-repeat center center fixed;"
            + "  -webkit-background-size: cover;"
            + "  -moz-background-size: cover;"
            + "  -o-background-size: cover;"
            + "  background-size: cover;");

    private void addNodeHealth(HtmlCanvas canvas, NodeHealth nodeHealth) throws Exception {

        canvas.div()
            .div(colorStyle("background-color", nodeHealth.health))
            .content(String.valueOf(nodeHealth.health))
            .h2()
            .content(nodeHealth.host + ":" + nodeHealth.port)
            .content("");

        canvas.ol(HtmlAttributesFactory.style("list-style-type:none;"));
        for (NannyHealth nannyHealth : nodeHealth.nannyHealths) {
            canvas.li();
            addNannyHealth(canvas, nodeHealth, nannyHealth);
            canvas._li();
        }
        canvas._ol();
    }

    private void addNannyHealth(HtmlCanvas canvas, NodeHealth nodeHealth, NannyHealth nannyHealth) throws Exception {
        InstanceDescriptor id = nannyHealth.instanceDescriptor;
        ServiceHealth serviceHealth = nannyHealth.serviceHealth;

        canvas.div()
            .div(colorStyle("background-color", (serviceHealth == null) ? 0.0d : serviceHealth.health))
            .content(String.valueOf((serviceHealth == null) ? 0.0d : serviceHealth.health))
            .h3()
            .a(HtmlAttributesFactory.href("http://" + nodeHealth.host + ":" + nannyHealth.instanceDescriptor.ports.get("manage").port + "/manage/ui"))
            .content(id.clusterName + " " + id.serviceName + " " + id.instanceName + " " + id.versionName)
            ._h3()
            .content("");
        //

        StringBuilder sb = new StringBuilder();
        for (Entry<String, InstanceDescriptor.InstanceDescriptorPort> port : id.ports.entrySet()) {
            sb.append(port.getKey()).append("=").append(port.getValue().port).append(" ");
        }

        canvas.ol(HtmlAttributesFactory.style("list-style-type:none;"));
        canvas.pre().content(sb.toString());
        canvas.pre().content(Joiner.on("\n").join(nannyHealth.log));

        if (serviceHealth != null) {

            HtmlAttributes border = HtmlAttributesFactory.style("border: 1px solid  gray;");
            canvas.table(border);
            canvas.tr(HtmlAttributesFactory.style("background-color:#bbbbbb;"));
            canvas.td(border).content(String.valueOf("Health"));
            canvas.td(border).content(String.valueOf("Name"));
            canvas.td(border).content(String.valueOf("Status"));
            canvas.td(border).content(String.valueOf("Description"));
            canvas.td(border).content(String.valueOf("Resolution"));
            canvas.td(border).content(String.valueOf("Age in millis"));
            canvas._tr();
            for (Health health : serviceHealth.healthChecks) {
                if (-Double.MAX_VALUE != health.health) {
                    canvas.tr();
                    canvas.td(colorStyle("background-color", health.health)).content(String.valueOf(health.health));
                    canvas.td(border).content(String.valueOf(health.name));
                    canvas.td(border).content(String.valueOf(health.status));
                    canvas.td(border).content(String.valueOf(health.description));
                    canvas.td(border).content(String.valueOf(health.resolution));
                    long ageInMillis = System.currentTimeMillis() - health.timestamp;
                    double ageHealth = 0.0d;
                    if (health.checkIntervalMillis > 0) {
                        ageHealth = 1d - (ageInMillis / health.checkIntervalMillis);
                    }
                    canvas.td(colorStyle("background-color", ageHealth)).content(String.valueOf(ageInMillis));
                    canvas._tr();
                }
            }
            canvas._table();

        }
        canvas._ol();
    }

    HtmlAttributes colorStyle(String key, double health) {
        return HtmlAttributesFactory.style(key + ":#" + getHEXTrafficlightColor(health, 1.0F) + ";float: left;padding: 8px;margin: 8px;" + roundBorder);
    }

    String getHEXTrafficlightColor(double value, float sat) {
        //String s = Integer.toHexString(Color.HSBtoRGB(0.6f, 1f - ((float) value), sat) & 0xffffff);
        String s = Integer.toHexString(Color.HSBtoRGB((float) value / 3f, sat, 1f) & 0xffffff);
        return "000000".substring(s.length()) + s;
    }

    @GET
    @Consumes("application/json")
    @Path("/health/instance")
    public Response getInstanceHealth() {
        try {
            NodeHealth upenaHealth = buildNodeHealth();
            return ResponseHelper.INSTANCE.jsonResponse(upenaHealth);
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/health/cluster")
    public Response getClusterHealth(@Context UriInfo uriInfo) {
        try {
            ClusterHealth clusterHealth = buildClusterHealth(uriInfo);
            return ResponseHelper.INSTANCE.jsonResponse(clusterHealth);
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    private ClusterHealth buildClusterHealth(UriInfo uriInfo) throws Exception {
        ClusterHealth clusterHealth = new ClusterHealth();
        for (RingHost ringHost : new RingHost[]{
            new RingHost("soa-prime-data5.phx1.jivehosted.com", 1175),
            new RingHost("soa-prime-data6.phx1.jivehosted.com", 1175),
            new RingHost("soa-prime-data7.phx1.jivehosted.com", 1175),
            new RingHost("soa-prime-data8.phx1.jivehosted.com", 1175),
            new RingHost("soa-prime-data9.phx1.jivehosted.com", 1175),
            new RingHost("soa-prime-data10.phx1.jivehosted.com", 1175)
        }) { //amzaInstance.getRing("MASTER")) {
            try {
                RequestHelper requestHelper = buildRequestHelper(ringHost.getHost(), ringHost.getPort());
                String path = Joiner.on("/").join(uriInfo.getPathSegments().subList(0, uriInfo.getPathSegments().size()));
                NodeHealth nodeHealth = requestHelper.executeGetRequest("/" + path + "/instance", NodeHealth.class, null);
                clusterHealth.health = Math.min(nodeHealth.health, clusterHealth.health);
                clusterHealth.nodeHealths.add(nodeHealth);
            } catch (Exception x) {
                clusterHealth.health = 0.0d;
                NodeHealth nodeHealth = new NodeHealth(ringHost.getHost(), ringHost.getPort());
                nodeHealth.health = 0.0d;
                nodeHealth.nannyHealths = new ArrayList<>();
                clusterHealth.nodeHealths.add(nodeHealth);
                LOG.warn("Failed getting cluster health for " + ringHost, x);
            }
        }
        return clusterHealth;
    }

    RequestHelper buildRequestHelper(String host, int port) {
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().setSocketTimeoutInMillis(10000).build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider()
            .createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(host, port);
        RequestHelper requestHelper = new RequestHelper(httpClient, new ObjectMapper());
        return requestHelper;
    }

    private NodeHealth buildNodeHealth() {
        NodeHealth nodeHealth = new NodeHealth(ringHost.getHost(), ringHost.getPort());
        for (Entry<String, Nanny> nanny : ubaService.iterateNannies()) {
            Nanny n = nanny.getValue();
            InstanceDescriptor id = n.getInstanceDescriptor();
            List<String> log = n.getDeployLog().commitedLog();
            List<String> copyLog = n.getHealthLog().commitedLog();
            ServiceHealth serviceHealth = null;
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                serviceHealth = mapper.readValue(Joiner.on("").join(copyLog), ServiceHealth.class);
                nodeHealth.health = Math.min(nodeHealth.health, serviceHealth.health);
            } catch (Exception x) {
                LOG.warn("Failed parsing service health for " + id + " " + Joiner.on("").join(copyLog), x);
                nodeHealth.health = 0.0d;
                log.add("Failed to parse serviceHealth" + x.getMessage());
            }
            NannyHealth nannyHealth = new NannyHealth(id, log, serviceHealth);
            nodeHealth.nannyHealths.add(nannyHealth);

        }
        return nodeHealth;
    }

    static public class ClusterHealth {

        public double health = 1d;
        public List<NodeHealth> nodeHealths = new ArrayList<>();
    }

    static public class NodeHealth {

        public double health = 1d;
        public String host;
        public int port;
        public List<NannyHealth> nannyHealths = new ArrayList<>();

        public NodeHealth() {
        }

        public NodeHealth(String host, int port) {
            this.host = host;
            this.port = port;
        }

    }

    static public class NannyHealth {

        public InstanceDescriptor instanceDescriptor;
        public List<String> log;
        public ServiceHealth serviceHealth;

        public NannyHealth() {
        }

        public NannyHealth(InstanceDescriptor instanceDescriptor, List<String> log, ServiceHealth serviceHealth) {
            this.instanceDescriptor = instanceDescriptor;
            this.log = log;
            this.serviceHealth = serviceHealth;
        }

    }

    static public class ServiceHealth {

        public double health = 1.0d;
        public List<Health> healthChecks = new ArrayList<>();
    }

    static public class Health {

        public String name;
        public double health;
        public String status;
        public String description;
        public String resolution;
        public long timestamp;
        public long checkIntervalMillis;
    }

}
