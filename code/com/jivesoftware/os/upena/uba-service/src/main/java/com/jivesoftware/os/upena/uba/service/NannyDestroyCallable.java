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

import java.io.File;
import java.util.Collection;
import java.util.concurrent.Callable;
import org.apache.commons.io.FileUtils;

class NannyDestroyCallable implements Callable<Boolean> {

    private final InstancePath instancePath;
    private final DeployLog deployLog;
    private final DeployableScriptInvoker invokeScript;

    public NannyDestroyCallable(InstancePath instancePath,
            DeployLog deployLog,
            DeployableScriptInvoker invokeScript) {
        this.instancePath = instancePath;
        this.deployLog = deployLog;
        this.invokeScript = invokeScript;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            return destroy();
        } catch (Exception x) {
            deployLog.log("nanny failed.", x);
            return false;
        }
    }

    public boolean destroy() throws Exception {
        if (invokeScript.exists(instancePath, "status")) {
            if (invokeScript.invoke(deployLog, instancePath, "status")) {
                if (invokeScript.exists(instancePath, "kill")) {
                    invokeScript.invoke(deployLog, instancePath, "kill");
                    int checks = 0;
                    while (checks < 10) {
                        // todo expose to config or to instance
                        if (!invokeScript.invoke(deployLog, instancePath, "status")) {
                            deployLog.log("OFFLINE Service:" + instancePath.toHumanReadableName(), null);
                            break;
                        } else {
                            checks++;
                            deployLog.log("Waiting for Service:"+instancePath.toHumanReadableName()+" to start for " + checks + " time.", null);
                            Thread.sleep(1000); // todo expose to config or to instance
                        }
                    }
                }
            }
        }
        File serviceRoot = instancePath.serviceRoot();
        if (serviceRoot.exists()) {
            FileUtils.deleteQuietly(serviceRoot);
            deleteFolderIfEmpty(serviceRoot.getParentFile());
        }
        return true;
    }

    private void deleteFolderIfEmpty(File folder) {
        if (folder.equals(instancePath.serviceRoot())) {
            return;
        }
        Collection<File> files = FileUtils.listFiles(folder, null, true);
        if (files.isEmpty()) {
            FileUtils.deleteQuietly(folder);
            deleteFolderIfEmpty(folder.getParentFile());
        }
    }
}
