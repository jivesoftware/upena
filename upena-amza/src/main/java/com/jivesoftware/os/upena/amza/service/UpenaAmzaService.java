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
package com.jivesoftware.os.upena.amza.service;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jivesoftware.os.jive.utils.ordered.id.TimestampedOrderIdProvider;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.amza.service.storage.RowStoreUpdates;
import com.jivesoftware.os.upena.amza.service.storage.TableStore;
import com.jivesoftware.os.upena.amza.service.storage.TableStoreProvider;
import com.jivesoftware.os.upena.amza.service.storage.replication.HostRing;
import com.jivesoftware.os.upena.amza.service.storage.replication.HostRingBuilder;
import com.jivesoftware.os.upena.amza.service.storage.replication.HostRingProvider;
import com.jivesoftware.os.upena.amza.service.storage.replication.TableReplicator;
import com.jivesoftware.os.upena.amza.shared.AmzaInstance;
import com.jivesoftware.os.upena.amza.shared.Marshaller;
import com.jivesoftware.os.upena.amza.shared.UpenaRingHost;
import com.jivesoftware.os.upena.amza.shared.RowChanges;
import com.jivesoftware.os.upena.amza.shared.RowIndexKey;
import com.jivesoftware.os.upena.amza.shared.RowIndexValue;
import com.jivesoftware.os.upena.amza.shared.RowScan;
import com.jivesoftware.os.upena.amza.shared.RowScanable;
import com.jivesoftware.os.upena.amza.shared.TableName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 *
 * Amza pronounced (AH m z ah )
 *
 * Sanskrit word meaning partition / share.
 *
 *
 */
public class UpenaAmzaService implements HostRingProvider, AmzaInstance {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private UpenaRingHost ringHost;
    private ScheduledExecutorService scheduledThreadPool;
    private final TimestampedOrderIdProvider orderIdProvider;
    private final Marshaller marshaller;
    private final TableReplicator tableReplicator;
    private final AmzaTableWatcher amzaTableWatcher;
    private final TableStoreProvider tableStoreProvider;
    private final TableName tableIndexKey = new TableName("MASTER", "TABLE_INDEX", null, null);

    public UpenaAmzaService(TimestampedOrderIdProvider orderIdProvider,
        Marshaller marshaller,
        TableReplicator tableReplicator,
        TableStoreProvider tableStoreProvider,
        AmzaTableWatcher amzaTableWatcher) {
        this.orderIdProvider = orderIdProvider;
        this.marshaller = marshaller;
        this.tableReplicator = tableReplicator;
        this.tableStoreProvider = tableStoreProvider;
        this.amzaTableWatcher = amzaTableWatcher;
    }

    synchronized public UpenaRingHost ringHost() {
        return ringHost;
    }

    synchronized public void start(UpenaRingHost ringHost,
        long resendReplicasIntervalInMillis,
        long applyReplicasIntervalInMillis,
        long takeFromNeighborsIntervalInMillis,
        long checkIfCompactionIsNeededIntervalInMillis,
        final long removeTombstonedOlderThanNMilli) throws Exception {

        final int silenceBackToBackErrors = 100;
        if (scheduledThreadPool == null) {
            this.ringHost = ringHost;
            ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("tableReplicator-%d").build();
            scheduledThreadPool = Executors.newScheduledThreadPool(4,namedThreadFactory);
            scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
                int failedToSend = 0;

                @Override
                public void run() {
                    try {
                        failedToSend = 0;
                        tableReplicator.resendLocalChanges(UpenaAmzaService.this);
                    } catch (Exception x) {
                        LOG.debug("Failed while resending replicas.", x);
                        if (failedToSend % silenceBackToBackErrors == 0) {
                            failedToSend++;
                            LOG.error("Failed while resending replicas.", x);
                        }
                    }
                }
            }, resendReplicasIntervalInMillis, resendReplicasIntervalInMillis, TimeUnit.MILLISECONDS);

            scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
                int failedToReceive = 0;

