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
package com.jivesoftware.os.upena.amza.test;

import com.google.common.base.Optional;
import com.jivesoftware.os.jive.utils.ordered.id.ConstantWriterIdProvider;
import com.jivesoftware.os.jive.utils.ordered.id.JiveEpochTimestampProvider;
import com.jivesoftware.os.jive.utils.ordered.id.OrderIdProviderImpl;
import com.jivesoftware.os.jive.utils.ordered.id.TimestampedOrderIdProvider;
import com.jivesoftware.os.upena.amza.service.AmzaChangeIdPacker;
import com.jivesoftware.os.upena.amza.service.UpenaAmzaService;
import com.jivesoftware.os.upena.amza.service.UpenaAmzaServiceInitializer;
import com.jivesoftware.os.upena.amza.service.UpenaAmzaServiceInitializer.UpenaAmzaServiceConfig;
import com.jivesoftware.os.upena.amza.service.AmzaTable;
import com.jivesoftware.os.upena.amza.service.storage.replication.SendFailureListener;
import com.jivesoftware.os.upena.amza.service.storage.replication.UpenaTakeFailureListener;
import com.jivesoftware.os.upena.amza.shared.MemoryRowsIndex;
import com.jivesoftware.os.upena.amza.shared.UpenaRingHost;
import com.jivesoftware.os.upena.amza.shared.RowIndexKey;
import com.jivesoftware.os.upena.amza.shared.RowScan;
import com.jivesoftware.os.upena.amza.shared.RowScanable;
import com.jivesoftware.os.upena.amza.shared.RowsChanged;
import com.jivesoftware.os.upena.amza.shared.RowsIndexProvider;
import com.jivesoftware.os.upena.amza.shared.RowsStorageProvider;
import com.jivesoftware.os.upena.amza.shared.TableName;
import com.jivesoftware.os.upena.amza.shared.UpdatesSender;
import com.jivesoftware.os.upena.amza.shared.UpdatesTaker;
import com.jivesoftware.os.upena.amza.storage.FstMarshaller;
import com.jivesoftware.os.upena.amza.storage.RowMarshaller;
import com.jivesoftware.os.upena.amza.storage.RowTable;
import com.jivesoftware.os.upena.amza.storage.binary.BinaryRowMarshaller;
import com.jivesoftware.os.upena.amza.storage.binary.BinaryRowsTx;
import de.ruedigermoeller.serialization.FSTConfiguration;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

public class AmzaTestCluster {

    private final File workingDirctory;
    private final ConcurrentSkipListMap<UpenaRingHost, AmzaNode> cluster = new ConcurrentSkipListMap<>();
    private int oddsOfAConnectionFailureWhenAdding = 0; // 0 never - 100 always
    private int oddsOfAConnectionFailureWhenTaking = 0; // 0 never - 100 always
    private UpenaAmzaService lastUpenaAmzaService = null;

    public AmzaTestCluster(File workingDirctory,
            int oddsOfAConnectionFailureWhenAdding,
            int oddsOfAConnectionFailureWhenTaking) {
        this.workingDirctory = workingDirctory;
        this.oddsOfAConnectionFailureWhenAdding = oddsOfAConnectionFailureWhenAdding;
        this.oddsOfAConnectionFailureWhenTaking = oddsOfAConnectionFailureWhenTaking;
    }

    public Collection<AmzaNode> getAllNodes() {
        return cluster.values();
    }

    public AmzaNode get(UpenaRingHost host) {
        return cluster.get(host);
    }

    public void remove(UpenaRingHost host) {
        cluster.remove(host);
    }

