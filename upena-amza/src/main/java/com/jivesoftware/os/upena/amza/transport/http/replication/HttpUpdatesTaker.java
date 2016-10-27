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
import com.jivesoftware.os.upena.amza.shared.RingHost;
import com.jivesoftware.os.upena.amza.shared.RowScan;
import com.jivesoftware.os.upena.amza.shared.TableName;
import com.jivesoftware.os.upena.amza.shared.UpdatesTaker;
import com.jivesoftware.os.upena.amza.storage.RowMarshaller;
import com.jivesoftware.os.upena.amza.storage.binary.BinaryRowMarshaller;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class HttpUpdatesTaker implements UpdatesTaker {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final ConcurrentHashMap<RingHost, HttpRequestHelper> requestHelpers = new ConcurrentHashMap<>();
    private final boolean sslEnable;
    private final boolean allowSelfSignedCerts;
    private final OAuthSigner signer;

    public HttpUpdatesTaker(boolean sslEnable, boolean allowSelfSignedCerts, OAuthSigner signer) {
        this.sslEnable = sslEnable;
        this.allowSelfSignedCerts = allowSelfSignedCerts;
        this.signer = signer;
    }

    @Override
    public void takeUpdates(RingHost ringHost,
        TableName tableName,
        long transationId,
        RowScan tookRowUpdates) throws Exception {

        RowUpdates changeSet = new RowUpdates(transationId, tableName, new ArrayList<>());
        RowUpdates took = getRequestHelper(ringHost).executeRequest(changeSet, "/amza/changes/take", RowUpdates.class, null);
        if (took == null) {
            return;
        }
        if (!took.getTableName().equals(tableName)) {
            LOG.error("Took from table:{} but received for table:{}", took.getTableName(), tableName);
            return;
        }
        final BinaryRowMarshaller rowMarshaller = new BinaryRowMarshaller();
        for (byte[] row : took.getChanges()) {
            RowMarshaller.WALRow walr = rowMarshaller.fromRow(row);
            tookRowUpdates.row(walr.getTransactionId(), walr.getKey(), walr.getValue());
        }
    }

    HttpRequestHelper getRequestHelper(RingHost ringHost) throws Exception {
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
