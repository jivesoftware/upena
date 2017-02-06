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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.base.SoySyntaxException;
import com.google.template.soy.tofu.SoyTofu;
import com.jivesoftware.os.amza.api.BAInterner;
import com.jivesoftware.os.amza.api.partition.PartitionProperties;
import com.jivesoftware.os.amza.api.ring.RingHost;
import com.jivesoftware.os.amza.api.ring.RingMember;
import com.jivesoftware.os.amza.api.ring.RingMemberAndHost;
import com.jivesoftware.os.amza.berkeleydb.BerkeleyDBWALIndexProvider;
import com.jivesoftware.os.amza.embed.EmbedAmzaServiceInitializer.QuorumTimeouts;
import com.jivesoftware.os.amza.lab.pointers.LABPointerIndexConfig;
import com.jivesoftware.os.amza.lab.pointers.LABPointerIndexWALIndexProvider;
import com.jivesoftware.os.amza.service.AmzaService;
import com.jivesoftware.os.amza.service.AmzaServiceInitializer;
import com.jivesoftware.os.amza.service.AmzaServiceInitializer.AmzaServiceConfig;
import com.jivesoftware.os.amza.service.EmbeddedClientProvider;
import com.jivesoftware.os.amza.service.SickPartitions;
import com.jivesoftware.os.amza.service.replication.TakeFailureListener;
import com.jivesoftware.os.amza.service.replication.http.HttpAvailableRowsTaker;
import com.jivesoftware.os.amza.service.replication.http.HttpRowsTaker;
import com.jivesoftware.os.amza.service.ring.AmzaRingReader;
import com.jivesoftware.os.amza.service.ring.AmzaRingWriter;
import com.jivesoftware.os.amza.service.ring.RingTopology;
import com.jivesoftware.os.amza.service.stats.AmzaStats;
import com.jivesoftware.os.amza.service.storage.PartitionPropertyMarshaller;
import com.jivesoftware.os.amza.service.storage.binary.BinaryHighwaterRowMarshaller;
import com.jivesoftware.os.amza.service.storage.binary.BinaryPrimaryRowMarshaller;
import com.jivesoftware.os.amza.service.take.AvailableRowsTaker;
import com.jivesoftware.os.amza.service.take.RowsTakerFactory;
import com.jivesoftware.os.aquarium.AquariumStats;
import com.jivesoftware.os.jive.utils.ordered.id.ConstantWriterIdProvider;
import com.jivesoftware.os.jive.utils.ordered.id.JiveEpochTimestampProvider;
import com.jivesoftware.os.jive.utils.ordered.id.OrderIdProvider;
import com.jivesoftware.os.jive.utils.ordered.id.OrderIdProviderImpl;
import com.jivesoftware.os.jive.utils.ordered.id.SnowflakeIdPacker;
import com.jivesoftware.os.jive.utils.ordered.id.TimestampedOrderIdProvider;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.authentication.AuthValidationFilter;
import com.jivesoftware.os.routing.bird.authentication.NoAuthEvaluator;
import com.jivesoftware.os.routing.bird.health.api.HealthCheckConfigBinder;
import com.jivesoftware.os.routing.bird.health.api.HealthCheckRegistry;
import com.jivesoftware.os.routing.bird.health.api.HealthChecker;
import com.jivesoftware.os.routing.bird.health.api.HealthFactory;
import com.jivesoftware.os.routing.bird.health.api.HealthTimer;
import com.jivesoftware.os.routing.bird.health.api.PercentileHealthCheckConfig;
import com.jivesoftware.os.routing.bird.health.api.SickHealthCheckConfig;
import com.jivesoftware.os.routing.bird.health.api.TimerHealthCheckConfig;
import com.jivesoftware.os.routing.bird.health.api.TriggerTimeoutHealthCheck;
import com.jivesoftware.os.routing.bird.health.checkers.PercentileHealthChecker;
import com.jivesoftware.os.routing.bird.health.checkers.SickThreads;
import com.jivesoftware.os.routing.bird.health.checkers.TimerHealthChecker;
import com.jivesoftware.os.routing.bird.http.client.HttpDeliveryClientHealthProvider;
import com.jivesoftware.os.routing.bird.http.client.OAuthSigner;
import com.jivesoftware.os.routing.bird.http.client.OAuthSignerProvider;
import com.jivesoftware.os.routing.bird.http.client.TenantAwareHttpClient;
import com.jivesoftware.os.routing.bird.http.client.TenantRoutingHttpClientInitializer;
import com.jivesoftware.os.routing.bird.server.InitializeRestfulServer;
import com.jivesoftware.os.routing.bird.server.RestfulServer;
import com.jivesoftware.os.routing.bird.server.oauth.AuthValidationException;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthEvaluator;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthSecretManager;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthServiceLocatorShim;
import com.jivesoftware.os.routing.bird.server.oauth.validator.AuthValidator;
import com.jivesoftware.os.routing.bird.server.oauth.validator.DefaultOAuthValidator;
import com.jivesoftware.os.routing.bird.shared.AuthEvaluator.AuthStatus;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptorsProvider;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptorsResponse;
import com.jivesoftware.os.routing.bird.shared.HostPort;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptorsRequest;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptorsResponse;
import com.jivesoftware.os.routing.bird.shared.TenantsServiceConnectionDescriptorProvider;
import com.jivesoftware.os.uba.shared.PasswordStore;
import com.jivesoftware.os.upena.amza.service.UpenaAmzaService;
import com.jivesoftware.os.upena.amza.service.UpenaAmzaServiceInitializer;
import com.jivesoftware.os.upena.amza.service.UpenaAmzaServiceInitializer.UpenaAmzaServiceConfig;
import com.jivesoftware.os.upena.amza.service.discovery.AmzaDiscovery;
import com.jivesoftware.os.upena.amza.service.storage.replication.SendFailureListener;
import com.jivesoftware.os.upena.amza.service.storage.replication.UpenaTakeFailureListener;
import com.jivesoftware.os.upena.amza.shared.AmzaInstance;
import com.jivesoftware.os.upena.amza.shared.MemoryRowsIndex;
import com.jivesoftware.os.upena.amza.shared.RowIndexKey;
import com.jivesoftware.os.upena.amza.shared.RowIndexValue;
import com.jivesoftware.os.upena.amza.shared.RowsIndexProvider;
import com.jivesoftware.os.upena.amza.shared.RowsStorageProvider;
import com.jivesoftware.os.upena.amza.shared.UpdatesSender;
import com.jivesoftware.os.upena.amza.shared.UpdatesTaker;
import com.jivesoftware.os.upena.amza.shared.UpenaRingHost;
import com.jivesoftware.os.upena.amza.storage.RowTable;
import com.jivesoftware.os.upena.amza.storage.binary.BinaryRowMarshaller;
import com.jivesoftware.os.upena.amza.storage.binary.BinaryRowsTx;
import com.jivesoftware.os.upena.amza.transport.http.replication.HttpUpdatesSender;
import com.jivesoftware.os.upena.amza.transport.http.replication.HttpUpdatesTaker;
import com.jivesoftware.os.upena.amza.transport.http.replication.endpoints.AmzaReplicationRestEndpoints;
import com.jivesoftware.os.upena.deployable.UpenaHealth.NannyHealth;
import com.jivesoftware.os.upena.deployable.UpenaHealth.NodeHealth;
import com.jivesoftware.os.upena.deployable.aws.AWSClientFactory;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaConnectivityEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaHealthEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaManagedDeployableEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.UpenaRepoEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.v1.UpenaClusterRestEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.v1.UpenaHostRestEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.v1.UpenaInstanceRestEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.v1.UpenaReleaseRestEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.v1.UpenaServiceRestEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.api.v1.UpenaTenantRestEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.loopback.UpenaConfigRestEndpoints;
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
import com.jivesoftware.os.upena.deployable.endpoints.ui.ManagedDeployablePluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ModulesPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.MonkeyPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.PermissionsPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ProfilerPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ProjectsPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ProxyPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ReleasesPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.RepoPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ServicesPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.ThrownPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.TopologyPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.UpenaRingPluginEndpoints;
import com.jivesoftware.os.upena.deployable.endpoints.ui.UsersPluginEndpoints;
import com.jivesoftware.os.upena.deployable.lookup.AsyncLookupService;
import com.jivesoftware.os.upena.deployable.okta.OktaCredentialsMatcher;
import com.jivesoftware.os.upena.deployable.okta.OktaLog;
import com.jivesoftware.os.upena.deployable.okta.OktaRealm;
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
import com.jivesoftware.os.upena.deployable.region.HealthPluginRegion;
import com.jivesoftware.os.upena.deployable.region.HomeRegion;
import com.jivesoftware.os.upena.deployable.region.HostsPluginRegion;
import com.jivesoftware.os.upena.deployable.region.InstancesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.JVMPluginRegion;
import com.jivesoftware.os.upena.deployable.region.LoadBalancersPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ManagedDeployablePluginRegion;
import com.jivesoftware.os.upena.deployable.region.MenuRegion;
import com.jivesoftware.os.upena.deployable.region.ModulesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.MonkeyPluginRegion;
import com.jivesoftware.os.upena.deployable.region.OktaMFAAuthPluginRegion;
import com.jivesoftware.os.upena.deployable.region.PermissionsPluginRegion;
import com.jivesoftware.os.upena.deployable.region.PluginHandle;
import com.jivesoftware.os.upena.deployable.region.ProfilerPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ProjectsPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ProxyPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ReleasesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.RepoPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ServicesPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ThrownPluginRegion;
import com.jivesoftware.os.upena.deployable.region.TopologyPluginRegion;
import com.jivesoftware.os.upena.deployable.region.UnauthorizedPluginRegion;
import com.jivesoftware.os.upena.deployable.region.UpenaRingPluginRegion;
import com.jivesoftware.os.upena.deployable.region.UsersPluginRegion;
import com.jivesoftware.os.upena.deployable.server.UpenaJerseyEndpoints;
import com.jivesoftware.os.upena.deployable.soy.SoyDataUtils;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import com.jivesoftware.os.upena.service.ChaosService;
import com.jivesoftware.os.upena.service.DiscoveredRoutes;
import com.jivesoftware.os.upena.service.HostKeyProvider;
import com.jivesoftware.os.upena.service.InstanceHealthly;
import com.jivesoftware.os.upena.service.SessionStore;
import com.jivesoftware.os.upena.service.UpenaConfigStore;
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
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import jersey.repackaged.com.google.common.collect.Sets;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.signature.HmacSha1MessageSigner;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;
import org.merlin.config.BindInterfaceToConfiguration;
import org.merlin.config.Config;
import org.merlin.config.defaults.DoubleDefault;
import org.merlin.config.defaults.LongDefault;
import org.merlin.config.defaults.StringDefault;