    public AmzaNode newNode(final UpenaRingHost serviceHost) throws Exception {

        AmzaNode service = cluster.get(serviceHost);
        if (service != null) {
            return service;
        }

        UpenaAmzaServiceConfig config = new UpenaAmzaServiceConfig();
        config.workingDirectory = workingDirctory.getAbsolutePath() + "/" + serviceHost.getHost() + "-" + serviceHost.getPort();
        config.replicationFactor = 4;
        config.takeFromFactor = 4;
        config.resendReplicasIntervalInMillis = 1000;
        config.applyReplicasIntervalInMillis = 100;
        config.takeFromNeighborsIntervalInMillis = 1000;
        config.compactTombstoneIfOlderThanNMillis = 1000L;

        UpdatesSender changeSetSender = (UpenaRingHost ringHost, TableName mapName, RowScanable changes) -> {
            AmzaNode service1 = cluster.get(ringHost);
            if (service1 == null) {
                throw new IllegalStateException("Service doesn't exists for " + ringHost);
            } else {
                service1.addToReplicatedWAL(mapName, changes);
            }
        };

        UpdatesTaker tableTaker = (UpenaRingHost ringHost, TableName mapName, long transationId, RowScan rowStream) -> {
            AmzaNode service1 = cluster.get(ringHost);
            if (service1 == null) {
                throw new IllegalStateException("Service doesn't exists for " + ringHost);
            } else {
                service1.takeTable(mapName, transationId, rowStream);
            }
        };

        // TODO need to get writer id from somewhere other than port.
        final TimestampedOrderIdProvider orderIdProvider = new OrderIdProviderImpl(new ConstantWriterIdProvider(serviceHost.getPort()),
                new AmzaChangeIdPacker(), new JiveEpochTimestampProvider());

        final RowsIndexProvider tableIndexProvider = (TableName tableName) -> new MemoryRowsIndex();

        RowsStorageProvider tableStorageProvider = (File workingDirectory, String tableDomain, TableName tableName) -> {
            File file = new File(workingDirectory, tableDomain + File.separator + tableName.getTableName() + ".kvt");
            file.getParentFile().mkdirs();
            RowMarshaller<byte[]> rowMarshaller = new BinaryRowMarshaller();
            return new RowTable(tableName,
                orderIdProvider,
                rowMarshaller,
                new BinaryRowsTx(file, rowMarshaller, tableIndexProvider, 100));
        };

        FstMarshaller marshaller = new FstMarshaller(FSTConfiguration.getDefaultConfiguration());
        UpenaAmzaService upenaAmzaService = new UpenaAmzaServiceInitializer().initialize(config,
                orderIdProvider,
                marshaller,
                tableStorageProvider,
                tableStorageProvider,
                tableStorageProvider,
                changeSetSender,
                tableTaker,
                Optional.<SendFailureListener>absent(),
                Optional.<UpenaTakeFailureListener>absent(), (RowsChanged changes) -> {
        });

        upenaAmzaService.start(serviceHost, config.resendReplicasIntervalInMillis,
                config.applyReplicasIntervalInMillis,
                config.takeFromNeighborsIntervalInMillis,
                config.checkIfCompactionIsNeededIntervalInMillis,
                config.compactTombstoneIfOlderThanNMillis);

        //if (serviceHost.getPort() % 2 == 0) {
        final TableName tableName = new TableName("test", "table1", null, null);
        upenaAmzaService.watch(tableName, (RowsChanged changes) -> {
            if (changes.getApply().size() > 0) {
                System.out.println("Service:" + serviceHost
                    + " Table:" + tableName.getTableName()
                    + " Changed:" + changes.getApply().size());
            }
        });
        //}

        upenaAmzaService.addRingHost("test", serviceHost); // ?? Hacky
        upenaAmzaService.addRingHost("MASTER", serviceHost); // ?? Hacky
        if (lastUpenaAmzaService != null) {
            upenaAmzaService.addRingHost("test", lastUpenaAmzaService.ringHost()); // ?? Hacky
            upenaAmzaService.addRingHost("MASTER", lastUpenaAmzaService.ringHost()); // ?? Hacky

            lastUpenaAmzaService.addRingHost("test", serviceHost); // ?? Hacky
            lastUpenaAmzaService.addRingHost("MASTER", serviceHost); // ?? Hacky
        }
        lastUpenaAmzaService = upenaAmzaService;

        service = new AmzaNode(serviceHost, upenaAmzaService);
        cluster.put(serviceHost, service);
        System.out.println("Added serviceHost:" + serviceHost + " to the cluster.");
        return service;
    }

    public class AmzaNode {

        private final Random random = new Random();
        private final UpenaRingHost serviceHost;
        private final UpenaAmzaService upenaAmzaService;
        private boolean off = false;
        private int flapped = 0;

        public AmzaNode(UpenaRingHost serviceHost, UpenaAmzaService upenaAmzaService) {
            this.serviceHost = serviceHost;
            this.upenaAmzaService = upenaAmzaService;
        }

        @Override
        public String toString() {
            return serviceHost.toString();
        }

        public boolean isOff() {
            return off;
        }

        public void setOff(boolean off) {
            this.off = off;
            flapped++;
        }

        public void stop() throws Exception {
            upenaAmzaService.stop();
        }

        void addToReplicatedWAL(TableName mapName, RowScanable changes) throws Exception {
            if (off) {
                throw new RuntimeException("Service is off:" + serviceHost);
            }
            if (random.nextInt(100) > (100 - oddsOfAConnectionFailureWhenAdding)) {
                throw new RuntimeException("Random connection failure:" + serviceHost);
            }
            upenaAmzaService.updates(mapName, changes);
        }

        public void update(TableName tableName, RowIndexKey k, byte[] v, long timestamp, boolean tombstone) throws Exception {
            if (off) {
                throw new RuntimeException("Service is off:" + serviceHost);
            }
            AmzaTable amzaTable = upenaAmzaService.getTable(tableName);
            if (tombstone) {
                amzaTable.remove(k);
            } else {
                amzaTable.set(k, v);
            }

        }

        public byte[] get(TableName tableName, RowIndexKey key) throws Exception {
            if (off) {
                throw new RuntimeException("Service is off:" + serviceHost);
            }
            AmzaTable amzaTable = upenaAmzaService.getTable(tableName);
            return amzaTable.get(key);
        }

        public void takeTable(TableName tableName, long transationId, RowScan rowStream) throws Exception {
            if (off) {
                throw new RuntimeException("Service is off:" + serviceHost);
            }
            if (random.nextInt(100) > (100 - oddsOfAConnectionFailureWhenTaking)) {
                throw new RuntimeException("Random take failure:" + serviceHost);
            }
            AmzaTable got = upenaAmzaService.getTable(tableName);
            if (got != null) {
                got.takeRowUpdatesSince(transationId, rowStream);
            }
        }

        public void printService() throws Exception {
            if (off) {
                System.out.println(serviceHost.getHost() + ":" + serviceHost.getPort() + " is OFF flapped:" + flapped);
                return;
            }
            upenaAmzaService.printService();
        }

        public boolean compare(AmzaNode service) throws Exception {
            if (off || service.off) {
                return true;
            }
            Map<TableName, AmzaTable> aTables = upenaAmzaService.getTables();
            Map<TableName, AmzaTable> bTables = service.upenaAmzaService.getTables();

            Set<TableName> tableNames = new HashSet<>();
            tableNames.addAll(aTables.keySet());
            tableNames.addAll(bTables.keySet());

            for (TableName tableName : tableNames) {
                AmzaTable a = upenaAmzaService.getTable(tableName);
                AmzaTable b = service.upenaAmzaService.getTable(tableName);
                if (!a.compare(b)) {
                    return false;
                }
            }
            return true;
        }
    }
}
