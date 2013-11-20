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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.jive.utils.logger.MetricLogger;
import com.jivesoftware.os.jive.utils.logger.MetricLoggerFactory;
import com.jivesoftware.os.jive.utils.shell.utils.Curl;
import com.jivesoftware.os.jive.utils.shell.utils.Untar;
import com.jivesoftware.os.jive.utils.shell.utils.Unzip;
import com.jivesoftware.os.uba.shared.NannyReport;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
import com.jivesoftware.os.upena.uba.service.ShellOut.ShellOutput;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.io.FileUtils;

public class Nanny {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final ObjectMapper mapper;
    private final InstancePath instancePath;
    private final AtomicReference<InstanceDescriptor> instanceDescriptor;
    private final CircularFifoBuffer messages = new CircularFifoBuffer(1000);
    private final AtomicReference<String> state = new AtomicReference<>("idle");
    private final ExecutorService execThreads;
    private final LinkedBlockingQueue<Runnable> linkedBlockingQueue;
    private final ThreadPoolExecutor threadPoolExecutor;

    public Nanny(ObjectMapper mapper, InstanceDescriptor instanceDescriptor, InstancePath instancePath) {
        this.mapper = mapper;
        this.instanceDescriptor = new AtomicReference<>(instanceDescriptor);
        this.instancePath = instancePath;
        execThreads = Executors.newCachedThreadPool();
        linkedBlockingQueue = new LinkedBlockingQueue<>(1);
        threadPoolExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, linkedBlockingQueue);

    }

    public InstanceDescriptor getInstanceDescriptor() {
        return instanceDescriptor.get();
    }

    private void initialize(String host, String upenaHost, int upenaPort) {
        InstanceDescriptor id = instanceDescriptor.get();
        File instanceDescriptorFile = new File(instancePath.serviceRoot(), "instanceDescriptor.json");
        try {
            mapper.writeValue(instanceDescriptorFile, id);
            message("wrote overlay instanceDescriptor.json.", null);
        } catch (Exception x) {
            message("error while writing overlay instanceDescriptor.json.", x);
            x.printStackTrace(); // Hmmm
        }

        File overlayProperties = new File(instancePath.serviceRoot(), "overlay-config.properties");

        List<String> overlay = new ArrayList<>();
        overlay.add("RoutableDeployableConfig/default/instanceGUID=" + id.instanceKey);
        overlay.add("RoutableDeployableConfig/default/cluster=" + id.clusterName);
        overlay.add("RoutableDeployableConfig/default/host=" + host);
        overlay.add("RoutableDeployableConfig/default/serviceName=" + id.instanceKey);
        overlay.add("RoutableDeployableConfig/default/version=" + id.versionName);
        overlay.add("RoutableDeployableConfig/default/instanceId=" + id.instanceName);
        overlay.add("RoutableDeployableConfig/default/managePort=" + id.ports.get("manage").port);
        overlay.add("RoutableDeployableConfig/default/port=" + id.ports.get("main").port);
        overlay.add("RoutableDeployableConfig/default/routesHost=" + upenaHost);
        overlay.add("RoutableDeployableConfig/default/routesPort=" + upenaPort);

        try {
            FileUtils.writeLines(overlayProperties, "UTF-8", overlay, "\n", false);
            message("wrote overlay properties.", null);
        } catch (Exception x) {
            message("error while writing overlay properties.", x);
            x.printStackTrace(); // Hmmm
        }

        File instanceEnvFile = new File(instancePath.serviceRoot(), "instance-env.sh");

        List<String> instanceEnv = new ArrayList<>();
        instanceEnv.add("#!/bin/bash");
        instanceEnv.add("JAVA_XMS=64");
        instanceEnv.add("JAVA_XMX=512");
        String name = instanceDescriptor.get().serviceName.replace('-', '.');
        instanceEnv.add("JAVA_MAIN=com.jivesoftware.jive.hello.routing.bird." + name + ".Main"); // hack
        instanceEnv.add("UPENA_HOST=" + upenaHost);
        instanceEnv.add("UPENA_PORT=" + upenaPort);
        instanceEnv.add("CLUSTER_KEY=" + id.clusterKey);
        instanceEnv.add("SERVICE_KEY=" + id.serviceKey);
        instanceEnv.add("RELEASE_GROUP_KEY=" + id.releaseGroupKey);
        instanceEnv.add("INSTANCE_KEY=" + id.instanceKey);

        for (Map.Entry<String, InstanceDescriptor.InstanceDescriptorPort> p : id.ports.entrySet()) {
            instanceEnv.add(p.getKey().toUpperCase() + "_PORT" + "=" + p.getValue().port);
        }

        try {
            FileUtils.writeLines(instanceEnvFile, "UTF-8", instanceEnv, "\n", false);
            message("wrote instance-env.sh.", null);
        } catch (Exception x) {
            x.printStackTrace(); // Hmmm
            message("error while writing overlay instance-env.sh.", x);
        }
    }

    public NannyReport report() {
        List<String> report = new ArrayList<>();
        for (Iterator it = messages.iterator(); it.hasNext();) {
            Object object = it.next();
            if (object != null) {
                report.add(object.toString());
            }
        }
        return new NannyReport(state.get(), instanceDescriptor.get(), report);
    }

    synchronized public String nanny(String host, String upenaHost, int upenaPort) {
        if (linkedBlockingQueue.size() == 0) {
            try {
                NannyTask nannyTask = new NannyTask(host, upenaHost, upenaPort);
                execThreads.submit(nannyTask);
            } catch (RejectedExecutionException x) {
                message("Nanny is already running. " + this, null);
                x.printStackTrace();
            }
            return state.get();
        } else {
            return state.get();
        }
    }

    public void setInstanceDescriptor(InstanceDescriptor id) {
        instanceDescriptor.set(id);
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
        } catch (Exception x) {
            LOG.warn("failed to invalidate tenant routing for tenantId:" + tenantId + " on " + this);
            return "failed to invalidate tenant routing for tenantId:" + tenantId + " on " + this;
        }
    }

    private class NannyTask implements Runnable {

        private final String host;
        private final String upenaHost;
        private final int upenaPort;

        public NannyTask(String host, String upenaHost, int upenaPort) {
            this.host = host;
            this.upenaHost = upenaHost;
            this.upenaPort = upenaPort;
        }

        @Override
        public void run() {
            System.out.println("Starting Nanny Thread.");
            try {
                messages.clear();
                message("nanny starting.", null);
                if (instancePath.script("status").exists()) {
                    if (instancePath.artifactFile(".tar.gz").exists()) {
                        if (destroy()) {
                            deploy();
                        } else {
                            message("nanny failed to destroy service.", null);
                        }
                    } else if (!invoke("status")) {
                        if (invoke("start")) {
                            nannySucces();
                        } else {
                            message("nanny cannot tell if service is ok.", null);
                        }
                    } else {
                        nannySucces();
                    }
                } else {
                    if (instancePath.artifactFile(".tar.gz").exists()) {
                        deploy();
                    } else {
                        if (invoke("download")) {
                            deploy();
                        } else {
                            message("nanny failed to download artifact.", null);
                        }
                    }
                }

            } catch (Exception x) {
                message("nanny failed.", x);
            }
            System.out.println("Exited Nanny Thread.");
        }

        void deploy() {
            if (explodeArtifact()) {
                initialize(host, upenaHost, upenaPort);
                if (invoke("init")) {
                    if (invoke("start")) {
                        nannySucces();
                    } else {
                        message("nanny failed to start service.", null);
                    }
                } else {
                    message("nanny failed to init service.", null);
                }
            } else {
                message("nanny failed to deploy artifact.", null);
            }
        }
    }

    boolean startService() {
        invoke("config"); //?? is this needed or should it just be part of start?
        if (invoke("start")) {
            nannySucces();
            return true;
        } else {
            message("nanny failed to start service.", null);
            return false;
        }
    }

    private void nannySucces() {
        message("running. " + instancePath.toHumanReadableName() + " port:" + instanceDescriptor.get().ports, null);
        message("nanny was SUCCESSFUL!", null);
    }

    public boolean destroy() throws Exception {
        Boolean result = invoke("kill");
        File serviceRoot = instancePath.serviceRoot();
        if (serviceRoot.exists()) {
            FileUtils.deleteQuietly(serviceRoot);
            deleteFolderIfEmpty(serviceRoot.getParentFile());
        }
        return result;
    }

    public void stop() {
        execThreads.shutdownNow();
        threadPoolExecutor.shutdownNow();
    }

    synchronized public void message(String message, Throwable t) {
        if (t != null) {
            LOG.warn(message, t);
            messages.add(message);
            Writer result = new StringWriter();
            PrintWriter printWriter = new PrintWriter(result);
            t.printStackTrace(printWriter);
            messages.add(result.toString());
            state.set(message + " " + result.toString());
        } else {
            LOG.info(message);
            messages.add(message);
            state.set(message);
        }
    }

    @Override
    public String toString() {
        return "Player{"
                + "instancePath=" + instancePath
                + ", instanceDescriptor=" + instanceDescriptor
                + '}';
    }

    private boolean explodeArtifact() {
        try {
            File serviceRoot = instancePath.serviceRoot();
            File gzip = instancePath.artifactFile(".tar.gz");
            if (gzip.exists()) {
                Unzip.unGzip(true, serviceRoot, "artifact.tar", gzip, true);
            } else {
                message("There is NO " + gzip + " so there is nothing we can do.", null);
                return false;
            }
            File tar = new File(serviceRoot, "artifact.tar");
            if (tar.exists()) {
                Untar.unTar(true, serviceRoot, tar, true);
                if (validateDeployable()) {
                    return true;
                } else {
                    message("Deployable is invalid.", null);
                    return false;
                }
            } else {
                message("There is NO " + tar + " so there is nothing we can do.", null);
                return false;
            }
        } catch (Exception x) {
            message("Encountered the following issues trying to explode artifact " + this, x);
            return false;
        }
    }

    private boolean validateDeployable() {
        File serviceRoot = instancePath.serviceRoot();
        String[] expectedScriptNames = new String[]{"init", "kill", "start", "status"};
        for (String scriptName : expectedScriptNames) {
            File scripFile = new File(serviceRoot, "bin/" + scriptName);
            if (!scripFile.exists()) {
                message("Invalid deployable: bin/" + scriptName + " doesn't exists.", null);
                return false;
            }
            if (!scripFile.isFile()) {
                message("Invalid deployable: bin/" + scriptName + " expected to be a file.", null);
                return false;
            }
            scripFile.setExecutable(true);
        }
        return true;
    }

    private Boolean invoke(final String script) {
        try {
            message("invoking " + script, null);
            File scriptFile = instancePath.script(script);
            if (scriptFile.exists()) {
                String[] command = new String[]{scriptFile.getAbsolutePath()};
                ShellOutput shellOutput = new ShellOutput() {
                    @Override
                    public void line(String line) {
                        message(script + ":" + line, null);
                    }

                    @Override
                    public void close() {
                    }
                };
                final ShellOut shellOut = new ShellOut(instancePath.serviceRoot(), Arrays.asList(command), shellOutput, shellOutput);
                Future<Integer> future = execThreads.submit(new Callable<Integer>() {

                    @Override
                    public Integer call() throws Exception {
                        return shellOut.exec();
                    }
                });
                return future.get() == 0;
            } else {
                message("Failed to invoke script: " + scriptFile + " because it doesn't exisit.", null);
                return false;
            }
        } catch (Exception x) {
            message("Encountered when running script: " + script, x);
            return false;
        }
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

    public void writeArtifact(String version, byte[] deployableFileBytes, String extension) throws IOException {
        File artifactFile = instancePath.artifactFile(extension);
        if (artifactFile.exists()) {
            FileUtils.deleteQuietly(artifactFile);
        }
        FileUtils.writeByteArrayToFile(artifactFile, deployableFileBytes);
        message("Wrote " + artifactFile.getAbsolutePath() + " Size:" + deployableFileBytes.length, null);
    }
}