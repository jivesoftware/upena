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
import com.jivesoftware.os.amza.service.AmzaService;
import com.jivesoftware.os.amza.service.AmzaServiceInitializer;
import com.jivesoftware.os.amza.service.AmzaServiceInitializer.AmzaServiceConfig;
import com.jivesoftware.os.amza.service.discovery.AmzaDiscovery;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.amza.shared.RingHost;
import com.jivesoftware.os.amza.shared.TableDelta;
import com.jivesoftware.os.amza.shared.TableIndex;
import com.jivesoftware.os.amza.shared.TableIndexProvider;
import com.jivesoftware.os.amza.shared.TableName;
import com.jivesoftware.os.amza.shared.TableStateChanges;
import com.jivesoftware.os.amza.shared.TableStorage;
import com.jivesoftware.os.amza.shared.TableStorageProvider;
import com.jivesoftware.os.amza.storage.FileBackedTableStorage;
import com.jivesoftware.os.amza.storage.RowTable;
import com.jivesoftware.os.amza.storage.binary.BinaryRowMarshaller;
import com.jivesoftware.os.amza.storage.binary.BinaryRowReader;
import com.jivesoftware.os.amza.storage.binary.BinaryRowWriter;
import com.jivesoftware.os.amza.storage.chunks.Filer;
import com.jivesoftware.os.amza.storage.index.MapDBTableIndex;
import com.jivesoftware.os.amza.transport.http.replication.HttpChangeSetSender;
import com.jivesoftware.os.amza.transport.http.replication.HttpChangeSetTaker;
import com.jivesoftware.os.amza.transport.http.replication.endpoints.AmzaReplicationRestEndpoints;
import com.jivesoftware.os.jive.utils.base.service.ServiceHandle;
import com.jivesoftware.os.jive.utils.ordered.id.OrderIdProvider;
import com.jivesoftware.os.jive.utils.ordered.id.OrderIdProviderImpl;
import com.jivesoftware.os.server.http.jetty.jersey.server.InitializeRestfulServer;
import com.jivesoftware.os.server.http.jetty.jersey.server.JerseyEndpoints;
import com.jivesoftware.os.upena.config.UpenaConfigRestEndpoints;
import com.jivesoftware.os.upena.config.UpenaConfigStore;
import com.jivesoftware.os.upena.routing.shared.InstanceChanged;
import com.jivesoftware.os.upena.routing.shared.TenantChanged;
import com.jivesoftware.os.upena.service.InstanceChanges;
import com.jivesoftware.os.upena.service.TenantChanges;
import com.jivesoftware.os.upena.service.UpenaRestEndpoints;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.uba.service.UbaService;
import com.jivesoftware.os.upena.uba.service.UbaServiceInitializer;
import com.jivesoftware.os.upena.uba.service.endpoints.UbaServiceRestEndpoints;
import de.ruedigermoeller.serialization.FSTConfiguration;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

    public void run(String[] args) throws Exception {

        String hostname = args[0];
        int port = Integer.parseInt(System.getProperty("amza.port", "1175"));
        String multicastGroup = System.getProperty("amza.multicast.group", "225.4.5.6");
        int multicastPort = Integer.parseInt(System.getProperty("amza.multicast.port", "1123"));
        String clusterName = (args.length > 1 ? args[1] : null);

        RingHost ringHost = new RingHost(hostname, port);
        final OrderIdProvider orderIdProvider = new OrderIdProviderImpl(new Random().nextInt(512)); // todo need a better way to create writter id.

        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);

        TableStorageProvider tableStorageProvider = new TableStorageProvider() {
            @Override
            public TableStorage createTableStorage(File workingDirectory,
                    String tableDomain,
                    TableName tableName) throws Exception {
                File directory = new File(workingDirectory, tableDomain);
                directory.mkdirs();
                File file = new File(directory, tableName.getTableName() + ".kvt");

                Filer filer = Filer.open(file, "rw");
                BinaryRowReader reader = new BinaryRowReader(filer);
                BinaryRowWriter writer = new BinaryRowWriter(filer);
                BinaryRowMarshaller rowMarshaller = new BinaryRowMarshaller();
                TableIndexProvider tableIndexProvider = new TableIndexProvider() {

                    @Override
                    public TableIndex createTableIndex(TableName tableName) {
                        return new MapDBTableIndex(tableName.getTableName());
                    }
                };
                RowTable<byte[]> rowTableFile = new RowTable<>(tableName,
                        orderIdProvider,
                        tableIndexProvider,
                        rowMarshaller,
                        reader,
                        writer);
                return new FileBackedTableStorage(rowTableFile);
            }
        };

        AmzaServiceConfig amzaServiceConfig = new AmzaServiceInitializer.AmzaServiceConfig();
        AmzaService amzaService = new AmzaServiceInitializer().initialize(amzaServiceConfig,
                orderIdProvider,
                new com.jivesoftware.os.amza.storage.FstMarshaller(FSTConfiguration.getDefaultConfiguration()),
                tableStorageProvider,
                tableStorageProvider,
                tableStorageProvider,
                new HttpChangeSetSender(),
                new HttpChangeSetTaker(), new TableStateChanges() {

                    @Override
                    public void changes(TableName partitionName, TableDelta changes) throws Exception {
                        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }
                });

        amzaService.start(ringHost, amzaServiceConfig.resendReplicasIntervalInMillis,
                amzaServiceConfig.applyReplicasIntervalInMillis,
                amzaServiceConfig.takeFromNeighborsIntervalInMillis,
                amzaServiceConfig.compactTombstoneIfOlderThanNMillis);

        System.out.println("-----------------------------------------------------------------------");
        System.out.println("|      Amza Service Online");
        System.out.println("-----------------------------------------------------------------------");

        final UpenaConfigStore upenaConfigStore = new UpenaConfigStore(amzaService);

        System.out.println("-----------------------------------------------------------------------");
        System.out.println("|      Upena Config Store Online");
        System.out.println("-----------------------------------------------------------------------");

        final AtomicReference<UbaService> conductor = new AtomicReference<>();
        UpenaStore upenaStore = new UpenaStore(amzaService, new InstanceChanges() {
            @Override
            public void changed(final List<InstanceChanged> instanceChanges) throws Exception {

                Executors.newSingleThreadExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        UbaService got = conductor.get();
                        if (got != null) {
                            try {
                                got.instanceChanged(instanceChanges);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });
            }
        }, new InstanceChanges() {

            @Override
            public void changed(List<InstanceChanged> changes) throws Exception {
                for (InstanceChanged instanceChanged : changes) {
                    upenaConfigStore.remove(instanceChanged.getInstanceId(), "default");
                    upenaConfigStore.remove(instanceChanged.getInstanceId(), "override");
                }
            }
        }, new TenantChanges() {
            @Override
            public void changed(List<TenantChanged> change) throws Exception {
                System.out.println("TODO: tie into conductor. " + change);
            }
        });
        upenaStore.attachWatchers();

        UpenaService upenaService = new UpenaService(upenaStore);

        String workingDir = System.getProperty("user.dir");
        Host host = new Host(ringHost.getHost(), ringHost.getHost(), ringHost.getPort(), workingDir, null);
        HostKey hostKey = upenaStore.hosts.toKey(host);
        Host gotHost = upenaStore.hosts.get(hostKey);
        if (gotHost == null) {
            HashMap<ServiceKey, ReleaseGroupKey> defaultReleaseGroups = new HashMap<>();
            Cluster cluster = new Cluster("adhoc", "default adhoc cluster", defaultReleaseGroups);
            ClusterKey clusterKey = upenaStore.clusters.toKey(cluster);
            Cluster gotCluster = upenaStore.clusters.get(clusterKey);
            if (gotCluster == null) {
                upenaStore.clusters.update(clusterKey, cluster);
            }
            host = new Host(ringHost.getHost(), ringHost.getHost(), ringHost.getPort(), workingDir, clusterKey);
            upenaStore.hosts.update(null, host);
        }

        System.out.println("-----------------------------------------------------------------------");
        System.out.println("|      Upena Service Online");
        System.out.println("-----------------------------------------------------------------------");


        final UbaService conductorService = new UbaServiceInitializer().initialize(hostKey.getKey(),
                workingDir,
                ringHost.getHost(),
                ringHost.getPort());

        JerseyEndpoints jerseyEndpoints = new JerseyEndpoints()
                .addEndpoint(UpenaRestEndpoints.class)
                .addInjectable(upenaService)
                .addInjectable(upenaStore)
                .addEndpoint(UpenaConfigRestEndpoints.class)
                .addInjectable(upenaConfigStore)
                .addEndpoint(UbaServiceRestEndpoints.class)
                .addInjectable(conductorService)
                .addEndpoint(AmzaReplicationRestEndpoints.class)
                .addInjectable(AmzaInstance.class, amzaService);

        InitializeRestfulServer initializeRestfulServer = new InitializeRestfulServer(port, "UpenaNode", 128, 10000);
        initializeRestfulServer.addContextHandler("/", jerseyEndpoints);
        ServiceHandle serviceHandle = initializeRestfulServer.build();
        serviceHandle.start();

        System.out.println("-----------------------------------------------------------------------");
        System.out.println("|      Jetty Service Online");
        System.out.println("-----------------------------------------------------------------------");

        if (conductorService != null) {
            Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        conductorService.nanny();
                    } catch (Exception ex) {
                        ex.printStackTrace(); // HACK
                    }
                }
            }, 1, 1, TimeUnit.MINUTES);
            System.out.println("-----------------------------------------------------------------------");
            System.out.println("|      Uba Service Online");
            System.out.println("-----------------------------------------------------------------------");
        }

        conductor.set(conductorService);

        if (clusterName != null) {
            AmzaDiscovery amzaDiscovery = new AmzaDiscovery(amzaService, ringHost, clusterName, multicastGroup, multicastPort);
            amzaDiscovery.start();
            System.out.println("-----------------------------------------------------------------------");
            System.out.println("|      Amza Service Discovery Online");
            System.out.println("-----------------------------------------------------------------------");
        } else {
            System.out.println("-----------------------------------------------------------------------");
            System.out.println("|     Amze Service is in manual Discovery mode.  No cluster name was specified");
            System.out.println("-----------------------------------------------------------------------");
        }
    }
}
