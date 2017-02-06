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
package com.jivesoftware.os.upena.amza.transport.http.replication;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelperUtils;
import com.jivesoftware.os.routing.bird.http.client.OAuthSigner;
import com.jivesoftware.os.upena.amza.shared.UpenaRingHost;
import com.jivesoftware.os.upena.amza.shared.RowIndexKey;
import com.jivesoftware.os.upena.amza.shared.RowIndexValue;
import com.jivesoftware.os.upena.amza.shared.RowScanable;
import com.jivesoftware.os.upena.amza.shared.TableName;
import com.jivesoftware.os.upena.amza.shared.UpdatesSender;
import com.jivesoftware.os.upena.amza.storage.binary.BinaryRowMarshaller;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class HttpUpdatesSender implements UpdatesSender {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final ConcurrentHashMap<UpenaRingHost, HttpRequestHelper> requestHelpers = new ConcurrentHashMap<>();
    private final boolean sslEnable;
    private final boolean allowSelfSignedCerts;
    private final OAuthSigner signer;

    public HttpUpdatesSender(boolean sslEnable, boolean allowSelfSignedCerts, OAuthSigner signer) {
        this.sslEnable = sslEnable;
        this.allowSelfSignedCerts = allowSelfSignedCerts;
        this.signer = signer;
    }

    @Override
    public void sendUpdates(UpenaRingHost ringHost, TableName tableName, RowScanable changes) throws Exception {

        final BinaryRowMarshaller rowMarshaller = new BinaryRowMarshaller();
        final List<byte[]> rows = new ArrayList<>();
        changes.rowScan((long orderId, RowIndexKey key, RowIndexValue value) -> {
            // We make this copy because we don't know how the value is being stored. By calling value.getValue()
            // we ensure that the value from the tableIndex is real vs a pointer.
            RowIndexValue copy = new RowIndexValue(value.getValue(), value.getTimestampId(), value.getTombstoned());
            rows.add(rowMarshaller.toRow(orderId, key, copy));
            return true;
        });
        if (!rows.isEmpty()) {
            LOG.debug("Pushing " + rows.size() + " changes to " + ringHost);
            RowUpdates changeSet = new RowUpdates(-1, tableName, rows);
            getRequestHelper(ringHost).executeRequest(changeSet, "/amza/changes/add", Boolean.class, false);
        }
    }

    HttpRequestHelper getRequestHelper(UpenaRingHost ringHost) throws Exception {
        HttpRequestHelper requestHelper = requestHelpers.get(ringHost);
        if (requestHelper == null) {
            requestHelper = HttpRequestHelperUtils.buildRequestHelper(sslEnable, allowSelfSignedCerts, signer, ringHost.getHost(), ringHost.getPort());
            HttpRequestHelper had = requestHelpers.putIfAbsent(ringHost, requestHelper);
            if (had != null) {
                requestHelper = had;
            }
        }
        return requestHelper;
    }

}
