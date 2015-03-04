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
package com.jivesoftware.os.upena.hello.routing.bird.service;

import com.jivesoftware.os.jive.utils.http.client.HttpClientException;
import com.jivesoftware.os.jive.utils.http.client.HttpResponse;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.tenant.routing.http.client.TenantAwareHttpClient;

/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
public class HelloRoutingBirdService {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final String greeting;
    private final TenantAwareHttpClient<String> client;

    public HelloRoutingBirdService(String greeting, TenantAwareHttpClient<String> client) {
        this.greeting = greeting;
        this.client = client;
    }

    public String echo(String tenantId, String message, int echos) throws HttpClientException {
        LOG.info("echo: tenantId:" + tenantId + " message:" + message + " echos:" + echos);
        if (echos > 0) {
            HttpResponse got = client.get(tenantId, "/echo?tenantId=" + tenantId + "&message=" + message + "&echos=" + (echos - 1));
            return "{" + new String(got.getResponseBody()) + " " + message + "}";
        }
        return "{" + message + "} ";
    }

    public String greetings() {

        LOG.inc("countHello-world");

        LOG.startTimer("timerHello-world");
        try {
            Thread.sleep(20);
        } catch (InterruptedException ex) {
            //I DON'T CARE
        }
        LOG.stopTimer("timerHello-world");

        return greeting;
    }
}
