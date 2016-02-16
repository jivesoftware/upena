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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.base.SoySyntaxException;
import com.google.template.soy.tofu.SoyTofu;
import com.jivesoftware.os.amza.service.AmzaService;
import com.jivesoftware.os.amza.service.AmzaServiceInitializer;
import com.jivesoftware.os.amza.service.AmzaServiceInitializer.AmzaServiceConfig;
import com.jivesoftware.os.amza.service.discovery.AmzaDiscovery;
import com.jivesoftware.os.amza.service.storage.replication.SendFailureListener;
import com.jivesoftware.os.amza.service.storage.replication.TakeFailureListener;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.amza.shared.MemoryRowsIndex;
import com.jivesoftware.os.amza.shared.RingHost;
import com.jivesoftware.os.amza.shared.RowIndexKey;
import com.jivesoftware.os.amza.shared.RowIndexValue;
import com.jivesoftware.os.amza.shared.RowsIndexProvider;
import com.jivesoftware.os.amza.shared.RowsStorageProvider;
import com.jivesoftware.os.amza.shared.UpdatesSender;
import com.jivesoftware.os.amza.shared.UpdatesTaker;
import com.jivesoftware.os.amza.storage.RowTable;
import com.jivesoftware.os.amza.storage.binary.BinaryRowMarshaller;
import com.jivesoftware.os.amza.storage.binary.BinaryRowsTx;
import com.jivesoftware.os.amza.transport.http.replication.HttpUpdatesSender;
import com.jivesoftware.os.amza.transport.http.replication.HttpUpdatesTaker;
import com.jivesoftware.os.amza.transport.http.replication.endpoints.AmzaReplicationRestEndpoints;
import com.jivesoftware.os.jive.utils.ordered.id.ConstantWriterIdProvider;
import com.jivesoftware.os.jive.utils.ordered.id.OrderIdProvider;
import com.jivesoftware.os.jive.utils.ordered.id.OrderIdProviderImpl;
import com.jivesoftware.os.jive.utils.ordered.id.TimestampedOrderIdProvider;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.config.UpenaConfigRestEndpoints;
import com.jivesoftware.os.upena.config.UpenaConfigStore;
import com.jivesoftware.os.upena.deployable.UpenaEndpoints.AmzaClusterName;
import com.jivesoftware.os.upena.deployable.endpoints.AsyncLookupEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ChangeLogPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ClustersPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ConfigPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ConnectivityPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.DependenciesPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.HealthPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.HostsPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.InstancesPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ModulesPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ProfilerPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ProjectsPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ReleasesPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.SARPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ServicesPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.TopologyPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.UpenaRingPluginEndpoints;
import com.jivesoftware.os.upena.deployable.lookup.AsyncLookupService;
import com.jivesoftware.os.upena.deployable.profiler.model.ServicesCallDepthStack;
import com.jivesoftware.os.upena.deployable.profiler.server.endpoints.PerfService;
import com.jivesoftware.os.upena.deployable.profiler.server.endpoints.PerfServiceEndpoint;
import com.jivesoftware.os.upena.deployable.profiler.visualize.NameUtils;
import com.jivesoftware.os.upena.deployable.profiler.visualize.VisualizeProfile;
import com.jivesoftware.os.upena.deployable.region.ChangeLogPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ClustersPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ConfigPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ConnectivityPluginRegion;
import com.jivesoftware.os.upena.deployable.region.DependenciesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.HeaderRegion;
import com.jivesoftware.os.upena.deployable.region.HealthPluginRegion;
import com.jivesoftware.os.upena.deployable.region.HomeRegion;
import com.jivesoftware.os.upena.deployable.region.HostsPluginRegion;
import com.jivesoftware.os.upena.deployable.region.InstancesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ManagePlugin;
import com.jivesoftware.os.upena.deployable.region.MenuRegion;
import com.jivesoftware.os.upena.deployable.region.ModulesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ProfilerPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ProjectsPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ReleasesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.SARPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ServicesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.TopologyPluginRegion;
import com.jivesoftware.os.upena.deployable.region.UpenaRingPluginRegion;
import com.jivesoftware.os.upena.deployable.server.InitializeRestfulServer;
import com.jivesoftware.os.upena.deployable.server.JerseyEndpoints;
import com.jivesoftware.os.upena.deployable.server.RestfulServer;
import com.jivesoftware.os.upena.deployable.soy.SoyDataUtils;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import com.jivesoftware.os.upena.service.DiscoveredRoutes;
import com.jivesoftware.os.upena.service.UpenaRestEndpoints;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.uba.service.UbaLog;
import com.jivesoftware.os.upena.uba.service.UbaService;
import com.jivesoftware.os.upena.uba.service.UbaServiceInitializer;
import com.jivesoftware.os.upena.uba.service.endpoints.UbaServiceRestEndpoints;
import de.ruedigermoeller.serialization.FSTConfiguration;
import java.io.File;
import java.net.InetAddress;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.out.println("Usage:");
                System.out.println("");
                System.out.println("    java -jar upena.jar <hostName>                   (manual cluster discovery)");
                System.out.println("");
                System.out.println(" or ");
                System.out.println("");
                System.out.println("    java -jar upena.jar <hostName> <clusterName>     (automatic cluster discovery)");
                System.out.println("");
                System.out.println("Overridable properties:");
                System.out.println("");
                System.out.println("    -Dhost.rack=<rackId>");
                System.out.println("    -Dhost.datacenter=<datacenterId>");
                System.out.println("");
                System.out.println("    -Damza.port=1175");
                System.out.println("         (change the port upena uses to interact with other upena nodes.) ");
                System.out.println("");
                System.out.println("     Only applicable if you have specified a <clusterName>.");
                System.out.println("          -Damza.discovery.group=225.4.5.6");
                System.out.println("          -Damza.discovery.port=1123");
                System.out.println("");
                System.out.println("Example:");
                System.out.println("java -jar upena.jar " + InetAddress.getLocalHost().getHostName() + " dev");
                System.out.println("");
                System.exit(1);

            } else {
                new Main().run(args);
            }
        } catch (Exception x) {
            x.printStackTrace();
            System.exit(1);
        }
    }

    public void run(String[] args) throws Exception {

        String hostname = InetAddress.getLocalHost().getHostName();
        if (args != null && args.length > 0) {
            hostname = args[0];
        }
        int port = Integer.parseInt(System.getProperty("amza.port", "1175"));
        String multicastGroup = System.getProperty("amza.discovery.group", "225.4.5.6");
        int multicastPort = Integer.parseInt(System.getProperty("amza.discovery.port", "1123"));
        String clusterName = (args.length > 1 ? args[1] : null);

        String datacenter = System.getProperty("host.datacenter", "unknownDatacenter");
        String rack = System.getProperty("host.rack", "unknownRack");
        String publicHost = System.getProperty("public.host.name", hostname);

        final RingHost ringHost = new RingHost(hostname, port); // TODO include rackId
        // todo need a better way to create writter id.
        final TimestampedOrderIdProvider orderIdProvider = new OrderIdProviderImpl(new ConstantWriterIdProvider(new Random().nextInt(512)));

        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);

        final AmzaServiceConfig amzaServiceConfig = new AmzaServiceConfig();

        RowsStorageProvider rowsStorageProvider = rowsStorageProvider(orderIdProvider);

        UpdatesSender changeSetSender = new HttpUpdatesSender();
        UpdatesTaker tableTaker = new HttpUpdatesTaker();

        AmzaService amzaService = new AmzaServiceInitializer().initialize(amzaServiceConfig,
            orderIdProvider,
            new com.jivesoftware.os.amza.storage.FstMarshaller(FSTConfiguration.getDefaultConfiguration()),
            rowsStorageProvider,
            rowsStorageProvider,
            rowsStorageProvider,
            changeSetSender,
            tableTaker,
            Optional.<SendFailureListener>absent(),
            Optional.<TakeFailureListener>absent(),
            (changes) -> {
            }
        );

        amzaService.start(ringHost, amzaServiceConfig.resendReplicasIntervalInMillis,
            amzaServiceConfig.applyReplicasIntervalInMillis,
            amzaServiceConfig.takeFromNeighborsIntervalInMillis,
            amzaServiceConfig.checkIfCompactionIsNeededIntervalInMillis,
            amzaServiceConfig.compactTombstoneIfOlderThanNMillis);

        LOG.info("-----------------------------------------------------------------------");
        LOG.info("|      Amza Service Online");
        LOG.info("-----------------------------------------------------------------------");

        final UpenaConfigStore upenaConfigStore = new UpenaConfigStore(amzaService);

        LOG.info("-----------------------------------------------------------------------");
        LOG.info("|      Upena Config Store Online");
        LOG.info("-----------------------------------------------------------------------");

        final AtomicReference<UbaService> conductor = new AtomicReference<>();
        final UpenaStore upenaStore = new UpenaStore(mapper, amzaService, (instanceChanges) -> {
            Executors.newSingleThreadExecutor().submit(() -> {
                UbaService got = conductor.get();
                if (got != null) {
                    try {
                        got.instanceChanged(instanceChanges);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }, (changes) -> {
        }, (change) -> {
            LOG.info("TODO: tie into conductor. " + change);
        });
        upenaStore.attachWatchers();

        UpenaService upenaService = new UpenaService(datacenter, rack, publicHost, upenaStore);

        LOG.info("-----------------------------------------------------------------------");
        LOG.info("|      Upena Service Online");
        LOG.info("-----------------------------------------------------------------------");

        String workingDir = System.getProperty("user.dir");
        Host host = new Host(publicHost, datacenter, rack, ringHost.getHost(), ringHost.getPort(), workingDir, null);

        HostKey hostKey = upenaStore.hosts.toKey(host);
        Host gotHost = upenaStore.hosts.get(hostKey);
        if (gotHost == null || !gotHost.equals(host)) {
            upenaStore.hosts.update(hostKey, host);
        }

        UbaLog ubaLog = (what, why, how) -> {
            try {
                upenaStore.record("Uba-" + ringHost.getHost() + ":" + ringHost.getPort(), what, System.currentTimeMillis(), why, "", how);
            } catch (Exception x) {
                x.printStackTrace(); // Hmm lame
            }
        };

        final UbaService ubaService = new UbaServiceInitializer().initialize(hostKey.getKey(),
            workingDir,
            datacenter,
            rack,
            publicHost,
            ringHost.getHost(),
            ringHost.getPort(),
            ubaLog);

        DiscoveredRoutes discoveredRoutes = new DiscoveredRoutes();

        JerseyEndpoints jerseyEndpoints = new JerseyEndpoints()
            .addEndpoint(UpenaRestEndpoints.class)
            .addInjectable(upenaService)
            .addInjectable(upenaStore)
            .addEndpoint(UpenaConfigRestEndpoints.class)
            .addInjectable(upenaConfigStore)
            .addEndpoint(UbaServiceRestEndpoints.class)
            .addInjectable(ubaService)
            .addEndpoint(AmzaReplicationRestEndpoints.class)
            .addInjectable(AmzaInstance.class, amzaService)
            .addEndpoint(UpenaEndpoints.class)
            .addInjectable(DiscoveredRoutes.class, discoveredRoutes)
            .addInjectable(RingHost.class, ringHost)
            .addInjectable(HostKey.class, hostKey);

        injectUI(amzaService, hostKey, ringHost, upenaStore, upenaConfigStore, upenaService, ubaService, jerseyEndpoints, clusterName, discoveredRoutes);

        InitializeRestfulServer initializeRestfulServer = new InitializeRestfulServer(port, "UpenaNode", 128, 10000);
        initializeRestfulServer.addClasspathResource("/resources");
        initializeRestfulServer.addContextHandler("/", jerseyEndpoints);

        RestfulServer restfulServer = initializeRestfulServer.build();
        restfulServer.start();

        System.out.println("-----------------------------------------------------------------------");
        System.out.println("|      Jetty Service Online");
        System.out.println("-----------------------------------------------------------------------");

        if (ubaService != null) {
            Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(() -> {
                try {
                    ubaService.nanny();
                } catch (Exception ex) {
                    ex.printStackTrace(); // HACK
                }
            }, 15, 15, TimeUnit.SECONDS);
            System.out.println("-----------------------------------------------------------------------");
            System.out.println("|      Uba Service Online");
            System.out.println("-----------------------------------------------------------------------");
        }
        conductor.set(ubaService);

        if (clusterName != null) {
            AmzaDiscovery amzaDiscovery = new AmzaDiscovery(amzaService, ringHost, clusterName, multicastGroup, multicastPort);
            amzaDiscovery.start();
            System.out.println("-----------------------------------------------------------------------");
            System.out.println("|      Amza Service Discovery Online");
            System.out.println("-----------------------------------------------------------------------");
        } else {
            System.out.println("-----------------------------------------------------------------------");
            System.out.println("|     Amza Service is in manual Discovery mode.  No cluster name was specified");
            System.out.println("-----------------------------------------------------------------------");
        }
    }

    private void injectUI(AmzaService amzaService,
        HostKey hostKey,
        RingHost ringHost,
        UpenaStore upenaStore,
        UpenaConfigStore upenaConfigStore,
        UpenaService upenaService,
        UbaService ubaService,
        JerseyEndpoints jerseyEndpoints,
        String clusterName,
        DiscoveredRoutes discoveredRoutes) throws SoySyntaxException {

        SoyFileSet.Builder soyFileSetBuilder = new SoyFileSet.Builder();

        LOG.info("Add....");

        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/chrome.soy"), "chome.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/homeRegion.soy"), "home.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/changeLogPluginRegion.soy"), "changeLog.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/instanceHealthPluginRegion.soy"), "instanceHealthPluginRegion.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/healthPluginRegion.soy"), "health.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/topologyPluginRegion.soy"), "topology.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/connectivityPluginRegion.soy"), "connectivity.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/connectionsHealth.soy"), "connectionsHealth.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/instancesPluginRegion.soy"), "instances.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/dependenciesPluginRegion.soy"), "dependencies.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/modulesPluginRegion.soy"), "modules.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/projectsPluginRegion.soy"), "projects.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/clustersPluginRegion.soy"), "clusters.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/hostsPluginRegion.soy"), "hosts.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/servicesPluginRegion.soy"), "services.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/releasesPluginRegion.soy"), "releases.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/upenaRingPluginRegion.soy"), "upenaRing.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/configPluginRegion.soy"), "config.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/sarPluginRegion.soy"), "sarPluginRegion.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/profilerPluginRegion.soy"), "profilerPluginRegion.soy");

        SoyFileSet sfs = soyFileSetBuilder.build();
        SoyTofu tofu = sfs.compileToTofu();
        SoyRenderer renderer = new SoyRenderer(tofu, new SoyDataUtils());
        SoyService soyService = new SoyService(renderer,
            new HeaderRegion("soy.chrome.headerRegion", renderer),
            new MenuRegion("soy.chrome.menuRegion", renderer),
            new HomeRegion("soy.page.homeRegion", renderer),
            clusterName,
            hostKey,
            upenaStore
        );

        HealthPluginRegion healthPluginRegion = new HealthPluginRegion(ringHost, "soy.page.healthPluginRegion",
            "soy.page.instanceHealthPluginRegion",
            renderer,
            amzaService,
            upenaStore,
            upenaConfigStore);
        ReleasesPluginRegion releasesPluginRegion = new ReleasesPluginRegion("soy.page.releasesPluginRegion", "soy.page.releasesPluginRegionList",
            renderer, upenaStore);
        HostsPluginRegion hostsPluginRegion = new HostsPluginRegion("soy.page.hostsPluginRegion", renderer, upenaStore);
        InstancesPluginRegion instancesPluginRegion = new InstancesPluginRegion("soy.page.instancesPluginRegion",
            "soy.page.instancesPluginRegionList", renderer, upenaStore, healthPluginRegion);

        ManagePlugin health = new ManagePlugin("fire", null, "Health", "/ui/health",
            HealthPluginEndpoints.class, healthPluginRegion);

        ManagePlugin topology = new ManagePlugin("th", null, "Topology", "/ui/topology",
            TopologyPluginEndpoints.class,
            new TopologyPluginRegion("soy.page.topologyPluginRegion", "soy.page.connectionsHealth",
                renderer, amzaService, upenaStore, healthPluginRegion, hostsPluginRegion, releasesPluginRegion, instancesPluginRegion, discoveredRoutes));

        ManagePlugin connectivity = new ManagePlugin("transfer", null, "Connectivity", "/ui/connectivity",
            ConnectivityPluginEndpoints.class,
            new ConnectivityPluginRegion("soy.page.connectivityPluginRegion", "soy.page.connectionsHealth",
                renderer, amzaService, upenaStore, healthPluginRegion, hostsPluginRegion, releasesPluginRegion, instancesPluginRegion, discoveredRoutes));

        ManagePlugin changes = new ManagePlugin("road", null, "Changes", "/ui/changeLog",
            ChangeLogPluginEndpoints.class,
            new ChangeLogPluginRegion("soy.page.changeLogPluginRegion", renderer, upenaStore));

        ManagePlugin instances = new ManagePlugin(null, "instance", "Instances", "/ui/instances",
            InstancesPluginEndpoints.class, instancesPluginRegion);

        ManagePlugin config = new ManagePlugin("cog", null, "Config", "/ui/config",
            ConfigPluginEndpoints.class,
            new ConfigPluginRegion("soy.page.configPluginRegion", renderer, upenaStore, upenaConfigStore));

         ManagePlugin projects = new ManagePlugin("folder-open", null, "Projects", "/ui/projects",
            ProjectsPluginEndpoints.class,
            new ProjectsPluginRegion("soy.page.projectsPluginRegion", renderer, upenaStore));

        ManagePlugin clusters = new ManagePlugin(null, "cluster", "Clusters", "/ui/clusters",
            ClustersPluginEndpoints.class,
            new ClustersPluginRegion("soy.page.clustersPluginRegion", renderer, upenaStore));

        ManagePlugin hosts = new ManagePlugin(null, "host", "Hosts", "/ui/hosts",
            HostsPluginEndpoints.class, hostsPluginRegion);

        ManagePlugin services = new ManagePlugin(null, "service", "Services", "/ui/services",
            ServicesPluginEndpoints.class,
            new ServicesPluginRegion("soy.page.servicesPluginRegion", renderer, amzaService, upenaStore, upenaService, ubaService, ringHost));

        ManagePlugin releases = new ManagePlugin(null, "release", "Releases", "/ui/releases",
            ReleasesPluginEndpoints.class, releasesPluginRegion);

        ManagePlugin dependencies = new ManagePlugin("list", null, "Dep Versions", "/ui/dependencies",
            DependenciesPluginEndpoints.class,
            new DependenciesPluginRegion("soy.page.dependenciesPluginRegion", renderer, upenaStore));

        ManagePlugin modules = new ManagePlugin("wrench", null, "Modules", "/ui/modules",
            ModulesPluginEndpoints.class,
            new ModulesPluginRegion("soy.page.modulesPluginRegion", renderer, upenaStore));

        ManagePlugin ring = new ManagePlugin("leaf", null, "Upena Ring", "/ui/ring",
            UpenaRingPluginEndpoints.class,
            new UpenaRingPluginRegion("soy.page.upenaRingPluginRegion", renderer, amzaService, upenaStore, upenaService, ubaService, ringHost));

        ManagePlugin sar = new ManagePlugin("dashboard", null, "SAR", "/ui/sar",
            SARPluginEndpoints.class,
            new SARPluginRegion("soy.page.sarPluginRegion", renderer, amzaService, ringHost));

        ServicesCallDepthStack servicesCallDepthStack = new ServicesCallDepthStack();
        PerfService perfService = new PerfService(servicesCallDepthStack);

        ManagePlugin profiler = new ManagePlugin("hourglass", null, "Profiler", "/ui/profiler",
            ProfilerPluginEndpoints.class,
            new ProfilerPluginRegion("soy.page.profilerPluginRegion", renderer, new VisualizeProfile(new NameUtils(), servicesCallDepthStack)));

        List<ManagePlugin> plugins = Lists.newArrayList(
            projects,
            modules,
            dependencies,
            changes,
            config,
            clusters,
            hosts,
            services,
            instances,
            releases,
            health,
            connectivity,
            topology,
            profiler,
            sar,
            ring);

        jerseyEndpoints.addInjectable(SoyService.class, soyService);
        jerseyEndpoints.addEndpoint(AsyncLookupEndpoints.class);
        jerseyEndpoints.addInjectable(AsyncLookupService.class, new AsyncLookupService(upenaStore));

        jerseyEndpoints.addEndpoint(PerfServiceEndpoint.class);
        jerseyEndpoints.addInjectable(PerfService.class, perfService);

        for (ManagePlugin plugin : plugins) {
            soyService.registerPlugin(plugin);
            jerseyEndpoints.addEndpoint(plugin.endpointsClass);
            jerseyEndpoints.addInjectable(plugin.region.getClass(), plugin.region);
        }
        jerseyEndpoints.addEndpoint(UpenaPropagatorEndpoints.class);
        jerseyEndpoints.addInjectable(AmzaClusterName.class, new AmzaClusterName((clusterName == null) ? "manual" : clusterName));
    }

    private RowsStorageProvider rowsStorageProvider(final OrderIdProvider orderIdProvider) {
        RowsStorageProvider rowsStorageProvider = (workingDirectory, tableDomain, tableName) -> {
            final File directory = new File(workingDirectory, tableDomain);
            directory.mkdirs();
            File file = new File(directory, tableName.getTableName() + ".kvt");

            BinaryRowMarshaller rowMarshaller = new BinaryRowMarshaller();
            RowsIndexProvider tableIndexProvider = (tableName1) -> {
                NavigableMap<RowIndexKey, RowIndexValue> navigableMap = new ConcurrentSkipListMap<>();
                return new MemoryRowsIndex(navigableMap);
            };

            return new RowTable(tableName,
                orderIdProvider,
                rowMarshaller,
                new BinaryRowsTx(file, rowMarshaller, tableIndexProvider, 1000));
        };
        return rowsStorageProvider;
    }
}
