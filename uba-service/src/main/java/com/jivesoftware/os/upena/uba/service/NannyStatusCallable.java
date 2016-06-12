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

import com.google.common.base.Objects;
import com.google.common.cache.Cache;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class NannyStatusCallable implements Callable<Boolean> {

    private final Nanny nanny;
    private final AtomicReference<String> status;
    private final AtomicLong startupTimestamp;
    private final InstanceDescriptor id;
    private final InstancePath instancePath;
    private final DeployLog deployLog;
    private final HealthLog healthLog;
    private final DeployableScriptInvoker invokeScript;
    private final UbaLog ubaLog;
    private final Cache<InstanceDescriptor, Boolean> haveRunConfigExtractionCache;

    public NannyStatusCallable(
        Nanny nanny,
        AtomicReference<String> status,
        AtomicLong startupTimestamp,
        InstanceDescriptor id,
        InstancePath instancePath,
        DeployLog deployLog,
        HealthLog healthLog,
        DeployableScriptInvoker invokeScript,
        UbaLog ubaLog,
        Cache<InstanceDescriptor, Boolean> haveRunConfigExtractionCache) {

        this.nanny = nanny;
        this.status = status;
        this.startupTimestamp = startupTimestamp;
        this.id = id;
        this.instancePath = instancePath;
        this.deployLog = deployLog;
        this.healthLog = healthLog;
        this.invokeScript = invokeScript;
        this.ubaLog = ubaLog;
        this.haveRunConfigExtractionCache = haveRunConfigExtractionCache;
    }

    boolean callable() {
        return invokeScript.exists(instancePath, "status");
    }

    @Override
    public Boolean call() throws Exception {
        try {
            if (!id.enabled
                && Objects.firstNonNull(haveRunConfigExtractionCache.getIfPresent(id), Boolean.FALSE)) {
                status.set("");
                deployLog.log("Service:" + instancePath.toHumanReadableName(), "Skipping config extraction.", null);
                healthLog.commit();
                startupTimestamp.set(-1);
                return true;
            }

            status.set("");
            if (invokeScript.invoke(deployLog, instancePath, "status")) {
                status.set("");
                deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'status'", "ONLINE", null);
                if (!invokeScript.invoke(healthLog, instancePath, "health")) {
                    status.set("No health");
                    deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'health'", "nanny health command failed", null);
                    healthLog.forcedHealthState("Service health",
                        "Service is failing to report health", "Look at logs or health script:" + invokeScript.scriptPath(instancePath, "health"));
                    return true;
                }
                healthLog.commit();
                return true;
            }

            if (nanny.lastStartupId.get() == nanny.startupId.get()) {
                nanny.unexpectedRestartTimestamp.compareAndSet(-1, System.currentTimeMillis());
            }

            healthLog.forcedHealthState("Service Startup",
                "Service is attempting to start. Phase: configuring...", "Be patient");
            status.set("Config");
            if (!invokeScript.invoke(deployLog, instancePath, "config")) {
                status.set("Config failed!");
                deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'config'", "nanny health command failed", null);
                healthLog.forcedHealthState("Service Config",
                    "Service is failing to generate config", "Look at config script:" + invokeScript.scriptPath(instancePath, "config"));
                startupTimestamp.set(-1);
                return false;
            }

            if (!id.enabled) {
                status.set("");
                healthLog.forcedHealthState("Service Startup",
                    "Service is not enabled. Phase: stop...", "Enable when ready");
                haveRunConfigExtractionCache.put(id, Boolean.TRUE);
                return true;
            }

            healthLog.forcedHealthState("Service Startup",
                "Service is attempting to start. Phase: start...", "Be patient");
            status.set("Start");
            if (!invokeScript.invoke(deployLog, instancePath, "start")) {
                status.set("Failed to start!");
                deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'start'", "nanny failed while start.", null);
                healthLog.forcedHealthState("Service Start",
                    "Service is failing to generate config", "Look at config script:" + invokeScript.scriptPath(instancePath, "config"));
                startupTimestamp.set(-1);
                return false;
            } else {
                status.set("Auto-restarted");
                ubaLog.record("auto-restart", id.toString(), invokeScript.scriptPath(instancePath, "start"));
            }
            int checks = 1;
            while (checks < 10) { // todo expose to config or to instance
                healthLog.forcedHealthState("Service Startup", "Service is being verifyed. Phase: verify...", "Be patient");
                status.set("Verfying" + checks);
                if (invokeScript.invoke(deployLog, instancePath, "status")) {
                    deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'status'", "ONLINE", null);
                    status.set("Health" + checks);
                    if (!invokeScript.invoke(healthLog, instancePath, "health")) {
                        status.set("Health failed!" + checks);
                        deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'health'", "nanny health command failed", null);
                        healthLog.forcedHealthState("Service health",
                            "Service is failing to report health", "Look at logs or health script:" + invokeScript.scriptPath(instancePath, "health"));

                    } else {
                        healthLog.commit();
                        break;
                    }
                } else {
                    status.set("Health failed!" + checks);
                    checks++;
                    deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'status'",
                        "Waiting for Service:" + instancePath.toHumanReadableName() + " to start for " + checks + " time.", null);
                    Thread.sleep(1000); // todo expose to config or to instance
                }
            }
            status.set("");
            startupTimestamp.set(System.currentTimeMillis());
            nanny.lastStartupId.set(nanny.startupId.get());
            return true;

        } catch (InterruptedException x) {
            status.set("Interrupted");
            deployLog.log("Nanny", "status failed.", x);
            healthLog.forcedHealthState("Nanny Interrupted",
                "The nanny service was interruptd", x.toString());
            startupTimestamp.set(-1);
            return false;
        }
    }

}
