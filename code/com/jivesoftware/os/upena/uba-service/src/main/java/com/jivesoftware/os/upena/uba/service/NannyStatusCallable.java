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
    private final DeployableScriptInvoker invokeScript;

    public NannyStatusCallable(InstanceDescriptor id,
        InstancePath instancePath,
        DeployLog deployLog,
        DeployableScriptInvoker invokeScript) {

        this.id = id;
        this.instancePath = instancePath;
        this.deployLog = deployLog;
        this.invokeScript = invokeScript;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            if (invokeScript.invoke(deployLog, instancePath, "status")) {
                deployLog.log("ONLINE Service:" + instancePath.toHumanReadableName(), null);

                if (!invokeScript.invoke(deployLog, instancePath, "health")) {
                    deployLog.log("nanny failed while calling 'bin/health' Service:" + instancePath.toHumanReadableName(), null);
                }
                return true;
            }
            if (!invokeScript.invoke(deployLog, instancePath, "config")) {
                deployLog.log("nanny failed while calling 'bin/config' Service:" + instancePath.toHumanReadableName(), null);
                return false;
            }
            if (!invokeScript.invoke(deployLog, instancePath, "start")) {
                deployLog.log("nanny failed while calling 'bin/start' Service:" + instancePath.toHumanReadableName(), null);
                return false;
            }
            int checks = 0;
            while (checks < 10) {
                // todo expose to config or to instance
                if (invokeScript.invoke(deployLog, instancePath, "status")) {
                    deployLog.log("ONLINE Service:" + instancePath.toHumanReadableName(), null);
                    if (!invokeScript.invoke(deployLog, instancePath, "health")) {
                        deployLog.log("nanny failed while calling 'bin/health' Service:" + instancePath.toHumanReadableName(), null);
                    }
                    break;
                } else {
                    checks++;
                    deployLog.log("Waiting for Service:" + instancePath.toHumanReadableName() + " to start for " + checks + " time.", null);
                    Thread.sleep(1000); // todo expose to config or to instance
                }
            }
            return true;

        } catch (InterruptedException x) {
            deployLog.log("nanny failed.", x);
            return false;
        }
    }
}
