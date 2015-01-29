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
                }
                healthLog.commit();
                return true;
            }
            if (!invokeScript.invoke(deployLog, instancePath, "config")) {
                deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'config'", "nanny health command failed", null);
                return false;
            }
            if (!invokeScript.invoke(deployLog, instancePath, "start")) {
                deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'start'", "nanny failed while start.", null);
                return false;
            }
            int checks = 0;
            while (checks < 10) {
                // todo expose to config or to instance
                if (invokeScript.invoke(deployLog, instancePath, "status")) {
                    deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'status'", "ONLINE", null);
                    if (!invokeScript.invoke(healthLog, instancePath, "health")) {
                        deployLog.log("Service:" + instancePath.toHumanReadableName() + " 'health'", "nanny health command failed", null);
                    }
                    healthLog.commit();
                    break;
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
            healthLog.commit();
            return false;
        }
    }
}
