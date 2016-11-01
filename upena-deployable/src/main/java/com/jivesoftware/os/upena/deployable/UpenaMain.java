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
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.base.SoySyntaxException;
import com.google.template.soy.tofu.SoyTofu;
import com.jivesoftware.os.jive.utils.ordered.id.ConstantWriterIdProvider;
import com.jivesoftware.os.jive.utils.ordered.id.OrderIdProvider;
import com.jivesoftware.os.jive.utils.ordered.id.OrderIdProviderImpl;
import com.jivesoftware.os.jive.utils.ordered.id.TimestampedOrderIdProvider;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.http.client.OAuthSigner;
import com.jivesoftware.os.routing.bird.server.InitializeRestfulServer;
import com.jivesoftware.os.routing.bird.server.RestfulServer;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptorsRequest;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptorsResponse;
import com.jivesoftware.os.uba.shared.PasswordStore;
import com.jivesoftware.os.upena.amza.service.AmzaService;
import com.jivesoftware.os.upena.amza.service.AmzaServiceInitializer;
import com.jivesoftware.os.upena.amza.service.AmzaServiceInitializer.AmzaServiceConfig;
import com.jivesoftware.os.upena.amza.service.discovery.AmzaDiscovery;
import com.jivesoftware.os.upena.amza.service.storage.replication.SendFailureListener;
import com.jivesoftware.os.upena.amza.service.storage.replication.TakeFailureListener;
import com.jivesoftware.os.upena.amza.shared.AmzaInstance;
import com.jivesoftware.os.upena.amza.shared.MemoryRowsIndex;
import com.jivesoftware.os.upena.amza.shared.RingHost;
import com.jivesoftware.os.upena.amza.shared.RowIndexKey;
import com.jivesoftware.os.upena.amza.shared.RowIndexValue;
import com.jivesoftware.os.upena.amza.shared.RowsIndexProvider;
import com.jivesoftware.os.upena.amza.shared.RowsStorageProvider;
import com.jivesoftware.os.upena.amza.shared.UpdatesSender;
import com.jivesoftware.os.upena.amza.shared.UpdatesTaker;
import com.jivesoftware.os.upena.amza.storage.RowTable;
import com.jivesoftware.os.upena.amza.storage.binary.BinaryRowMarshaller;
import com.jivesoftware.os.upena.amza.storage.binary.BinaryRowsTx;
import com.jivesoftware.os.upena.amza.transport.http.replication.HttpUpdatesSender;
import com.jivesoftware.os.upena.amza.transport.http.replication.HttpUpdatesTaker;
import com.jivesoftware.os.upena.amza.transport.http.replication.endpoints.AmzaReplicationRestEndpoints;
import com.jivesoftware.os.upena.config.UpenaConfigRestEndpoints;
import com.jivesoftware.os.upena.config.UpenaConfigStore;
import com.jivesoftware.os.upena.deployable.aws.AWSClientFactory;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaClusterRestEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaEndpoints.AmzaClusterName;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaHealthEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaHostRestEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaInstanceRestEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaReleaseRestEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaRepoEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaServiceRestEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaTenantRestEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.loopback.UpenaLoopbackEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.AWSPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ApiPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.AsyncLookupEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.AuthPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.BreakpointDumperPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ChangeLogPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ClustersPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ConfigPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ConnectivityPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.HealthPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.HostsPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.InstancesPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.JVMPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.LoadBalancersPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ModulesPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.MonkeyPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ProfilerPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ProjectsPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ProxyPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ReleasesPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.RepoPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ServicesPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.TopologyPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.UpenaRingPluginEndpoints;
import com.jivesoftware.os.upena.deployable.lookup.AsyncLookupService;
import com.jivesoftware.os.upena.deployable.profiler.model.ServicesCallDepthStack;
import com.jivesoftware.os.upena.deployable.profiler.server.endpoints.PerfService;
import com.jivesoftware.os.upena.deployable.profiler.server.endpoints.PerfServiceEndpoints;
import com.jivesoftware.os.upena.deployable.profiler.visualize.NameUtils;
import com.jivesoftware.os.upena.deployable.profiler.visualize.VisualizeProfile;
import com.jivesoftware.os.upena.deployable.region.AWSPluginRegion;
import com.jivesoftware.os.upena.deployable.region.AuthPluginRegion;
import com.jivesoftware.os.upena.deployable.region.BreakpointDumperPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ChangeLogPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ClustersPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ConfigPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ConnectivityPluginRegion;
import com.jivesoftware.os.upena.deployable.region.HeaderRegion;
import com.jivesoftware.os.upena.deployable.region.HealthPluginRegion;
import com.jivesoftware.os.upena.deployable.region.HomeRegion;
import com.jivesoftware.os.upena.deployable.region.HostsPluginRegion;
import com.jivesoftware.os.upena.deployable.region.InstancesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.JVMPluginRegion;
import com.jivesoftware.os.upena.deployable.region.LoadBalancersPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ManagePlugin;
import com.jivesoftware.os.upena.deployable.region.MenuRegion;
import com.jivesoftware.os.upena.deployable.region.ModulesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.MonkeyPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ProfilerPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ProjectsPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ProxyPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ReleasesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.RepoPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ServicesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.TopologyPluginRegion;
import com.jivesoftware.os.upena.deployable.region.UnauthorizedPluginRegion;
import com.jivesoftware.os.upena.deployable.region.UpenaRingPluginRegion;
import com.jivesoftware.os.upena.deployable.server.UpenaJerseyEndpoints;
import com.jivesoftware.os.upena.deployable.soy.SoyDataUtils;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import com.jivesoftware.os.upena.service.ChaosService;
import com.jivesoftware.os.upena.service.DiscoveredRoutes;
import com.jivesoftware.os.upena.service.HostKeyProvider;
import com.jivesoftware.os.upena.service.SessionStore;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.PathToRepo;
import com.jivesoftware.os.upena.uba.service.RepositoryProvider;
import com.jivesoftware.os.upena.uba.service.SelfSigningCertGenerator;
import com.jivesoftware.os.upena.uba.service.UbaCoordinate;
import com.jivesoftware.os.upena.uba.service.UbaLog;
import com.jivesoftware.os.upena.uba.service.UbaService;
import com.jivesoftware.os.upena.uba.service.UbaServiceInitializer;
import com.jivesoftware.os.upena.uba.service.UpenaClient;
import de.ruedigermoeller.serialization.FSTConfiguration;
import io.swagger.jaxrs.config.BeanConfig;
import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class UpenaMain {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public static final String[] USAGE = new String[]{
        "Usage:",
        "",
        "    java -jar upena.jar <hostName>                   (manual cluster discovery)",
        "",
        " or ",
        "",
        "    java -jar upena.jar <hostName> <clusterName>     (automatic cluster discovery)",
        "",
        "Overridable properties:",
        "",
        "    -DMASTER_PASSWORD=<password>",
        "         (used to read keystores) ",
        "",
        "    -Dhost.instance.id=<instanceId>",
        "    -Dhost.rack=<rackId>",
        "    -Dhost.datacenter=<datacenterId>",
        "    -Dpublic.host.name=<publiclyReachableHostname>",
        "    -Dmanual.peers=<upenaPeer1Host:port,upenaPeer2Host:port,...>",
        "",
        "    -Damza.port=1175",
        "         (change the port upena uses to interact with other upena nodes.) ",
        "",
        "    -Dmin.service.port=10000",
        "    -Dmax.service.port=32767",
        "         (adjust range to avoid port collision.) ",
        "",
        "    -DpathToRepo=<path>",
        "         (when using upena as a artifact repository. Default is new File(System.getProperty(\"user.home\"), \".m2\")) ",
        "",
        "     Only applicable if you are in aws.",
        "          -Daws.region=<region>",
        "          -Daws.roleArn=<arn>",
        "          -Daws.vpc=<vpcInstanceId>",
        "",
        "     Only applicable if you have specified a <clusterName>.",
        "          -Damza.discovery.group=225.4.5.6",
        "          -Damza.discovery.port=1123",
        "",
        "Example:",
        "nohup java -Xdebug -Xrunjdwp:transport=dt_socket,address=1176,server=y,suspend=n -classpath \"/usr/java/latest/lib/tools.jar:./upena.jar\" com.jivesoftware.os.upena.deployable.UpenaMain `hostname` dev",
        "",};

    public static void main(String[] args) throws Exception {

        try {
            if (args.length == 0) {
                for (String u : USAGE) {
                    LOG.info(u);
                }
                System.exit(1);
            } else {
                new UpenaMain().run(args);
            }
        } catch (Exception x) {
            LOG.error("Catastrophic startup failure.", x);
            System.exit(1);
        }
    }

    public void run(String[] args) throws Exception {

        String workingDir = System.getProperty("user.dir");
        long start = System.currentTimeMillis();
        Exception failed = null;
        while (start + TimeUnit.SECONDS.toMillis(10) > System.currentTimeMillis()) {
            try {
                File lockFile = new File(workingDir, "onlyLetOneRunningAtATime");
                lockFile.createNewFile();
                FileChannel.open(lockFile.toPath(), StandardOpenOption.WRITE).lock();
                failed = null;
                break;
            } catch (Exception x) {
                failed = x;
                LOG.warn("Failed to acquire lock on onlyLetOneRunningAtATime", x);
                Thread.sleep(1000);
            }
        }
        if (failed != null) {
            throw failed;
        }

        JDIAPI jvmapi = null;
        try {
            jvmapi = new JDIAPI();
        } catch (NoClassDefFoundError x) {
            LOG.warn("Failed to local tools.jar. Please manually add to classpath. Breakpoint debugger will be disabled.");
        }

        String hostname = args[0];

        int loopbackPort = Integer.parseInt(System.getProperty("amza.loopback.port", "1174"));
        int port = Integer.parseInt(System.getProperty("amza.port", "1175"));
        String multicastGroup = System.getProperty("amza.discovery.group", "225.4.5.6");
        int multicastPort = Integer.parseInt(System.getProperty("amza.discovery.port", "1123"));
        String clusterName = (args.length > 1 ? args[1] : null);

        String datacenter = System.getProperty("host.datacenter", "unknownDatacenter");
        String rack = System.getProperty("host.rack", "unknownRack");
        String publicHost = System.getProperty("public.host.name", hostname);

        RingHost ringHost = new RingHost(hostname, port); // TODO include rackId

        // todo need a better way to create writer id.
        TimestampedOrderIdProvider orderIdProvider = new OrderIdProviderImpl(new ConstantWriterIdProvider(new Random().nextInt(512)));

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final AmzaServiceConfig amzaServiceConfig = new AmzaServiceConfig();

        RowsStorageProvider rowsStorageProvider = rowsStorageProvider(orderIdProvider);

        boolean sslEnable = Boolean.parseBoolean(System.getProperty("ssl.enabled", "true"));
        String sslKeystorePassword = System.getProperty("ssl.keystore.password", "password");
        String sslKeystorePath = System.getProperty("ssl.keystore.path", "./certs/sslKeystore");
        String sslKeyStoreAlias = System.getProperty("ssl.keystore.alias", "upenanode").toLowerCase();
        boolean sslAutoGenerateSelfSignedCert = Boolean.parseBoolean(System.getProperty("ssl.keystore.autoGenerate", "true"));

        File sslKeystore = new File(sslKeystorePath);
        if (sslEnable) {
            SelfSigningCertGenerator selfSigningCertGenerator = new SelfSigningCertGenerator();
            if (sslKeystore.exists()) {
                if (!selfSigningCertGenerator.validate(sslKeyStoreAlias, sslKeystorePassword, sslKeystore)) {
                    LOG.error("SSL keystore validation failed. keyStoreAlias:{} sslKeystore:{}", sslKeyStoreAlias, sslKeystore);
                    System.exit(1);
                }
            } else {
                sslKeystore.getParentFile().mkdirs();
                if (sslAutoGenerateSelfSignedCert) {
                    selfSigningCertGenerator.create(sslKeyStoreAlias, sslKeystorePassword, sslKeystore);
                } else {
                    LOG.error("Failed to locate mandatory sslKeystore:{}", sslKeystore);
                    System.exit(1);
                }
            }
        }
        OAuthSigner authSigner = null;
        UpenaSSLConfig upenaSSLConfig = new UpenaSSLConfig(sslEnable, sslAutoGenerateSelfSignedCert, authSigner);
        UpdatesSender changeSetSender = new HttpUpdatesSender(sslEnable, sslAutoGenerateSelfSignedCert, authSigner);
        UpdatesTaker tableTaker = new HttpUpdatesTaker(sslEnable, sslAutoGenerateSelfSignedCert, authSigner);

        AmzaService amzaService = new AmzaServiceInitializer().initialize(amzaServiceConfig,
            orderIdProvider,
            new com.jivesoftware.os.upena.amza.storage.FstMarshaller(FSTConfiguration.getDefaultConfiguration()),
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

        ObjectMapper storeMapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        UpenaConfigStore upenaConfigStore = new UpenaConfigStore(storeMapper, amzaService);

        LOG.info("-----------------------------------------------------------------------");
        LOG.info("|      Upena Config Store Online");
        LOG.info("-----------------------------------------------------------------------");

        ExecutorService instanceChangedThreads = Executors.newFixedThreadPool(32);

        AtomicReference<UbaService> ubaServiceReference = new AtomicReference<>();
        UpenaStore upenaStore = new UpenaStore(
            orderIdProvider,
            storeMapper,
            amzaService, (instanceChanges) -> {
                instanceChangedThreads.submit(() -> {
                    UbaService got = ubaServiceReference.get();
                    if (got != null) {
                        try {
                            got.instanceChanged(instanceChanges);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            },
            (changes) -> {
            },
            (change) -> {
                LOG.info("TODO: tie into conductor. " + change);
            },
            Integer.parseInt(System.getProperty("min.service.port", "10000")),
            Integer.parseInt(System.getProperty("max.service.port", String.valueOf(Short.MAX_VALUE)))
        );
        upenaStore.attachWatchers();

        ChaosService chaosService = new ChaosService(upenaStore);
        PasswordStore passwordStore = (key) -> {
            return System.getProperty("MASTER_PASSWORD", "PASSWORD"); // cough
        };

        SessionStore sessionStore = new SessionStore(TimeUnit.MINUTES.toMillis(60), TimeUnit.MINUTES.toMillis(1));

        UpenaService upenaService = new UpenaService(passwordStore, sessionStore, upenaStore, chaosService);

        LOG.info("-----------------------------------------------------------------------");
        LOG.info("|      Upena Service Online");
        LOG.info("-----------------------------------------------------------------------");

        File defaultPathToRepo = new File(new File(System.getProperty("user.home"), ".m2"), "repository");
        PathToRepo localPathToRepo = new PathToRepo(new File(System.getProperty("pathToRepo", defaultPathToRepo.getAbsolutePath())));
        RepositoryProvider repositoryProvider = new RepositoryProvider(localPathToRepo);

        Host host = new Host(publicHost, datacenter, rack, ringHost.getHost(), ringHost.getPort(), workingDir, null, null);
        HostKey hostKey = new HostKeyProvider().getNodeKey(upenaStore.hosts, host);

        String hostInstanceId = System.getProperty("host.instance.id", hostKey.getKey());
        host = new Host(publicHost, datacenter, rack, ringHost.getHost(), ringHost.getPort(), workingDir, hostInstanceId, null);

        Host gotHost = upenaStore.hosts.get(hostKey);
        if (gotHost == null || !gotHost.equals(host)) {
            upenaStore.hosts.update(hostKey, host);
        }

        UbaLog ubaLog = (what, why, how) -> {
            try {
                upenaStore.record("Uba", what, System.currentTimeMillis(), why, ringHost.getHost() + ":" + ringHost.getPort(), how);
            } catch (Exception x) {
                x.printStackTrace(); // Hmm lame
            }
        };

        UpenaClient upenaClient = new UpenaClient() {
            @Override
            public InstanceDescriptorsResponse instanceDescriptor(InstanceDescriptorsRequest instanceDescriptorsRequest) throws Exception {
                return upenaService.instanceDescriptors(instanceDescriptorsRequest);
            }

            @Override
            public void updateKeyPair(String instanceKey, String publicKey) throws Exception {
                Instance i = upenaStore.instances.get(new InstanceKey(instanceKey));
                if (i != null) {
                    LOG.info("Updating publicKey for {}", instanceKey);
                    upenaStore.instances.update(new InstanceKey(instanceKey), new Instance(i.clusterKey,
                        i.hostKey,
                        i.serviceKey,
                        i.releaseGroupKey,
                        i.instanceId,
                        i.enabled,
                        i.locked,
                        publicKey,
                        i.restartTimestampGMTMillis,
                        i.ports));
                }
            }

        };

        final UbaService ubaService = new UbaServiceInitializer().initialize(passwordStore,
            upenaClient,
            repositoryProvider,
            hostKey.getKey(),
            workingDir,
            new UbaCoordinate(
                datacenter,
                rack,
                publicHost,
                host.hostName,
                "localhost",
                loopbackPort
            ),
            null,
            ubaLog);

        DiscoveredRoutes discoveredRoutes = new DiscoveredRoutes();
        ShiroRequestHelper shiroRequestHelper = new ShiroRequestHelper();

        UpenaJerseyEndpoints jerseyEndpoints = new UpenaJerseyEndpoints()
            .addInjectable(ShiroRequestHelper.class, shiroRequestHelper)
            .addEndpoint(UpenaClusterRestEndpoints.class)
            .addEndpoint(UpenaHostRestEndpoints.class)
            .addEndpoint(UpenaServiceRestEndpoints.class)
            .addEndpoint(UpenaReleaseRestEndpoints.class)
            .addEndpoint(UpenaInstanceRestEndpoints.class)
            .addEndpoint(UpenaTenantRestEndpoints.class)
            .addInjectable(upenaService)
            .addInjectable(upenaStore)
            .addInjectable(upenaConfigStore)
            .addInjectable(ubaService)
            .addEndpoint(AmzaReplicationRestEndpoints.class)
            .addInjectable(AmzaInstance.class, amzaService)
            .addEndpoint(UpenaEndpoints.class)
            .addEndpoint(UpenaHealthEndpoints.class)
            .addEndpoint(UpenaRepoEndpoints.class)
            .addInjectable(DiscoveredRoutes.class, discoveredRoutes)
            .addInjectable(RingHost.class, ringHost)
            .addInjectable(HostKey.class, hostKey)
            .addInjectable(UpenaAutoRelease.class, new UpenaAutoRelease(repositoryProvider, upenaStore))
            .addInjectable(PathToRepo.class, localPathToRepo);

        String region = System.getProperty("aws.region", null);
        String roleArn = System.getProperty("aws.roleArn", null);

        AWSClientFactory awsClientFactory = new AWSClientFactory(region, roleArn);

        injectUI(awsClientFactory,
            storeMapper,
            mapper,
            jvmapi,
            amzaService,
            localPathToRepo,
            repositoryProvider,
            hostKey,
            ringHost,
            upenaSSLConfig,
            upenaStore,
            upenaConfigStore,
            jerseyEndpoints,
            clusterName,
            discoveredRoutes);

        InitializeRestfulServer initializeRestfulServer = new InitializeRestfulServer(false,
            port,
            "UpenaNode",
            sslEnable,
            sslKeyStoreAlias,
            sslKeystorePassword,
            sslKeystorePath,
            128,
            10_000);

        buildSwagger();
        initializeRestfulServer.addClasspathResource("/resources");
//        initializeRestfulServer.addContextHandler("/docs/", (Server server, String context, String applicationName) -> {
//            return buildSwaggerUI(context);
//        });
        initializeRestfulServer.addContextHandler("/", jerseyEndpoints);

        RestfulServer restfulServer = initializeRestfulServer.build();
        restfulServer.start();

        LOG.info("-----------------------------------------------------------------------");
        LOG.info("|      Jetty Service Online");
        LOG.info("-----------------------------------------------------------------------");

        UpenaJerseyEndpoints loopbackJerseyEndpoints = new UpenaJerseyEndpoints()
            .addEndpoint(UpenaLoopbackEndpoints.class)
            .addEndpoint(UpenaConfigRestEndpoints.class)
            .addInjectable(DiscoveredRoutes.class, discoveredRoutes)
            .addInjectable(upenaConfigStore)
            .addInjectable(UpenaService.class, upenaService);

        InitializeRestfulServer initializeLoopbackRestfulServer = new InitializeRestfulServer(
            Boolean.parseBoolean(System.getProperty("amza.loopback.strict", "true")),
            loopbackPort,
            "UpenaNode",
            false,
            sslKeyStoreAlias,
            sslKeystorePassword,
            sslKeystorePath,
            128,
            10_000);
        initializeLoopbackRestfulServer.addClasspathResource("/resources");
        initializeLoopbackRestfulServer.addContextHandler("/", loopbackJerseyEndpoints);

        RestfulServer loopbackRestfulServer = initializeLoopbackRestfulServer.build();
        loopbackRestfulServer.start();

        LOG.info("-----------------------------------------------------------------------");
        LOG.info("|      Jetty Service Online");
        LOG.info("-----------------------------------------------------------------------");

        if (ubaService != null) {
            Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(() -> {
                try {
                    ubaService.nanny();
                } catch (Exception ex) {
                    LOG.error("Nanny failure", ex);
                }
            }, 15, 15, TimeUnit.SECONDS);
            LOG.info("-----------------------------------------------------------------------");
            LOG.info("|      Uba Service Online");
            LOG.info("-----------------------------------------------------------------------");
        }
        ubaServiceReference.set(ubaService);

        if (clusterName != null) {
            AmzaDiscovery amzaDiscovery = new AmzaDiscovery(amzaService, ringHost, clusterName, multicastGroup, multicastPort);
            amzaDiscovery.start();
            LOG.info("-----------------------------------------------------------------------");
            LOG.info("|      Amza Service Discovery Online");
            LOG.info("-----------------------------------------------------------------------");
        } else {
            LOG.info("-----------------------------------------------------------------------");
            LOG.info("|     Amza Service is in manual Discovery mode.  No cluster name was specified");
            LOG.info("-----------------------------------------------------------------------");
        }

        String peers = System.getProperty("manual.peers");
        if (peers != null) {
            String[] hostPortTuples = peers.split(",");
            for (String hostPortTuple : hostPortTuples) {
                String hostPort = hostPortTuple.trim();
                if (hostPort.length() > 0 && hostPort.contains(":")) {
                    String[] host_port = hostPort.split(":");
                    try {
                        RingHost anotherRingHost = new RingHost(host_port[0].trim(), Integer.parseInt(host_port[1].trim()));
                        List<RingHost> ring = amzaService.getRing("master");
                        if (!ring.contains(anotherRingHost)) {
                            LOG.info("Adding host to the cluster: " + anotherRingHost);
                            amzaService.addRingHost("master", anotherRingHost);
                        }
                    } catch (Exception x) {
                        LOG.warn("Malformed hostPortTuple {}", hostPort);
                    }
                } else {
                    LOG.warn("Malformed hostPortTuple {}", hostPort);
                }
            }
        }

        String vpc = System.getProperty("aws.vpc", null);
        UpenaAWSLoadBalancerNanny upenaAWSLoadBalancerNanny = new UpenaAWSLoadBalancerNanny(vpc, upenaStore, hostKey, awsClientFactory);

        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(() -> {
            try {
                upenaAWSLoadBalancerNanny.ensureSelf();
            } catch (Exception x) {
                LOG.warn("Failures while nannying load loadbalancer.", x);
            }
        }, 1, 1, TimeUnit.MINUTES); // TODO better
    }

    private void injectUI(AWSClientFactory awsClientFactory,
        ObjectMapper storeMapper,
        ObjectMapper mapper,
        JDIAPI jvmapi,
        AmzaService amzaInstance,
        PathToRepo localPathToRepo,
        RepositoryProvider repositoryProvider,
        HostKey hostKey,
        RingHost ringHost,
        UpenaSSLConfig upenaSSLConfig,
        UpenaStore upenaStore,
        UpenaConfigStore upenaConfigStore,
        UpenaJerseyEndpoints jerseyEndpoints,
        String clusterName,
        DiscoveredRoutes discoveredRoutes) throws SoySyntaxException {

        SoyFileSet.Builder soyFileSetBuilder = new SoyFileSet.Builder();

        LOG.info("Add....");

        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/chrome.soy"), "chrome.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/homeRegion.soy"), "home.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/changeLogPluginRegion.soy"), "changeLog.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/instanceHealthPluginRegion.soy"), "instanceHealthPluginRegion.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/healthPluginRegion.soy"), "health.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/healthPopup.soy"), "healthPopup.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/topologyPluginRegion.soy"), "topology.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/connectivityPluginRegion.soy"), "connectivity.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/connectionOverview.soy"), "connectionOverview.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/connectionsHealth.soy"), "connectionsHealth.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/instancesPluginRegion.soy"), "instances.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/modulesPluginRegion.soy"), "modules.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/clustersPluginRegion.soy"), "clusters.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/hostsPluginRegion.soy"), "hosts.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/servicesPluginRegion.soy"), "services.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/releasesPluginRegion.soy"), "releases.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/upenaRingPluginRegion.soy"), "upenaRing.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/configPluginRegion.soy"), "config.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/profilerPluginRegion.soy"), "profilerPluginRegion.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/projectsPluginRegion.soy"), "projects.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/projectBuildOutput.soy"), "projectOutput.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/projectBuildOutputTail.soy"), "projectOutputTail.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/repoPluginRegion.soy"), "repoPluginRegion.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/proxyPluginRegion.soy"), "proxyPluginRegion.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/monkeyPluginRegion.soy"), "monkeyPluginRegion.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/loadBalancersPluginRegion.soy"), "loadBalancersPluginRegion.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/authPluginRegion.soy"), "authPluginRegion.soy");
        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/unauthorizedPluginRegion.soy"), "unauthorizedPluginRegion.soy");

        if (jvmapi != null) {
            soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/jvmPluginRegion.soy"), "jvmPluginRegion.soy");
            soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/breakpointDumperPluginRegion.soy"), "breakpointDumperPluginRegion.soy");
        }

        soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/awsPluginRegion.soy"), "awsPluginRegion.soy");

        SoyFileSet sfs = soyFileSetBuilder.build();
        SoyTofu tofu = sfs.compileToTofu();
        SoyRenderer renderer = new SoyRenderer(tofu, new SoyDataUtils());
        SoyService soyService = new SoyService(renderer,
            new HeaderRegion("soy.chrome.headerRegion", renderer),
            new MenuRegion("soy.chrome.menuRegion", renderer),
            new HomeRegion("soy.page.homeRegion", renderer, hostKey, upenaStore),
            clusterName,
            hostKey,
            upenaStore
        );

        AuthPluginRegion authRegion = new AuthPluginRegion("soy.page.authPluginRegion", renderer);
        UnauthorizedPluginRegion unauthorizedRegion = new UnauthorizedPluginRegion("soy.page.unauthorizedPluginRegion", renderer);

        ManagePlugin auth = new ManagePlugin("login", null, "Login", "/ui/auth/login",
            AuthPluginEndpoints.class, authRegion, null, "read");

        HealthPluginRegion healthPluginRegion = new HealthPluginRegion(mapper,
            System.currentTimeMillis(),
            ringHost,
            "soy.page.healthPluginRegion",
            "soy.page.instanceHealthPluginRegion",
            "soy.page.healthPopup",
            renderer,
            amzaInstance,
            upenaSSLConfig,
            upenaStore,
            upenaConfigStore);
        ReleasesPluginRegion releasesPluginRegion = new ReleasesPluginRegion(mapper, repositoryProvider,
            "soy.page.releasesPluginRegion", "soy.page.releasesPluginRegionList",
            renderer, upenaStore);
        HostsPluginRegion hostsPluginRegion = new HostsPluginRegion("soy.page.hostsPluginRegion", renderer, upenaStore);
        InstancesPluginRegion instancesPluginRegion = new InstancesPluginRegion("soy.page.instancesPluginRegion",
            "soy.page.instancesPluginRegionList", renderer, upenaStore, hostKey, healthPluginRegion, awsClientFactory);

        ManagePlugin health = new ManagePlugin("fire", null, "Health", "/ui/health",
            HealthPluginEndpoints.class, healthPluginRegion, null, "read");

        ManagePlugin topology = new ManagePlugin("th", null, "Topology", "/ui/topology",
            TopologyPluginEndpoints.class,
            new TopologyPluginRegion(mapper, "soy.page.topologyPluginRegion", "soy.page.connectionsHealth",
                renderer, amzaInstance, upenaSSLConfig, upenaStore, healthPluginRegion, hostsPluginRegion, releasesPluginRegion, instancesPluginRegion,
                discoveredRoutes), null,
            "read");

        ManagePlugin connectivity = new ManagePlugin("transfer", null, "Connectivity", "/ui/connectivity",
            ConnectivityPluginEndpoints.class,
            new ConnectivityPluginRegion(mapper, "soy.page.connectivityPluginRegion", "soy.page.connectionsHealth", "soy.page.connectionOverview",
                renderer, amzaInstance, upenaSSLConfig, upenaStore, healthPluginRegion, hostsPluginRegion, releasesPluginRegion, instancesPluginRegion,
                discoveredRoutes), null,
            "read");

        ManagePlugin changes = new ManagePlugin("road", null, "Changes", "/ui/changeLog",
            ChangeLogPluginEndpoints.class,
            new ChangeLogPluginRegion("soy.page.changeLogPluginRegion", renderer, upenaStore), null, "read");

        ManagePlugin instances = new ManagePlugin("star", null, "Instances", "/ui/instances",
            InstancesPluginEndpoints.class, instancesPluginRegion, null, "read");

        ManagePlugin config = new ManagePlugin("cog", null, "Config", "/ui/config",
            ConfigPluginEndpoints.class,
            new ConfigPluginRegion(mapper, "soy.page.configPluginRegion", renderer, upenaSSLConfig, upenaStore, upenaConfigStore), null, "read");

        ManagePlugin repo = new ManagePlugin("hdd", null, "Repository", "/ui/repo",
            RepoPluginEndpoints.class,
            new RepoPluginRegion("soy.page.repoPluginRegion", renderer, upenaStore, localPathToRepo), null, "read");

        ManagePlugin projects = new ManagePlugin("folder-open", null, "Projects", "/ui/projects",
            ProjectsPluginEndpoints.class,
            new ProjectsPluginRegion("soy.page.projectsPluginRegion", "soy.page.projectBuildOutput", "soy.page.projectBuildOutputTail", renderer, upenaStore,
                localPathToRepo), null, "read");

        ManagePlugin clusters = new ManagePlugin("cloud", null, "Clusters", "/ui/clusters",
            ClustersPluginEndpoints.class,
            new ClustersPluginRegion("soy.page.clustersPluginRegion", renderer, upenaStore), null, "read");

        ManagePlugin hosts = new ManagePlugin("tasks", null, "Hosts", "/ui/hosts",
            HostsPluginEndpoints.class, hostsPluginRegion, null, "read");

        ManagePlugin services = new ManagePlugin("tint", null, "Services", "/ui/services",
            ServicesPluginEndpoints.class,
            new ServicesPluginRegion(mapper, "soy.page.servicesPluginRegion", renderer, upenaStore), null, "read");

        ManagePlugin releases = new ManagePlugin("send", null, "Releases", "/ui/releases",
            ReleasesPluginEndpoints.class, releasesPluginRegion, null, "read");

        ManagePlugin modules = new ManagePlugin("wrench", null, "Modules", "/ui/modules",
            ModulesPluginEndpoints.class,
            new ModulesPluginRegion(mapper, repositoryProvider, "soy.page.modulesPluginRegion", renderer, upenaStore), null, "read");

        ManagePlugin proxy = new ManagePlugin("random", null, "Proxies", "/ui/proxy",
            ProxyPluginEndpoints.class,
            new ProxyPluginRegion("soy.page.proxyPluginRegion", renderer), null, "read", "debug");

        ManagePlugin ring = new ManagePlugin("leaf", null, "Upena", "/ui/ring",
            UpenaRingPluginEndpoints.class,
            new UpenaRingPluginRegion(storeMapper, "soy.page.upenaRingPluginRegion", renderer, amzaInstance, upenaStore, upenaConfigStore), null, "read",
            "debug");

        ManagePlugin loadBalancer = new ManagePlugin("scale", null, "Load Balancer", "/ui/loadbalancers",
            LoadBalancersPluginEndpoints.class,
            new LoadBalancersPluginRegion("soy.page.loadBalancersPluginRegion", renderer, upenaStore, awsClientFactory), null, "read", "debug");

        ServicesCallDepthStack servicesCallDepthStack = new ServicesCallDepthStack();
        PerfService perfService = new PerfService(servicesCallDepthStack);

        ManagePlugin profiler = new ManagePlugin("hourglass", null, "Profiler", "/ui/profiler",
            ProfilerPluginEndpoints.class,
            new ProfilerPluginRegion("soy.page.profilerPluginRegion", renderer, new VisualizeProfile(new NameUtils(), servicesCallDepthStack)), null, "read",
            "debug");

        ManagePlugin jvm = null;
        ManagePlugin breakpointDumper = null;
        if (jvmapi != null) {
            jvm = new ManagePlugin("camera", null, "JVM", "/ui/jvm",
                JVMPluginEndpoints.class,
                new JVMPluginRegion("soy.page.jvmPluginRegion", renderer, upenaStore, jvmapi), null, "read", "debug");

            breakpointDumper = new ManagePlugin("record", null, "Breakpoint Dumper", "/ui/breakpoint",
                BreakpointDumperPluginEndpoints.class,
                new BreakpointDumperPluginRegion(hostKey, "soy.page.breakpointDumperPluginRegion", renderer, upenaStore, jvmapi), null, "read", "debug");
        }

        ManagePlugin aws = null;
        aws = new ManagePlugin("globe", null, "AWS", "/ui/aws",
            AWSPluginEndpoints.class,
            new AWSPluginRegion("soy.page.awsPluginRegion", renderer, awsClientFactory), null, "read", "debug");

        ManagePlugin monkey = new ManagePlugin("flash", null, "Chaos", "/ui/chaos",
            MonkeyPluginEndpoints.class,
            new MonkeyPluginRegion("soy.page.monkeyPluginRegion", renderer, upenaStore), null, "read", "debug");

        ManagePlugin api = new ManagePlugin("play-circle", null, "API", "/ui/api",
            ApiPluginEndpoints.class,
            null, null, "read", "debug");

        List<ManagePlugin> plugins = new ArrayList<>();
        plugins.add(auth);
        plugins.add(new ManagePlugin(null, null, "API", null, null, null, "separator", "read"));
        plugins.add(api);
        plugins.add(new ManagePlugin(null, null, "Build", null, null, null, "separator", "read"));
        plugins.add(repo);
        plugins.add(projects);
        plugins.add(modules);
        plugins.add(new ManagePlugin(null, null, "Config", null, null, null, "separator", "read"));
        plugins.add(aws);
        plugins.add(changes);
        plugins.add(config);
        plugins.add(clusters);
        plugins.add(hosts);
        plugins.add(services);
        plugins.add(releases);
        plugins.add(instances);
        plugins.add(loadBalancer);
        plugins.add(new ManagePlugin(null, null, "Health", null, null, null, "separator", "read"));
        plugins.add(health);
        plugins.add(connectivity);
        plugins.add(topology);
        plugins.add(new ManagePlugin(null, null, "Tools", null, null, null, "separator", "read", "debug"));
        plugins.add(monkey);
        plugins.add(proxy);
        if (jvm != null) {
            plugins.add(jvm);
            plugins.add(breakpointDumper);
        }
        plugins.add(profiler);
        plugins.add(ring);

        jerseyEndpoints.addInjectable(UpenaSSLConfig.class, upenaSSLConfig);
        jerseyEndpoints.addInjectable(SoyService.class, soyService);
        jerseyEndpoints.addEndpoint(AsyncLookupEndpoints.class);
        jerseyEndpoints.addInjectable(AsyncLookupService.class, new AsyncLookupService(upenaSSLConfig, upenaStore));

        jerseyEndpoints.addEndpoint(PerfServiceEndpoints.class);
        jerseyEndpoints.addInjectable(PerfService.class, perfService);

        for (ManagePlugin plugin : plugins) {
            soyService.registerPlugin(plugin);
            if (plugin.separator == null) {
                jerseyEndpoints.addEndpoint(plugin.endpointsClass);
                if (plugin.region != null) {
                    jerseyEndpoints.addInjectable(plugin.region.getClass(), plugin.region);
                }
            }
        }

        jerseyEndpoints.addInjectable(UnauthorizedPluginRegion.class, unauthorizedRegion);
        //jerseyEndpoints.addEndpoint(UpenaPropagatorEndpoints.class);
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

    public static void buildSwagger() {
        // This configures Swagger
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion("1.0.0");
        beanConfig.setResourcePackage("com.jivesoftware.os.upena.deployable");
        beanConfig.setScan(true);
        beanConfig.setBasePath("/");
        //beanConfig.setDescription("Upena");
        beanConfig.setTitle("Upena");
    }

}