import static org.merlin.config.BindInterfaceToConfiguration.bindDefault;

public class UpenaMain {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public static final String[] USAGE = new String[] {
        "Usage:",
        "",
        "    java -jar upena.jar <hostName>                   (manual cluster discovery)",
        "",
        " or ",
        "",
        "    java -jar upena.jar <hostName> <clusterDiscoveryName>     (automatic cluster discovery)",
        "",
        " Overridable properties:",
        "",
        "     Controls upena ssl terminination.",
        "       -Dssl.enabled=true",
        "       -Dssl.keystore.password=password",
        "       -Dssl.keystore.path=./certs/sslKeystore",
        "       -Dssl.keystore.alias=upenanode",
        "       -Dssl.keystore.autoGenerate=true",
        "",
        "     Controls upena to upena authentication.",
        "       -Dupena.consumerKey=<clusterDiscoveryName>",
        "       -Dupena.secret=secret",
        "            (all upena nodes that are part of the same cluster should have the same 'consumerKey' and 'secret'.) ",
        "",
        "     Handed out to deployable on request so they can access their instance specific keystores.",
        "       -Dsauth.keystore.password=<auto-generated-if-absent>",
        "",
        "    -Daccount.name=<clusterDiscoveryName>",
        "    -Dhost.instance.id=<instanceId>",
        "    -Dhost.rack=<rackId>",
        "    -Dhost.datacenter=<datacenterId>",
        "    -Dpublic.host.name=<publiclyReachableHostname>",
        "    -Dmanual.peers=<upenaPeer1Host:port,upenaPeer2Host:port,...>",
        "",
        "    -Damza.port=1175",
        "    -Damza.loopback.port=1174",
        "    -Damza.loopback.strict=true",
        "         (change the port upena uses to interact with other upena nodes.) ",
        "",
        "    -Dokta.base.url=<oktaBaseUrl>",
        "    -Dokta.api.key=<?>",
        "    -Dokta.mfa.factorId=<?>",
        "    -Dokta.roles.directory=<pathToRoles> ",
        "          (one role file per okta user that you want to have root access)",
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
        "     Only applicable if you have specified a <clusterDiscoveryName>.",
        "          -Damza.discovery.group=225.4.5.6",
        "          -Damza.discovery.port=1123",
        "",
        " Example:",
        " nohup java -Xdebug -Xrunjdwp:transport=dt_socket,address=1176,server=y,suspend=n -classpath \"/usr/java/latest/lib/tools.jar:./upena.jar\" com" +
            ".jivesoftware.os.upena.deployable.UpenaMain `hostname` dev",
        "", };

