package com.jivesoftware.os.upena.hello.routing.bird.service;

import com.jivesoftware.os.jive.utils.http.client.HttpClientException;
import com.jivesoftware.os.jive.utils.http.client.HttpResponse;
import com.jivesoftware.os.jive.utils.logger.MetricLogger;
import com.jivesoftware.os.jive.utils.logger.MetricLoggerFactory;
import com.jivesoftware.os.upena.tenant.routing.http.client.TenantRoutingHttpClient;

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
    private final TenantRoutingHttpClient<String> client;

    public HelloRoutingBirdService(String greeting, TenantRoutingHttpClient<String> client) {
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
