/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jivesoftware.os.upena.reporter.service;

import com.jivesoftware.os.upena.reporter.service.StatusReportBroadcaster.StatusReportCallback;
import java.net.SocketException;

public class StatusReportBroadcasterInitializer {

    public void initialize(String instanceKey, long broadcastStatusReportEveryNMillis,
            StatusReportCallback statusReportCallback) throws SocketException {

        if (broadcastStatusReportEveryNMillis > 0) {
            StatusReportBroadcaster.initialize(
                    instanceKey,
                    broadcastStatusReportEveryNMillis,
                    statusReportCallback);
        }
    }
}