    public static void main(String[] args) throws Exception {

        try {
            if (args.length == 0) {
                for (String u : USAGE) {
                    System.out.println(u);
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


        Properties buildProperties = new Properties();
        String upenaVersion = "";
        try {
            buildProperties.load(UpenaMain.class.getClassLoader().getResourceAsStream("build.properties"));
            upenaVersion = buildProperties.getProperty("my.version", "")
                + " "
                + buildProperties.getProperty("my.timestamp", "")
                + " sha:"
                + buildProperties.getProperty("git.commit.id", "");
        } catch (Exception x) {
            LOG.warn("Failed to locate build.properties");
        }


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
        String clusterDiscoveryName = (args.length > 1 ? args[1] : null);

        String datacenter = System.getProperty("host.datacenter", "unknownDatacenter");
        String rack = System.getProperty("host.rack", "unknownRack");
        String publicHost = System.getProperty("public.host.name", hostname);

        UpenaRingHost ringHost = new UpenaRingHost(hostname, port); // TODO include rackId

        // todo need a better way to create writer id.
        int writerId = new Random().nextInt(512);
        TimestampedOrderIdProvider orderIdProvider = new OrderIdProviderImpl(new ConstantWriterIdProvider(writerId));

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final UpenaAmzaServiceConfig upenaAmzaServiceConfig = new UpenaAmzaServiceConfig();

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

        String consumerKey = System.getProperty("upena.consumerKey", clusterDiscoveryName);
        if (consumerKey == null) {
            consumerKey = "upena";
            LOG.warn("Please provide a stronger consumerKey via -Dupena.consumerKey");
        }
        String finalConsumerKey = consumerKey;

        String secret = System.getProperty("upena.secret");
        if (secret == null) {
            secret = "secret";
            LOG.warn("Please provide a stronger secret via -Dupena.secret");
        }
        String finalSecret = secret;

        OAuthSigner authSigner = (request) -> {
            CommonsHttpOAuthConsumer oAuthConsumer = new CommonsHttpOAuthConsumer(finalConsumerKey, finalSecret);
            oAuthConsumer.setMessageSigner(new HmacSha1MessageSigner());
            oAuthConsumer.setTokenWithSecret(finalConsumerKey, finalSecret);
            return oAuthConsumer.sign(request);
        };
        UpenaSSLConfig upenaSSLConfig = new UpenaSSLConfig(sslEnable, sslAutoGenerateSelfSignedCert, authSigner);
        UpdatesSender changeSetSender = new HttpUpdatesSender(sslEnable, sslAutoGenerateSelfSignedCert, authSigner);
        UpdatesTaker tableTaker = new HttpUpdatesTaker(sslEnable, sslAutoGenerateSelfSignedCert, authSigner);

        UpenaAmzaService upenaAmzaService = new UpenaAmzaServiceInitializer().initialize(upenaAmzaServiceConfig,
            orderIdProvider,
            new com.jivesoftware.os.upena.amza.storage.FstMarshaller(FSTConfiguration.getDefaultConfiguration()),
            rowsStorageProvider,
            rowsStorageProvider,
            rowsStorageProvider,
            changeSetSender,
            tableTaker,
            Optional.<SendFailureListener>absent(),
            Optional.<UpenaTakeFailureListener>absent(),
            (changes) -> {
            }
        );

        upenaAmzaService.start(ringHost, upenaAmzaServiceConfig.resendReplicasIntervalInMillis,
            upenaAmzaServiceConfig.applyReplicasIntervalInMillis,
            upenaAmzaServiceConfig.takeFromNeighborsIntervalInMillis,
            upenaAmzaServiceConfig.checkIfCompactionIsNeededIntervalInMillis,
            upenaAmzaServiceConfig.compactTombstoneIfOlderThanNMillis);

        LOG.info("-----------------------------------------------------------------------");
        LOG.info("|      OLD Amza Service Online");
        LOG.info("-----------------------------------------------------------------------");

        BAInterner baInterner = new BAInterner();


        AtomicReference<Callable<RingTopology>> topologyProvider = new AtomicReference<>(); // bit of a hack
        InstanceDescriptor instanceDescriptor = new InstanceDescriptor(datacenter, rack, "", "", "", "", "", "", "", "", 0,
            "", "", "", 0L, true);
        ConnectionDescriptorsProvider connectionsProvider = (connectionDescriptorsRequest, expectedReleaseGroup) -> {
            try {
                RingTopology systemRing = topologyProvider.get().call();
                List<ConnectionDescriptor> descriptors = Lists.newArrayList(Iterables.transform(systemRing.entries,
                    input -> new ConnectionDescriptor(instanceDescriptor,
                        false,
                        false,
                        new HostPort(input.ringHost.getHost(), input.ringHost.getPort()),
                        Collections.emptyMap(),
                        Collections.emptyMap())));
                return new ConnectionDescriptorsResponse(200, Collections.emptyList(), "", descriptors, connectionDescriptorsRequest.getRequestUuid());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        TenantsServiceConnectionDescriptorProvider<String> connectionPoolProvider = new TenantsServiceConnectionDescriptorProvider<>(
            Executors.newScheduledThreadPool(1),
            "",
            connectionsProvider,
            "",
            "",
            10_000); // TODO config
        connectionPoolProvider.start();

        HttpDeliveryClientHealthProvider clientHealthProvider = new HttpDeliveryClientHealthProvider("", null, "", 5000, 100);

        TenantRoutingHttpClientInitializer<String> nonSigningClientInitializer = new TenantRoutingHttpClientInitializer<>(null);

        TenantAwareHttpClient<String> systemTakeClient = nonSigningClientInitializer.builder(
            connectionPoolProvider, // TODO config
            clientHealthProvider)
            .deadAfterNErrors(10)
            .checkDeadEveryNMillis(10_000)
            .maxConnections(1_000)
            .socketTimeoutInMillis(60_000)
            .build(); // TODO expose to conf
        TenantAwareHttpClient<String> stripedTakeClient = nonSigningClientInitializer.builder(
            connectionPoolProvider, // TODO config
            clientHealthProvider)
            .deadAfterNErrors(10)
            .checkDeadEveryNMillis(10_000)
            .maxConnections(1_000)
            .socketTimeoutInMillis(60_000)
            .build(); // TODO expose to conf


        TenantRoutingHttpClientInitializer<String> tenantRoutingHttpClientInitializer = new TenantRoutingHttpClientInitializer<>(
            new OAuthSignerProvider(() -> authSigner));

        TenantAwareHttpClient<String> ringClient = tenantRoutingHttpClientInitializer.builder(
            connectionPoolProvider, // TODO config
            clientHealthProvider)
            .deadAfterNErrors(10)
            .checkDeadEveryNMillis(10_000)
            .maxConnections(1_000)
            .socketTimeoutInMillis(60_000)
            .build(); // TODO expose to conf


        AmzaService amzaService = startAmza(baInterner, writerId, new RingHost(datacenter, rack, ringHost.getHost(), ringHost.getPort()),
            new RingMember(ringHost.getHost() + ":" + ringHost.getPort()), authSigner, systemTakeClient, stripedTakeClient, ringClient, topologyProvider);


        while (amzaService.isReady()) {

        }

        EmbeddedClientProvider embeddedClientProvider = new EmbeddedClientProvider(amzaService);
        boolean cleanup = false;

        LOG.info("-----------------------------------------------------------------------");
        LOG.info("|      Amza Service Online");
        LOG.info("-----------------------------------------------------------------------");


        ObjectMapper storeMapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        UpenaConfigStore upenaConfigStore = new UpenaConfigStore(storeMapper, upenaAmzaService, amzaService, embeddedClientProvider);

        LOG.info("-----------------------------------------------------------------------");
        LOG.info("|      Upena Config Store Online");
        LOG.info("-----------------------------------------------------------------------");


        ExecutorService instanceChangedThreads = Executors.newFixedThreadPool(32);

        AtomicReference<UbaService> ubaServiceReference = new AtomicReference<>();
        UpenaStore upenaStore = new UpenaStore(
            storeMapper,
            upenaAmzaService,
            (instanceChanges) -> {
                instanceChangedThreads.submit(
                    () -> {
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
            amzaService,
            embeddedClientProvider
        );
        upenaStore.attachWatchers();

        ChaosService chaosService = new ChaosService(upenaStore);
        SecureRandom random = new SecureRandom();
        PasswordStore passwordStore = (key) -> {
            String password = System.getProperty("sauth.keystore.password");
            if (password == null) {
                File passwordFile = new File(workingDir, "keystore/" + key + ".key");
                if (passwordFile.exists()) {
                    password = Files.toString(passwordFile, StandardCharsets.UTF_8);
                } else {
                    passwordFile.getParentFile().mkdirs();
                    password = new BigInteger(130, random).toString(32);
                    Files.write(password, passwordFile, StandardCharsets.UTF_8);
                }
            }
            return password;
        };

        SessionStore sessionStore = new SessionStore(TimeUnit.MINUTES.toMillis(60),
            TimeUnit.MINUTES.toMillis(30));

        AtomicReference<UpenaHealth> upenaHealthProvider = new AtomicReference<>();
        InstanceHealthly instanceHealthly = new InstanceHealthly() {
            @Override
            public boolean isHealth(InstanceKey key, String version) throws Exception {
                UpenaHealth upenaHealth = upenaHealthProvider.get();
                if (upenaHealth == null) {
                    return false;
                }
                ConcurrentMap<UpenaRingHost, NodeHealth> ringHostNodeHealth = upenaHealth.buildClusterHealth();
                for (NodeHealth nodeHealth : ringHostNodeHealth.values()) {
                    for (NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                        if (nannyHealth.instanceDescriptor.instanceKey.equals(key.getKey())) {
                            return nannyHealth.serviceHealth.fullyOnline ? nannyHealth.serviceHealth.version.equals(version) : false;
                        }
                    }
                }
                return false;
            }
        };
        UpenaService upenaService = new UpenaService(passwordStore, sessionStore, upenaStore, chaosService, instanceHealthly);

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

        OktaLog oktaLog = (who, what, why, how) -> {
            try {
                upenaStore.record("okta:" + who, what, System.currentTimeMillis(), why, ringHost.getHost() + ":" + ringHost.getPort(), how);
            } catch (Exception x) {
                x.printStackTrace(); // Hmm lame
            }
        };

        OktaCredentialsMatcher.oktaLog = oktaLog;
        OktaRealm.oktaLog = oktaLog;


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

        UpenaHealth upenaHealth = new UpenaHealth(upenaAmzaService, upenaSSLConfig, upenaConfigStore, ubaService, ringHost, hostKey);
        upenaHealthProvider.set(upenaHealth);

        DiscoveredRoutes discoveredRoutes = new DiscoveredRoutes();
        ShiroRequestHelper shiroRequestHelper = new ShiroRequestHelper(TimeUnit.DAYS.toMillis(1)); // TODO expose Sys prop?

        String shiroConfigLocation = System.getProperty("shiro.ini.location", "classpath:shiro.ini"); // classpath:oktashiro.ini

        UpenaJerseyEndpoints jerseyEndpoints = new UpenaJerseyEndpoints(shiroConfigLocation)
            .addInjectable(ShiroRequestHelper.class, shiroRequestHelper)
            .addEndpoint(UpenaClusterRestEndpoints.class)
            .addEndpoint(UpenaHostRestEndpoints.class)
            .addEndpoint(UpenaServiceRestEndpoints.class)
            .addEndpoint(UpenaReleaseRestEndpoints.class)
            .addEndpoint(UpenaInstanceRestEndpoints.class)
            .addEndpoint(UpenaTenantRestEndpoints.class)
            .addInjectable(upenaHealth)
            .addInjectable(upenaService)
            .addInjectable(upenaStore)
            .addInjectable(upenaConfigStore)
            .addInjectable(ubaService)
            //.addEndpoint(AmzaReplicationRestEndpoints.class)
            .addInjectable(AmzaInstance.class, upenaAmzaService)
            .addEndpoint(UpenaEndpoints.class)
            .addEndpoint(UpenaConnectivityEndpoints.class)
            .addEndpoint(UpenaManagedDeployableEndpoints.class)
            .addEndpoint(UpenaHealthEndpoints.class)
            .addEndpoint(UpenaRepoEndpoints.class)
            .addInjectable(DiscoveredRoutes.class, discoveredRoutes)
            .addInjectable(UpenaRingHost.class, ringHost)
            .addInjectable(HostKey.class, hostKey)
            .addInjectable(UpenaAutoRelease.class, new UpenaAutoRelease(repositoryProvider, upenaStore))
            .addInjectable(PathToRepo.class, localPathToRepo);

        PercentileHealthCheckConfig phcc = bindDefault(PercentileHealthCheckConfig.class);
        PercentileHealthChecker authFilterHealthCheck = new PercentileHealthChecker(phcc);
        AuthValidationFilter authValidationFilter = new AuthValidationFilter(authFilterHealthCheck);
        authValidationFilter.addEvaluator(new NoAuthEvaluator(),
            "/",
            "/swagger.json",
            "/ui/*", // Handled by Shiro
            "/repo/*" // Cough
        );

        OAuth1Signature verifier = new OAuth1Signature(new OAuthServiceLocatorShim());
        OAuthSecretManager oAuthSecretManager = new OAuthSecretManager() {
            @Override
            public void clearCache() {
            }

            @Override
            public String getSecret(String id) throws AuthValidationException {
                return id.equals(finalConsumerKey) ? finalSecret : null;
            }

            @Override
            public void verifyLastSecretRemovalTime() throws Exception {
            }
        };
        AuthValidator<OAuth1Signature, OAuth1Request> oAuthValidator = new DefaultOAuthValidator(Executors.newScheduledThreadPool(1),
            Long.MAX_VALUE,
            oAuthSecretManager,
            60_000,
            false,
            false
        );
        oAuthValidator.start();
        authValidationFilter.addEvaluator(new NoAuthEvaluator(), "/repo/*", "/amza/rows/stream/*", "/amza/rows/taken/*", "/amza/pong/*", "/amza/invalidate/*");
        authValidationFilter.addEvaluator(new OAuthEvaluator(oAuthValidator, verifier), "/upena/*", "/amza/*");


        // TODO something better someday
        String upenaApiUsername = System.getProperty("upena.api.username", null);
        String upenaApiPassword = System.getProperty("upena.api.password", null);

        if (upenaApiUsername != null && upenaApiPassword != null) {
            authValidationFilter.addEvaluator(containerRequestContext -> {
                String authCredentials = containerRequestContext.getHeaderString("Authorization");
                if (authCredentials == null) {
                    return AuthStatus.not_handled;
                }

                final String encodedUserPassword = authCredentials.replaceFirst("Basic" + " ", "");
                String usernameAndPassword = null;
                try {
                    byte[] decodedBytes = Base64.getDecoder().decode(encodedUserPassword);
                    usernameAndPassword = new String(decodedBytes, "UTF-8");
                } catch (IOException e) {
                    return AuthStatus.denied;
                }
                final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
                final String username = tokenizer.nextToken();
                final String password = tokenizer.nextToken();

                boolean authenticationStatus = upenaApiUsername.equals(username) && upenaApiPassword.equals(password);

                return authenticationStatus ? AuthStatus.authorized : AuthStatus.denied;
            }, "/api/*");
        }

        jerseyEndpoints.addContainerRequestFilter(authValidationFilter);

        String region = System.getProperty("aws.region", null);
        String roleArn = System.getProperty("aws.roleArn", null);

        AWSClientFactory awsClientFactory = new AWSClientFactory(region, roleArn);

        String accountName = System.getProperty("account.name", clusterDiscoveryName == null ? "" : clusterDiscoveryName);
        String humanReadableUpenaClusterName = datacenter + " - " + accountName;
        injectUI(upenaVersion,
            awsClientFactory,
            storeMapper,
            mapper,
            jvmapi,
            upenaAmzaService,
            localPathToRepo,
            repositoryProvider,
            hostKey,
            ringHost,
            upenaSSLConfig,
            port,
            sessionStore,
            ubaService,
            upenaHealth,
            upenaStore,
            upenaConfigStore,
            jerseyEndpoints,
            humanReadableUpenaClusterName,
            discoveredRoutes);


        injectAmza(baInterner, jerseyEndpoints, amzaService, ringClient);

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
        initializeRestfulServer.addContextHandler("/", jerseyEndpoints);

        RestfulServer restfulServer = initializeRestfulServer.build();
        restfulServer.start();


        LOG.info("-----------------------------------------------------------------------");
        LOG.info("|      Jetty Service Online");
        LOG.info("-----------------------------------------------------------------------");


        UpenaJerseyEndpoints loopbackJerseyEndpoints = new UpenaJerseyEndpoints(null)
            .addEndpoint(UpenaLoopbackEndpoints.class)
            .addEndpoint(UpenaConfigRestEndpoints.class)
            .addInjectable(SessionStore.class, sessionStore)
            .addInjectable(DiscoveredRoutes.class, discoveredRoutes)
            .addInjectable(upenaConfigStore)
            .addInjectable(upenaStore)
            .addInjectable(upenaHealth)
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

        if (clusterDiscoveryName != null) {
            AmzaDiscovery amzaDiscovery = new AmzaDiscovery(upenaAmzaService, ringHost, clusterDiscoveryName, multicastGroup, multicastPort);
            amzaDiscovery.start();
            LOG.info("-----------------------------------------------------------------------");
            LOG.info("|      Amza Service Discovery Online");
            LOG.info("-----------------------------------------------------------------------");
        } else {
            LOG.info("-----------------------------------------------------------------------");
            LOG.info("|     Amza Service is in manual Discovery mode.  No cluster discovery name was specified");
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
                        UpenaRingHost anotherRingHost = new UpenaRingHost(host_port[0].trim(), Integer.parseInt(host_port[1].trim()));
                        List<UpenaRingHost> ring = upenaAmzaService.getRing("master");
                        if (!ring.contains(anotherRingHost)) {
                            LOG.info("Adding host to the cluster: " + anotherRingHost);
                            upenaAmzaService.addRingHost("master", anotherRingHost);
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

        LOG.info("-----------------------------------------------------------------------");
        LOG.info("|     Begin Migration");
        LOG.info("-----------------------------------------------------------------------");

        upenaStore.init(orderIdProvider,
            Integer.parseInt(System.getProperty("min.service.port", "10000")),
            Integer.parseInt(System.getProperty("max.service.port", String.valueOf(Short.MAX_VALUE))),
            false);

        upenaConfigStore.init();

        LOG.info("-----------------------------------------------------------------------");
        LOG.info("|     End Migration");
        LOG.info("-----------------------------------------------------------------------");


    }


    private void injectUI(String upenaVersion,
        AWSClientFactory awsClientFactory,
        ObjectMapper storeMapper,
        ObjectMapper mapper,
        JDIAPI jvmapi,
        UpenaAmzaService amzaInstance,
        PathToRepo localPathToRepo,
        RepositoryProvider repositoryProvider,
        HostKey hostKey,
        UpenaRingHost ringHost,
        UpenaSSLConfig upenaSSLConfig,
        int port,
        SessionStore sessionStore,
        UbaService ubaService,
        UpenaHealth upenaHealth,
        UpenaStore upenaStore,
        UpenaConfigStore upenaConfigStore,
        UpenaJerseyEndpoints jerseyEndpoints,
        String humanReadableUpenaClusterName,
        DiscoveredRoutes discoveredRoutes) throws SoySyntaxException, IOException {

        SoyFileSet.Builder soyFileSetBuilder = new SoyFileSet.Builder();

        LOG.info("Add....");


        URL dirURL = UpenaMain.class.getClassLoader().getResource("resources/soy/");
        if (dirURL != null && dirURL.getProtocol().equals("jar")) {
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!"));
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith(".soy") && name.startsWith("resources/soy/")) {
                    String soyName = name.substring(name.lastIndexOf('/') + 1);
                    LOG.info("/" + name + " " + soyName);
                    soyFileSetBuilder.add(this.getClass().getResource("/" + name), soyName);
                }
            }
        } else {
            List<String> soyFiles = IOUtils.readLines(this.getClass().getResourceAsStream("resources/soy/"), StandardCharsets.UTF_8);
            for (String soyFile : soyFiles) {
                LOG.info("Adding {}", soyFile);
                soyFileSetBuilder.add(this.getClass().getResource("/resources/soy/" + soyFile), soyFile);
            }
        }


        SoyFileSet sfs = soyFileSetBuilder.build();
        SoyTofu tofu = sfs.compileToTofu();
        SoyRenderer renderer = new SoyRenderer(tofu, new SoyDataUtils());
        SoyService soyService = new SoyService(upenaVersion,
            renderer,
            //new HeaderRegion("soy.chrome.headerRegion", renderer),
            new MenuRegion("soy.chrome.menuRegion", renderer),
            new HomeRegion("soy.page.homeRegion", renderer, hostKey, upenaStore, ubaService),
            humanReadableUpenaClusterName,
            hostKey,
            upenaStore
        );

        AuthPluginRegion authRegion = new AuthPluginRegion("soy.page.authPluginRegion", renderer);
        OktaMFAAuthPluginRegion oktaMFAAuthRegion = new OktaMFAAuthPluginRegion("soy.page.oktaMFAAuthPluginRegion", renderer);
        jerseyEndpoints.addInjectable(OktaMFAAuthPluginRegion.class, oktaMFAAuthRegion);

        UnauthorizedPluginRegion unauthorizedRegion = new UnauthorizedPluginRegion("soy.page.unauthorizedPluginRegion", renderer);

        PluginHandle auth = new PluginHandle("login", null, "Login", "/ui/auth/login",
            AuthPluginEndpoints.class, authRegion, null, "read");

        HealthPluginRegion healthPluginRegion = new HealthPluginRegion(mapper,
            System.currentTimeMillis(),
            ringHost,
            "soy.page.healthPluginRegion",
            "soy.page.instanceHealthPluginRegion",
            "soy.page.healthPopup",
            renderer,
            upenaHealth,
            upenaStore);
        ReleasesPluginRegion releasesPluginRegion = new ReleasesPluginRegion(mapper, repositoryProvider,
            "soy.page.releasesPluginRegion", "soy.page.releasesPluginRegionList",
            renderer, upenaStore);
        HostsPluginRegion hostsPluginRegion = new HostsPluginRegion("soy.page.hostsPluginRegion", "soy.page.removeHostPluginRegion", renderer, upenaStore);
        InstancesPluginRegion instancesPluginRegion = new InstancesPluginRegion("soy.page.instancesPluginRegion",
            "soy.page.instancesPluginRegionList", renderer, upenaHealth, upenaStore, hostKey, healthPluginRegion, awsClientFactory);

        PluginHandle health = new PluginHandle("fire", null, "Health", "/ui/health",
            HealthPluginEndpoints.class, healthPluginRegion, null, "read");

        PluginHandle topology = new PluginHandle("th", null, "Topology", "/ui/topology",
            TopologyPluginEndpoints.class,
            new TopologyPluginRegion(mapper, "soy.page.topologyPluginRegion", "soy.page.connectionsHealth",
                renderer, upenaHealth, amzaInstance, upenaSSLConfig, upenaStore, healthPluginRegion, hostsPluginRegion, releasesPluginRegion,
                instancesPluginRegion,
                discoveredRoutes), null,
            "read");

        PluginHandle connectivity = new PluginHandle("transfer", null, "Connectivity", "/ui/connectivity",
            ConnectivityPluginEndpoints.class,
            new ConnectivityPluginRegion(mapper, "soy.page.connectivityPluginRegion", "soy.page.connectionsHealth", "soy.page.connectionOverview",
                renderer, upenaHealth, amzaInstance, upenaSSLConfig, upenaStore, healthPluginRegion,
                discoveredRoutes), null,
            "read");

        PluginHandle changes = new PluginHandle("road", null, "Changes", "/ui/changeLog",
            ChangeLogPluginEndpoints.class,
            new ChangeLogPluginRegion("soy.page.changeLogPluginRegion", renderer, upenaStore), null, "read");

        PluginHandle instances = new PluginHandle("star", null, "Instances", "/ui/instances",
            InstancesPluginEndpoints.class, instancesPluginRegion, null, "read");

        PluginHandle config = new PluginHandle("cog", null, "Config", "/ui/config",
            ConfigPluginEndpoints.class,
            new ConfigPluginRegion(mapper, "soy.page.configPluginRegion", renderer, upenaSSLConfig, upenaStore, upenaConfigStore), null, "read");

        PluginHandle repo = new PluginHandle("hdd", null, "Repository", "/ui/repo",
            RepoPluginEndpoints.class,
            new RepoPluginRegion("soy.page.repoPluginRegion", renderer, upenaStore, localPathToRepo), null, "read");

        PluginHandle projects = new PluginHandle("folder-open", null, "Projects", "/ui/projects",
            ProjectsPluginEndpoints.class,
            new ProjectsPluginRegion("soy.page.projectsPluginRegion", "soy.page.projectBuildOutput", "soy.page.projectBuildOutputTail", renderer, upenaStore,
                localPathToRepo), null, "read");

        PluginHandle users = new PluginHandle("user", null, "Users", "/ui/users",
            UsersPluginEndpoints.class,
            new UsersPluginRegion("soy.page.usersPluginRegion", renderer, upenaStore), null, "read");


        PluginHandle permissions = new PluginHandle("lock", null, "Permission", "/ui/permissions",
            PermissionsPluginEndpoints.class,
            new PermissionsPluginRegion("soy.page.permissionsPluginRegion", renderer, upenaStore), null, "read");


        PluginHandle clusters = new PluginHandle("cloud", null, "Clusters", "/ui/clusters",
            ClustersPluginEndpoints.class,
            new ClustersPluginRegion("soy.page.clustersPluginRegion", renderer, upenaStore), null, "read");

        PluginHandle hosts = new PluginHandle("tasks", null, "Hosts", "/ui/hosts",
            HostsPluginEndpoints.class, hostsPluginRegion, null, "read");

        PluginHandle services = new PluginHandle("tint", null, "Services", "/ui/services",
            ServicesPluginEndpoints.class,
            new ServicesPluginRegion(mapper, "soy.page.servicesPluginRegion", renderer, upenaStore), null, "read");

        PluginHandle releases = new PluginHandle("send", null, "Releases", "/ui/releases",
            ReleasesPluginEndpoints.class, releasesPluginRegion, null, "read");

        PluginHandle modules = new PluginHandle("wrench", null, "Modules", "/ui/modules",
            ModulesPluginEndpoints.class,
            new ModulesPluginRegion(mapper, repositoryProvider, "soy.page.modulesPluginRegion", renderer, upenaStore), null, "read");

        PluginHandle proxy = new PluginHandle("random", null, "Proxies", "/ui/proxy",
            ProxyPluginEndpoints.class,
            new ProxyPluginRegion("soy.page.proxyPluginRegion", renderer), null, "read", "debug");

        PluginHandle ring = new PluginHandle("leaf", null, "Upena", "/ui/ring",
            UpenaRingPluginEndpoints.class,
            new UpenaRingPluginRegion(storeMapper, "soy.page.upenaRingPluginRegion", renderer, amzaInstance, upenaStore, upenaConfigStore), null, "read",
            "debug");

        PluginHandle loadBalancer = new PluginHandle("scale", null, "Load Balancer", "/ui/loadbalancers",
            LoadBalancersPluginEndpoints.class,
            new LoadBalancersPluginRegion("soy.page.loadBalancersPluginRegion", renderer, upenaStore, awsClientFactory), null, "read", "debug");

        ServicesCallDepthStack servicesCallDepthStack = new ServicesCallDepthStack();
        PerfService perfService = new PerfService(servicesCallDepthStack);

        PluginHandle profiler = new PluginHandle("hourglass", null, "Profiler", "/ui/profiler",
            ProfilerPluginEndpoints.class,
            new ProfilerPluginRegion("soy.page.profilerPluginRegion", renderer, new VisualizeProfile(new NameUtils(), servicesCallDepthStack)), null, "read",
            "debug");

        PluginHandle jvm = null;
        PluginHandle breakpointDumper = null;
        if (jvmapi != null) {
            jvm = new PluginHandle("camera", null, "JVM", "/ui/jvm",
                JVMPluginEndpoints.class,
                new JVMPluginRegion("soy.page.jvmPluginRegion", renderer, upenaStore, jvmapi), null, "read", "debug");

            breakpointDumper = new PluginHandle("record", null, "Breakpoint Dumper", "/ui/breakpoint",
                BreakpointDumperPluginEndpoints.class,
                new BreakpointDumperPluginRegion("soy.page.breakpointDumperPluginRegion", renderer, upenaStore, jvmapi), null, "read", "debug");
        }

        PluginHandle aws = null;
        aws = new PluginHandle("globe", null, "AWS", "/ui/aws",
            AWSPluginEndpoints.class,
            new AWSPluginRegion("soy.page.awsPluginRegion", renderer, awsClientFactory), null, "read", "debug");

        PluginHandle monkey = new PluginHandle("flash", null, "Chaos", "/ui/chaos",
            MonkeyPluginEndpoints.class,
            new MonkeyPluginRegion("soy.page.monkeyPluginRegion", renderer, upenaStore), null, "read", "debug");

        PluginHandle api = new PluginHandle("play-circle", null, "API", "/ui/api",
            ApiPluginEndpoints.class,
            null, null, "read", "debug");

        PluginHandle thrown = new PluginHandle("equalizer", null, "Thrown", "/ui/thrown",
            ThrownPluginEndpoints.class,
            new ThrownPluginRegion(hostKey, "soy.page.thrownPluginRegion", renderer, upenaStore), null, "read", "debug");

        PluginHandle probe = new PluginHandle("hand-right", null, "Deployable", "/ui/deployable",
            ManagedDeployablePluginEndpoints.class,
            new ManagedDeployablePluginRegion(sessionStore,
                hostKey,
                "soy.page.deployablePluginRegion",
                renderer,
                upenaStore,
                upenaSSLConfig,
                port
            ), null, "read", "debug");

        List<PluginHandle> plugins = new ArrayList<>();
        plugins.add(auth);
        plugins.add(new PluginHandle(null, null, "API", null, null, null, "separator", "read"));
        plugins.add(api);
        plugins.add(new PluginHandle(null, null, "Build", null, null, null, "separator", "read"));
        plugins.add(repo);
        plugins.add(projects);
        plugins.add(modules);
        plugins.add(new PluginHandle(null, null, "Config", null, null, null, "separator", "read"));
        plugins.add(aws);
        plugins.add(changes);
        plugins.add(config);
        plugins.add(clusters);
        plugins.add(hosts);
        plugins.add(services);
        plugins.add(releases);
        plugins.add(instances);
        plugins.add(loadBalancer);
        plugins.add(new PluginHandle(null, null, "Health", null, null, null, "separator", "read"));
        plugins.add(health);
        plugins.add(connectivity);
        plugins.add(topology);
        plugins.add(new PluginHandle(null, null, "Tools", null, null, null, "separator", "read", "debug"));
        plugins.add(monkey);
        plugins.add(proxy);
        if (jvm != null) {
            plugins.add(jvm);
            plugins.add(thrown);
            plugins.add(breakpointDumper);
        }
        plugins.add(profiler);
        plugins.add(ring);
        plugins.add(users);
        plugins.add(permissions);

        jerseyEndpoints.addInjectable(SessionStore.class, sessionStore);
        jerseyEndpoints.addInjectable(UpenaSSLConfig.class, upenaSSLConfig);
        jerseyEndpoints.addInjectable(SoyService.class, soyService);
        jerseyEndpoints.addEndpoint(AsyncLookupEndpoints.class);
        jerseyEndpoints.addInjectable(AsyncLookupService.class, new AsyncLookupService(upenaSSLConfig, upenaStore));

        jerseyEndpoints.addEndpoint(PerfServiceEndpoints.class);
        jerseyEndpoints.addInjectable(PerfService.class, perfService);

        for (PluginHandle plugin : plugins) {
            soyService.registerPlugin(plugin);
            if (plugin.separator == null) {
                jerseyEndpoints.addEndpoint(plugin.endpointsClass);
                if (plugin.region != null) {
                    jerseyEndpoints.addInjectable(plugin.region.getClass(), plugin.region);
                }
            }
        }

        jerseyEndpoints.addEndpoint(probe.endpointsClass);
        jerseyEndpoints.addInjectable(probe.region.getClass(), probe.region);

        jerseyEndpoints.addInjectable(UnauthorizedPluginRegion.class, unauthorizedRegion);
        //jerseyEndpoints.addEndpoint(UpenaPropagatorEndpoints.class);
    }

    private RowsStorageProvider rowsStorageProvider(final OrderIdProvider orderIdProvider) {
        return (workingDirectory, tableDomain, tableName) -> {
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

    public AmzaService startAmza(BAInterner baInterner,
        int writerId,
        RingHost ringHost,
        RingMember ringMember,
        OAuthSigner authSigner,
        TenantAwareHttpClient<String> systemTakeClient,
        TenantAwareHttpClient<String> stripedTakeClient,
        TenantAwareHttpClient<String> ringClient, AtomicReference<Callable<RingTopology>> topologyProvider) throws Exception {

        HealthFactory.initialize(new HealthCheckConfigBinder() {
            @Override
            public <C extends Config> C bindConfig(Class<C> aClass) {
                return BindInterfaceToConfiguration.bindDefault(aClass);
            }
        }, new HealthCheckRegistry() {

            @Override
            public void register(HealthChecker<?> healthChecker) {

            }

            @Override
            public void unregister(HealthChecker<?> healthChecker) {

            }
        });

        //Deployable deployable = new Deployable(new String[0]);

        SnowflakeIdPacker idPacker = new SnowflakeIdPacker();
        JiveEpochTimestampProvider timestampProvider = new JiveEpochTimestampProvider();

        AmzaStats amzaStats = new AmzaStats();


        SickThreads sickThreads = new SickThreads();
        SickPartitions sickPartitions = new SickPartitions();
        //deployable.addHealthCheck(new SickThreadsHealthCheck(deployable.config(AmzaSickThreadsHealthConfig.class), sickThreads));
        //deployable.addHealthCheck(new SickPartitionsHealthCheck(sickPartitions));

        TimestampedOrderIdProvider orderIdProvider = new OrderIdProviderImpl(new ConstantWriterIdProvider(writerId),
            idPacker, timestampProvider);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);

        PartitionPropertyMarshaller partitionPropertyMarshaller = new PartitionPropertyMarshaller() {

            @Override
            public PartitionProperties fromBytes(byte[] bytes) {
                try {
                    return mapper.readValue(bytes, PartitionProperties.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public byte[] toBytes(PartitionProperties partitionProperties) {
                try {
                    return mapper.writeValueAsBytes(partitionProperties);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        BinaryPrimaryRowMarshaller primaryRowMarshaller = new BinaryPrimaryRowMarshaller(); // hehe you cant change this :)
        BinaryHighwaterRowMarshaller highwaterRowMarshaller = new BinaryHighwaterRowMarshaller(baInterner);

        RowsTakerFactory systemRowsTakerFactory = () -> new HttpRowsTaker(amzaStats, systemTakeClient, mapper, baInterner);
        RowsTakerFactory rowsTakerFactory = () -> new HttpRowsTaker(amzaStats, stripedTakeClient, mapper, baInterner);


        AvailableRowsTaker availableRowsTaker = new HttpAvailableRowsTaker(ringClient, baInterner, mapper);
        AquariumStats aquariumStats = new AquariumStats();

        QuorumTimeouts quorumTimeoutsConfig = bindDefault(QuorumTimeouts.class);

        TriggerTimeoutHealthCheck quorumTimeoutHealthCheck = new TriggerTimeoutHealthCheck(
            () -> amzaStats.getGrandTotal().quorumTimeouts.longValue(),
            quorumTimeoutsConfig
        );
        //deployable.addHealthCheck(quorumTimeoutHealthCheck);

        //LABPointerIndexConfig amzaLabConfig = deployable.config(LABPointerIndexConfig.class);
        LABPointerIndexConfig amzaLabConfig = bindDefault(UpenaLABPointerIndexConfig.class);

        AmzaServiceConfig amzaServiceConfig = new AmzaServiceConfig();
        Set<RingMember> blacklistRingMembers = Sets.newHashSet();
        AmzaService amzaService = new AmzaServiceInitializer().initialize(amzaServiceConfig,
            baInterner,
            aquariumStats,
            amzaStats,
            quorumLatency,
            () -> {
                Callable<RingTopology> ringTopologyCallable = topologyProvider.get();
                if (ringTopologyCallable != null) {
                    try {
                        return ringTopologyCallable.call().entries.size();
                    } catch (Exception x) {
                        LOG.error("issue determining system ring size", x);
                    }
                }
                return -1;
            },
            sickThreads,
            sickPartitions,
            primaryRowMarshaller,
            highwaterRowMarshaller,
            ringMember,
            ringHost,
            blacklistRingMembers,
            orderIdProvider,
            idPacker,
            partitionPropertyMarshaller,
            (workingIndexDirectories,
                indexProviderRegistry,
                ephemeralRowIOProvider,
                persistentRowIOProvider,
                partitionStripeFunction) -> {

                indexProviderRegistry.register(
                    new BerkeleyDBWALIndexProvider(BerkeleyDBWALIndexProvider.INDEX_CLASS_NAME,
                        partitionStripeFunction,
                        workingIndexDirectories),
                    persistentRowIOProvider);

                indexProviderRegistry.register(new LABPointerIndexWALIndexProvider(amzaLabConfig,
                        LABPointerIndexWALIndexProvider.INDEX_CLASS_NAME,
                        partitionStripeFunction,
                        workingIndexDirectories),
                    persistentRowIOProvider);
            },
            availableRowsTaker,
            systemRowsTakerFactory,
            rowsTakerFactory,
            Optional.<TakeFailureListener>absent(),
            rowsChanged -> {
            });


        String peers = System.getProperty("manual.peers");
        if (peers != null) {
            String[] hostPortTuples = peers.split(",");
            for (String hostPortTuple : hostPortTuples) {
                String hostPort = hostPortTuple.trim();
                if (hostPort.length() > 0 && hostPort.contains(":")) {
                    String[] host_port = hostPort.split(":");
                    try {
                        String host = host_port[0].trim();
                        int port = Integer.parseInt(host_port[1].trim());
                        RingTopology ring = amzaService.getRingReader().getRing(AmzaRingReader.SYSTEM_RING, -1);
                        for (RingMemberAndHost ringMemberAndHost : ring.entries) {
                            if (!ringMemberAndHost.ringHost.getHost().equals(host) && ringMemberAndHost.ringHost.getPort() != port) {
                                continue;
                            }
                            amzaService.getRingWriter().register(new RingMember(host + ":" + port), new RingHost("", "", host, port), 0L, false);
                            break;
                        }
                    } catch (Exception x) {
                        LOG.warn("Malformed hostPortTuple {}", hostPort);
                    }
                } else {
                    LOG.warn("Malformed hostPortTuple {}", hostPort);
                }
            }
        }

        topologyProvider.set(() -> amzaService.getRingReader().getRing(AmzaRingReader.SYSTEM_RING, -1));


        return amzaService;
    }


    private void injectAmza(BAInterner baInterner,
        UpenaJerseyEndpoints jerseyEndpoints,
        AmzaService amzaService,
        TenantAwareHttpClient<String> ringClient) {

        /*AmzaClientProvider<HttpClient, HttpClientException> clientProvider = new AmzaClientProvider<>(
            new HttpPartitionClientFactory(baInterner),
            new HttpPartitionHostsProvider(baInterner, ringClient, mapper),
            new RingHostHttpClientProvider(ringClient),
            Executors.newCachedThreadPool(),
            10_000, //TODO expose to conf
            -1,
            -1);*/



        /*new AmzaUIInitializer().initialize("upena", ringHost, amzaService, clientProvider, aquariumStats, amzaStats, timestampProvider, idPacker,
            new AmzaUIInitializer.InjectionCallback() {

                @Override
                public void addEndpoint(Class clazz) {
                    deployable.addEndpoints(clazz);
                }

                @Override
                public void addInjectable(Class clazz, Object instance) {
                    deployable.addInjectables(clazz, instance);
                }

                @Override
                public void addSessionAuth(String... paths) throws Exception {
                    deployable.addSessionAuth(paths);
                }
            });*/

        jerseyEndpoints.addEndpoint(AmzaReplicationRestEndpoints.class);
        jerseyEndpoints.addInjectable(AmzaService.class, amzaService);
        jerseyEndpoints.addInjectable(AmzaRingWriter.class, amzaService.getRingWriter());
        jerseyEndpoints.addInjectable(AmzaRingReader.class, amzaService.getRingReader());
        jerseyEndpoints.addInjectable(AmzaInstance.class, amzaService);
        jerseyEndpoints.addInjectable(BAInterner.class, baInterner);

       /* Resource staticResource = new Resource(null)
            .addClasspathResource("resources/static/amza")
            .setDirectoryListingAllowed(false)
            .setContext("/static/amza");
        deployable.addResource(staticResource);*/
    }

    interface UpenaLABPointerIndexConfig extends LABPointerIndexConfig {

        @LongDefault(100_000_000L)
        long getSplitWhenValuesAndKeysTotalExceedsNBytes();

        @StringDefault("upena")
        String getHeapPressureName();

        @LongDefault(200_000_000L)
        long getGlobalBlockOnHeapPressureInBytes();

        @LongDefault(100_000_000L)
        long getGlobalMaxHeapPressureInBytes();

        @LongDefault(1_000_000L)
        long getMaxHeapPressureInBytes();
    }

    interface AmzaSickThreadsHealthConfig extends SickHealthCheckConfig {

        @Override
        @StringDefault("sick>threads")
        String getName();

        @Override
        @StringDefault("No sick threads")
        String getDescription();

        @Override
        @DoubleDefault(0.2)
        Double getSickHealth();
    }

    public interface QuorumLatency extends TimerHealthCheckConfig {

        @StringDefault("ack>quorum>latency")
        @Override
        String getName();

        @StringDefault("How long its taking to achieve quorum.")
        @Override
        String getDescription();

        @DoubleDefault(30000d)
        @Override
        Double get95ThPecentileMax();
    }

    private final HealthTimer quorumLatency = HealthFactory.getHealthTimer(QuorumLatency.class, TimerHealthChecker.FACTORY);

}
