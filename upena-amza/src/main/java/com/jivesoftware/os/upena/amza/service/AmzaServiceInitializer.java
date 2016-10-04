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

import com.google.common.base.Optional;
import com.jivesoftware.os.upena.amza.service.storage.TableStoreProvider;
import com.jivesoftware.os.upena.amza.service.storage.replication.HostRingProvider;
import com.jivesoftware.os.upena.amza.service.storage.replication.MemoryBackedHighWaterMarks;
import com.jivesoftware.os.upena.amza.service.storage.replication.SendFailureListener;
import com.jivesoftware.os.upena.amza.service.storage.replication.TableReplicator;
import com.jivesoftware.os.upena.amza.service.storage.replication.TakeFailureListener;
import com.jivesoftware.os.upena.amza.shared.Marshaller;
import com.jivesoftware.os.upena.amza.shared.RowChanges;
import com.jivesoftware.os.upena.amza.shared.RowsChanged;
import com.jivesoftware.os.upena.amza.shared.RowsStorageProvider;
import com.jivesoftware.os.upena.amza.shared.UpdatesSender;
import com.jivesoftware.os.upena.amza.shared.UpdatesTaker;
import com.jivesoftware.os.jive.utils.ordered.id.TimestampedOrderIdProvider;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class AmzaServiceInitializer {

    public static class AmzaServiceConfig {

        public String workingDirectory = "./var/data/";
        public int replicationFactor = 1;
        public int takeFromFactor = 1;
        public int resendReplicasIntervalInMillis = 1000;
        public int applyReplicasIntervalInMillis = 1000;
        public int takeFromNeighborsIntervalInMillis = 1000;
        public long checkIfCompactionIsNeededIntervalInMillis = 60_000;
        public long compactTombstoneIfOlderThanNMillis = 30 * 24 * 60 * 60 * 1000L;
    }

    public AmzaService initialize(AmzaServiceConfig config,
        TimestampedOrderIdProvider orderIdProvider,
        Marshaller marshaller,
        RowsStorageProvider amzaStores,
        RowsStorageProvider amzaReplicasWAL,
        RowsStorageProvider amzaResendWAL,
        UpdatesSender updatesSender,
        UpdatesTaker updatesTaker,
        Optional<SendFailureListener> sendFailureListener,
        Optional<TakeFailureListener> takeFailureListener,
        final RowChanges allRowChanges) throws Exception {

        final AtomicReference<HostRingProvider> hostRingProvider = new AtomicReference<>();
        final AtomicReference<TableReplicator> replicator = new AtomicReference<>();
        RowChanges tableStateChanges = new RowChanges() {
            @Override
            public void changes(RowsChanged rowsChanged) throws Exception {
                TableReplicator tableReplicator = replicator.get();
                tableReplicator.replicateLocalUpdates(hostRingProvider.get(), rowsChanged.getTableName(), rowsChanged, true);
                allRowChanges.changes(rowsChanged);
            }
        };
        AmzaTableWatcher amzaTableWatcher = new AmzaTableWatcher(tableStateChanges);

        File workingDirectory = new File(config.workingDirectory);

        TableStoreProvider storesProvider = new TableStoreProvider(workingDirectory, "amza/stores", amzaStores, amzaTableWatcher);
        TableStoreProvider replicasProvider = new TableStoreProvider(workingDirectory, "amza/WAL/replicated", amzaReplicasWAL, null);
        TableStoreProvider resendsProvider = new TableStoreProvider(workingDirectory, "amza/WAL/resend", amzaResendWAL, null);

        MemoryBackedHighWaterMarks highWaterMarks = new MemoryBackedHighWaterMarks();

        TableReplicator tableReplicator = new TableReplicator(storesProvider,
            config.replicationFactor,
            config.takeFromFactor,
            highWaterMarks,
            replicasProvider,
            resendsProvider,
            updatesSender,
            updatesTaker,
            sendFailureListener,
            takeFailureListener);
        replicator.set(tableReplicator);

        AmzaService service = new AmzaService(orderIdProvider, marshaller, tableReplicator, storesProvider, amzaTableWatcher);
        hostRingProvider.set(service);
        return service;
    }
}
