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
package com.jivesoftware.os.upena.amza.shared;

import java.util.List;

public interface RowsStorage extends RowScanable {

    void load() throws Exception;

    // TODO Consider using a call back stream instead of returning RowsChanged
    RowsChanged update(RowScanable rowUpdates) throws Exception;

    RowIndexValue get(RowIndexKey key) throws Exception;

    boolean containsKey(RowIndexKey key) throws Exception;

    List<RowIndexValue> get(List<RowIndexKey> keys) throws Exception;

    List<Boolean> containsKey(List<RowIndexKey> keys) throws Exception;

    void takeRowUpdatesSince(final long transactionId, RowScan rowUpdates) throws Exception;

    void compactTombstone(long removeTombstonedOlderThanNMillis) throws Exception;

    void clear() throws Exception;

}
