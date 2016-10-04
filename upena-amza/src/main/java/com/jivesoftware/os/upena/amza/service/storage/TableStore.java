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
package com.jivesoftware.os.upena.amza.service.storage;

import com.jivesoftware.os.upena.amza.shared.RowChanges;
import com.jivesoftware.os.upena.amza.shared.RowIndexKey;
import com.jivesoftware.os.upena.amza.shared.RowIndexValue;
import com.jivesoftware.os.upena.amza.shared.RowScan;
import com.jivesoftware.os.upena.amza.shared.RowScanable;
import com.jivesoftware.os.upena.amza.shared.RowsChanged;
import com.jivesoftware.os.upena.amza.shared.RowsStorage;

public class TableStore implements RowScanable {

    private final RowsStorage rowsStorage;
    private final RowChanges rowChanges;

    public TableStore(RowsStorage rowsStorage, RowChanges rowChanges) {
        this.rowsStorage = rowsStorage;
        this.rowChanges = rowChanges;
    }

    public void load() throws Exception {
        rowsStorage.load();
    }

    @Override
    public <E extends Exception> void rowScan(RowScan<E> stream) throws E {
        rowsStorage.rowScan(stream);
    }

    @Override
    public <E extends Exception> void rangeScan(RowIndexKey from, RowIndexKey to, RowScan<E> rowScan) throws E {
        rowsStorage.rangeScan(from, to, rowScan);
    }

    public void compactTombstone(long removeTombstonedOlderThanTimestampId) throws Exception {
        rowsStorage.compactTombstone(removeTombstonedOlderThanTimestampId);
    }

    public RowIndexValue get(RowIndexKey key) throws Exception {
        return rowsStorage.get(key);
    }

    public boolean containsKey(RowIndexKey key) throws Exception {
        return rowsStorage.containsKey(key);
    }

    public void takeRowUpdatesSince(long transactionId, RowScan rowUpdates) throws Exception {
        rowsStorage.takeRowUpdatesSince(transactionId, rowUpdates);
    }

    public RowStoreUpdates startTransaction(long timestamp) throws Exception {
        return new RowStoreUpdates(this, new RowsStorageUpdates(rowsStorage, timestamp));
    }

    public void commit(RowScanable rowUpdates) throws Exception {
        RowsChanged updateMap = rowsStorage.update(rowUpdates);
        if (rowChanges != null && !updateMap.isEmpty()) {
            rowChanges.changes(updateMap);
        }
    }

    public void clear() throws Exception {
        rowsStorage.clear();
    }

}
