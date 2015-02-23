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
package com.jivesoftware.os.upena.uba.service;

import com.jivesoftware.os.jive.utils.shell.utils.Curl;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.uba.shared.NannyReport;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Nanny {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final InstancePath instancePath;
    private final DeployableValidator deployableValidator;
    private final DeployLog deployLog;
    private final HealthLog healthLog;
    private final DeployableScriptInvoker invokeScript;
    private final AtomicBoolean redeploy;
    private final AtomicBoolean destroyed;
    private final AtomicReference<InstanceDescriptor> instanceDescriptor;
    private final LinkedBlockingQueue<Runnable> linkedBlockingQueue;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final AtomicLong restartAtTimestamp = new AtomicLong(-1);

    public Nanny(InstanceDescriptor instanceDescriptor,
        InstancePath instancePath,
        DeployableValidator deployableValidator,
        DeployLog deployLog,
        HealthLog healthLog,
        DeployableScriptInvoker invokeScript) {
        this.instanceDescriptor = new AtomicReference<>(instanceDescriptor);
        this.instancePath = instancePath;
        this.deployableValidator = deployableValidator;
        this.deployLog = deployLog;
        this.healthLog = healthLog;
        this.invokeScript = invokeScript;
        linkedBlockingQueue = new LinkedBlockingQueue<>(10);
        threadPoolExecutor = new ThreadPoolExecutor(1, 1, 1000, TimeUnit.MILLISECONDS, linkedBlockingQueue);
        boolean exists = instancePath.script("status").exists();
        System.out.println("Stats script for " + instanceDescriptor + " exists ==" + exists);
        redeploy = new AtomicBoolean(!exists);
        destroyed = new AtomicBoolean(false);
    }

    public InstanceDescriptor getInstanceDescriptor() {
        return instanceDescriptor.get();
    }

    synchronized public void setInstanceDescriptor(InstanceDescriptor id) {
        InstanceDescriptor got = instanceDescriptor.get();
        if (got != null && !got.equals(id)) {
            redeploy.set(true);
            LOG.info("Instance changed from " + got + " to " + id);
        } else if (!instancePath.script("status").exists()) {
            redeploy.set(true);
            LOG.info("Missing status script from " + got + " to " + id);
        }
        if (!redeploy.get()) {
            LOG.info("Service:" + instancePath.toHumanReadableName() + " has NOT changed.");
        } else {
            instanceDescriptor.set(id);
        }
        if (id.restartTimestampGMTMillis > System.currentTimeMillis()) {
            restartAtTimestamp.set(id.restartTimestampGMTMillis);
        }
    }

    public DeployLog getDeployLog() {
        return deployLog;
    }

    public HealthLog getHealthLog() {
        return healthLog;
    }

    public NannyReport report() {
        return new NannyReport(deployLog.getState(), instanceDescriptor.get(), deployLog.commitedLog());
    }

    synchronized public String nanny(String host, String upenaHost, int upenaPort) throws InterruptedException, ExecutionException {

        if (restartAtTimestamp.get() > 0) {
            deployLog.log("Nanny", "Restart triggered by timestamp. " + this, null);
            if (kill()) {
                restartAtTimestamp.set(-1);
            }
        }

        try {
            if (destroyed.get()) {
                deployLog.log("Nanny", "tried to check a service that has been destroyed. " + this, null);
                return deployLog.getState();
            }
            if (linkedBlockingQueue.size() == 0) {
                try {
                    if (redeploy.get()) {
                        NannyDestroyCallable destroyTask = new NannyDestroyCallable(
                            instancePath,
                            deployLog,
                            healthLog,
                            invokeScript);
                        deployLog.log("Nanny", "destroying in preperation to redeploy. " + this, null);
                        Future<Boolean> detroyedFuture = threadPoolExecutor.submit(destroyTask);
                        if (detroyedFuture.get()) {

                            NannyDeployCallable deployTask = new NannyDeployCallable(host, upenaHost, upenaPort,
                                instanceDescriptor.get(),
                                instancePath,
                                deployLog,
                                healthLog,
                                deployableValidator,
                                invokeScript);
                            deployLog.log("Nanny", "redeploying. " + this, null);
                            Future<Boolean> deployedFuture = threadPoolExecutor.submit(deployTask);
                            if (deployedFuture.get()) {
                                redeploy.set(false);
                                deployLog.log("Nanny", " successfully redeployed. " + this, null);
                            }
                        }
                    }

                    NannyStatusCallable nannyTask = new NannyStatusCallable(
                        instanceDescriptor.get(),
                        instancePath,
                        deployLog,
                        healthLog,
                        invokeScript);
                    if (nannyTask.callable()) {
                        threadPoolExecutor.submit(nannyTask);
                    } else {
                        deployLog.log("Nanny", "skipped status check. " + this, null);
                    }

                } catch (InterruptedException | ExecutionException x) {
                    deployLog.log("Nanny", " is already running. " + this, x);
                }
                return deployLog.getState();
            } else {
                return deployLog.getState();
            }
        } finally {
            deployLog.commit();
        }
    }

    synchronized Boolean destroy() throws InterruptedException, ExecutionException {
        destroyed.set(true);
        NannyDestroyCallable nannyTask = new NannyDestroyCallable(
            instancePath,
            deployLog,
            healthLog,
            invokeScript);
        Future<Boolean> waitForDestory = threadPoolExecutor.submit(nannyTask);
        Boolean result = waitForDestory.get();
        nannyTask.wipeoutFiles();
        return result;

    }

    synchronized Boolean kill() throws InterruptedException, ExecutionException {
        NannyDestroyCallable nannyTask = new NannyDestroyCallable(
            instancePath,
            deployLog,
            healthLog,
            invokeScript);
        Future<Boolean> waitForDestory = threadPoolExecutor.submit(nannyTask);
        Boolean result = waitForDestory.get();
        return result;

    }

    String invalidateRouting(String tenantId) {
        try {
            LOG.info("invalidating tenant routing for tenatId:" + tenantId + " on " + this);
            StringBuilder curl = new StringBuilder();
            curl.append("localhost:");
            curl.append(instanceDescriptor.get().ports.get("manage"));
            curl.append("/tenant/routing/invalidate?");
            curl.append("tenantId=").append(tenantId).append('&');
            curl.append("connectToServiceId=*").append('&');
            curl.append("portName=*");

            String response = Curl.create().curl(curl.toString());
            LOG.info(response);
            return response;
        } catch (IOException x) {
            LOG.warn("failed to invalidate tenant routing for tenantId:" + tenantId + " on " + this);
            return "failed to invalidate tenant routing for tenantId:" + tenantId + " on " + this;
        }
    }

    public void stop() {
        threadPoolExecutor.shutdownNow();
    }

    @Override
    public String toString() {
        return "Nanny{"
            + "instancePath=" + instancePath
            + ", instanceDescriptor=" + instanceDescriptor
            + '}';
    }
}