                @Override
                public void run() {
                    try {
                        failedToReceive = 0;
                        tableReplicator.applyReceivedChanges();
                    } catch (Exception x) {
                        LOG.debug("Failing to replay apply replication.", x);
                        if (failedToReceive % silenceBackToBackErrors == 0) {
                            failedToReceive++;
                            LOG.error("Failing to replay apply replication.", x);
                        }
                    }
                }
            }, applyReplicasIntervalInMillis, applyReplicasIntervalInMillis, TimeUnit.MILLISECONDS);

            scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
                int failedToTake = 0;

                @Override
                public void run() {
                    try {
                        failedToTake = 0;
                        tableReplicator.takeChanges(UpenaAmzaService.this);
                    } catch (Exception x) {
                        LOG.debug("Failing to take from above and below.", x);
                        if (failedToTake % silenceBackToBackErrors == 0) {
                            failedToTake++;
                            LOG.error("Failing to take from above and below.");
                        }
                    }
                }
            }, takeFromNeighborsIntervalInMillis, takeFromNeighborsIntervalInMillis, TimeUnit.MILLISECONDS);

            scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
                int failedToCompact = 0;

                @Override
                public void run() {
                    try {
                        failedToCompact = 0;
                        long removeIfOlderThanTimestmapId = orderIdProvider.getApproximateId(System.currentTimeMillis() - removeTombstonedOlderThanNMilli);
                        tableReplicator.compactTombstone(removeIfOlderThanTimestmapId);
                    } catch (Exception x) {
                        LOG.debug("Failing to compact tombstones.", x);
                        if (failedToCompact % silenceBackToBackErrors == 0) {
                            failedToCompact++;
                            LOG.error("Failing to compact tombstones.");
                        }
                    }
                }
            }, checkIfCompactionIsNeededIntervalInMillis, checkIfCompactionIsNeededIntervalInMillis, TimeUnit.MILLISECONDS);

            tableReplicator.takeChanges(UpenaAmzaService.this);
        }
    }

    synchronized public void stop() throws Exception {
        this.ringHost = null;
        if (scheduledThreadPool != null) {
            this.scheduledThreadPool.shutdownNow();
            this.scheduledThreadPool = null;
        }
    }

    @Override
    public long getTimestamp(long timestampId, long wallClockMillis) throws Exception {
        return orderIdProvider.getApproximateId(timestampId, wallClockMillis);
    }

    @Override
    public void addRingHost(String ringName, UpenaRingHost ringHost) throws Exception {
        if (ringName == null) {
            throw new IllegalArgumentException("ringName cannot be null.");
        }
        if (ringHost == null) {
            throw new IllegalArgumentException("ringHost cannot be null.");
        }
        byte[] rawRingHost = marshaller.serialize(ringHost);
        TableName ringIndexKey = createRingTableName(ringName);
        TableStore ringIndex = tableStoreProvider.getTableStore(ringIndexKey);
        RowStoreUpdates tx = ringIndex.startTransaction(orderIdProvider.nextId());
        tx.add(new RowIndexKey(rawRingHost), rawRingHost);
        tx.commit();
    }

    @Override
    public void removeRingHost(String ringName, UpenaRingHost ringHost) throws Exception {
        if (ringName == null) {
            throw new IllegalArgumentException("ringName cannot be null.");
        }
        if (ringHost == null) {
            throw new IllegalArgumentException("ringHost cannot be null.");
        }
        byte[] rawRingHost = marshaller.serialize(ringHost);
        TableName ringIndexKey = createRingTableName(ringName);
        TableStore ringIndex = tableStoreProvider.getTableStore(ringIndexKey);
        RowStoreUpdates tx = ringIndex.startTransaction(orderIdProvider.nextId());
        tx.remove(new RowIndexKey(rawRingHost));
        tx.commit();
    }

    @Override
    public List<UpenaRingHost> getRing(String ringName) throws Exception {
        TableName ringIndexKey = createRingTableName(ringName);
        TableStore ringIndex = tableStoreProvider.getTableStore(ringIndexKey);
        if (ringIndex == null) {
            LOG.warn("No ring defined for ringName:" + ringName);
            return new ArrayList<>();
        } else {
            final Set<UpenaRingHost> ringHosts = new HashSet<>();
            List<RowIndexKey> badKeys = Lists.newArrayList();
            ringIndex.rowScan((orderId, key, value) -> {
                if (!value.getTombstoned()) {
                    try {
                        ringHosts.add(marshaller.deserialize(value.getValue(), UpenaRingHost.class));
                    } catch (Exception x) {
                        LOG.error("FAILED to deserialize RingHost:{}", new Object[]{value.getValue()}, x);
                        badKeys.add(key);
                    }
                }
                return true;
            });
            if (!badKeys.isEmpty()) {
                RowStoreUpdates updates = ringIndex.startTransaction(orderIdProvider.nextId());
                for (RowIndexKey key : badKeys) {
                    updates.remove(key);
                }
                updates.commit();
                LOG.info("Removed {} bad keys", badKeys.size());
            }
            return new ArrayList<>(ringHosts);
        }
    }

    @Override
    public HostRing getHostRing(String ringName) throws Exception {
        return new HostRingBuilder().build(ringHost, getRing(ringName));
    }

    private TableName createRingTableName(String ringName) {
        ringName = ringName.toUpperCase();
        return new TableName("MASTER", "RING_INDEX_" + ringName, null, null);
    }

    private boolean createTable(TableName tableName) throws Exception {
        byte[] rawTableName = marshaller.serialize(tableName);

        TableStore tableNameIndex = tableStoreProvider.getTableStore(tableIndexKey);
        RowIndexValue timestamptedTableKey = tableNameIndex.get(new RowIndexKey(rawTableName));
        if (timestamptedTableKey == null) {
            RowStoreUpdates tx = tableNameIndex.startTransaction(orderIdProvider.nextId());
            tx.add(new RowIndexKey(rawTableName), rawTableName);
            tx.commit();
            return true;
        } else {
            return !timestamptedTableKey.getTombstoned();
        }
    }

    public AmzaTable getTable(TableName tableName) throws Exception {
        byte[] rawTableName = marshaller.serialize(tableName);
        TableStore tableStoreIndex = tableStoreProvider.getTableStore(tableIndexKey);
        RowIndexValue timestampedKeyValueStoreName = tableStoreIndex.get(new RowIndexKey(rawTableName));
        while (timestampedKeyValueStoreName == null) {
            createTable(tableName);
            timestampedKeyValueStoreName = tableStoreIndex.get(new RowIndexKey(rawTableName));
        }
        if (timestampedKeyValueStoreName.getTombstoned()) {
            return null;
        } else {
            TableStore tableStore = tableStoreProvider.getTableStore(tableName);
            return new AmzaTable(orderIdProvider, tableName, tableStore);
        }
    }

    @Override
    public List<TableName> getTableNames() {
        List<TableName> amzaTableNames = new ArrayList<>();
        for (Entry<TableName, TableStore> tableStore : tableStoreProvider.getTableStores()) {
            amzaTableNames.add(tableStore.getKey());
        }
        return amzaTableNames;
    }

    public Map<TableName, AmzaTable> getTables() throws Exception {
        Map<TableName, AmzaTable> amzaTables = new HashMap<>();
        for (Entry<TableName, TableStore> tableStore : tableStoreProvider.getTableStores()) {
            amzaTables.put(tableStore.getKey(), new AmzaTable(orderIdProvider, tableStore.getKey(), tableStore.getValue()));
        }
        return amzaTables;
    }

    @Override
    public void destroyTable(TableName tableName) throws Exception {
        byte[] rawTableName = marshaller.serialize(tableName);
        TableStore tableIndex = tableStoreProvider.getTableStore(tableIndexKey);
        RowStoreUpdates tx = tableIndex.startTransaction(orderIdProvider.nextId());
        tx.remove(new RowIndexKey(rawTableName));
        tx.commit();
    }

    @Override
    public void updates(TableName tableName, RowScanable rowUpdates) throws Exception {
        tableReplicator.receiveChanges(tableName, rowUpdates);
    }

    public void watch(TableName tableName, RowChanges rowChanges) throws Exception {
        amzaTableWatcher.watch(tableName, rowChanges);
    }

    public RowChanges unwatch(TableName tableName) throws Exception {
        return amzaTableWatcher.unwatch(tableName);
    }

    @Override
    public void takeRowUpdates(TableName tableName, long transationId, RowScan rowUpdates) throws Exception {
        getTable(tableName).takeRowUpdatesSince(transationId, rowUpdates);
    }

    public void buildRandomSubRing(String ringName, int desiredRingSize) throws Exception {
        List<UpenaRingHost> ring = getRing("MASTER");
        if (ring.size() < desiredRingSize) {
            throw new IllegalStateException("Current master ring is not large enough to support a ring of size:" + desiredRingSize);
        }
        Collections.shuffle(ring);
        for (int i = 0; i < desiredRingSize; i++) {
            addRingHost(ringName, ring.get(i));
        }
    }

    //------ Used for debugging ------
    public void printService() throws Exception {
        for (Map.Entry<TableName, TableStore> table : tableStoreProvider.getTableStores()) {
            final TableName tableName = table.getKey();
            final TableStore sortedMapStore = table.getValue();
            sortedMapStore.rowScan(new RowScan<RuntimeException>() {

                @Override
                public boolean row(long orderId, RowIndexKey key, RowIndexValue value) throws RuntimeException {
                    System.out.println(ringHost.getHost() + ":" + ringHost.getPort()
                        + ":" + tableName.getTableName() + " k:" + key + " v:" + value.getValue()
                        + " d:" + value.getTombstoned() + " t:" + value.getTimestampId());
                    return true;
                }
            });
        }
    }
}
