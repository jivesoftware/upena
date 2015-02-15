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

import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
import java.util.concurrent.Callable;

class NannyStatusCallable implements Callable<Boolean> {

    private final InstanceDescriptor id;
    private final InstancePath instancePath;
    private final DeployLog deployLog;
    private final HealthLog healthLog;
    private final DeployableScriptInvoker invokeScript;

    public NannyStatusCallable(InstanceDescriptor id,
        InstancePath instancePath,
        DeployLog deployLog,
        HealthLog healthLog,
        DeployableScriptInvoker invokeScript) {

        this.id = id;
        this.instancePath = instancePath;
        this.deployLog = deployLog;
        this.healthLog = healthLog;
        this.invokeScript = invokeScript;
    }

    boolean callable() {
        return invokeScript.exists(instancePath, "status");
    }

    @Override
    public Boolean call() throws Exception {
        try {
            if (invokeScript.invoke(deployLog, instancePath, "status")) {
                deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'status'", "ONLINE", null);
                if (!invokeScript.invoke(healthLog, instancePath, "health")) {
                    deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'health'", "nanny health command failed", null);
                    healthLog.forecedHealthState("Service health",
                        "Service is failing to report health", "Look at logs or health script:" + invokeScript.scriptPath(instancePath, "health"));
                    return true;
                }
                healthLog.commit();
                return true;
            }
            healthLog.forecedHealthState("Service Startup",
                "Service is attempting to start. Phase: configuring...", "Be patient");
            if (!invokeScript.invoke(deployLog, instancePath, "config")) {
                deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'config'", "nanny health command failed", null);
                healthLog.forecedHealthState("Service Config",
                    "Service is failing to generate config", "Look at config script:" + invokeScript.scriptPath(instancePath, "config"));
                return false;
            }
            healthLog.forecedHealthState("Service Startup",
                "Service is attempting to start. Phase: start...", "Be patient");
            if (!invokeScript.invoke(deployLog, instancePath, "start")) {
                deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'start'", "nanny failed while start.", null);
                healthLog.forecedHealthState("Service Start",
                    "Service is failing to generate config", "Look at config script:" + invokeScript.scriptPath(instancePath, "config"));
                return false;
            }
            int checks = 1;
            while (checks < 10) {
                // todo expose to config or to instance
                healthLog.forecedHealthState("Service Startup",
                    "Service is being verifyed as onine to start. Phase: verify...", "Be patient");
                if (invokeScript.invoke(deployLog, instancePath, "status")) {
                    deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'status'", "ONLINE", null);
                    if (!invokeScript.invoke(healthLog, instancePath, "health")) {
                        deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'health'", "nanny health command failed", null);
                        healthLog.forecedHealthState("Service health",
                            "Service is failing to report health", "Look at logs or health script:" + invokeScript.scriptPath(instancePath, "health"));

                    } else {
                        healthLog.commit();
                        break;
                    }
                } else {
                    checks++;
                    deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'status'",
                        "Waiting for Service:" + instancePath.toHumanReadableName() + " to start for " + checks + " time.", null);
                    Thread.sleep(1000); // todo expose to config or to instance
                }
            }
            return true;

        } catch (InterruptedException x) {
            deployLog.log("Nanny", "status failed.", x);
            healthLog.forecedHealthState("Nanny Interrupted",
                "The nanny service was interruptd", x.toString());
            return false;
        }
    }

}
