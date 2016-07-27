package com.jivesoftware.os.upena.uba.service;

import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import java.io.File;
import java.util.Collection;
import java.util.concurrent.Callable;
import org.apache.commons.io.FileUtils;

class NannyDestroyCallable implements Callable<Boolean> {

    private final InstanceDescriptor id;
    private final InstancePath instancePath;
    private final DeployLog deployLog;
    private final HealthLog healthLog;
    private final DeployableScriptInvoker invokeScript;
    private final UbaLog ubaLog;

    public NannyDestroyCallable(InstanceDescriptor id,
        InstancePath instancePath,
        DeployLog deployLog,
        HealthLog healthLog,
        DeployableScriptInvoker invokeScript,
        UbaLog ubaLog) {
        this.id = id;
        this.instancePath = instancePath;
        this.deployLog = deployLog;
        this.healthLog = healthLog;
        this.invokeScript = invokeScript;
        this.ubaLog = ubaLog;
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
                                "Waiting for service to die for " + checks + " time.", null);
                            healthLog.forcedHealthState("Service health", "Service is refusing to be killed for the " + checks + " time.", "");
                            Thread.sleep(1000); // todo expose to config or to instance
                        }
                    }
                    if (checks > 10) {
                        ubaLog.record("failed to killed status never OFFLINE", id.toString(), invokeScript.scriptPath(instancePath, "status"));
                        return false;
                    }
                } else {
                    ubaLog.record("failed to killed missing script", id.toString(), invokeScript.scriptPath(instancePath, "kill"));
                    return false;
                }
            }
        }
        ubaLog.record("killed", id.toString(), invokeScript.scriptPath(instancePath, "kill"));
        return true;
    }

    public void wipeoutFiles() {
        File serviceRoot = instancePath.serviceRoot();
        if (serviceRoot.exists()) {
            FileUtils.deleteQuietly(serviceRoot);
            deleteFolderIfEmpty(serviceRoot.getParentFile());
        }
        healthLog.commit();
        healthLog.commit(); // Clear out all health
        ubaLog.record("wiped", id.toString(), serviceRoot.toString());

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
