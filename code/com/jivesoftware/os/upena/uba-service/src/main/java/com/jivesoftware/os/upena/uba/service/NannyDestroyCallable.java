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
            deployLog.log("Nanny for " + instancePath.toHumanReadableName(), "failed to destroy.", x);
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
                            deployLog.log("Service:" + instancePath.toHumanReadableName(), "OFFLINE", null);
                            break;
                        } else {
                            checks++;
                            deployLog.log("Service:" + instancePath.toHumanReadableName(),
                                "Waiting for service to start for " + checks + " time.", null);
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
