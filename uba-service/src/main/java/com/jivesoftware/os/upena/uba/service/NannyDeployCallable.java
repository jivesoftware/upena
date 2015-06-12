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

import com.google.common.base.Joiner;
import com.jivesoftware.os.jive.utils.shell.utils.Untar;
import com.jivesoftware.os.jive.utils.shell.utils.Unzip;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.FileUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

class NannyDeployCallable implements Callable<Boolean> {

    private final String host;
    private final String upenaHost;
    private final int upenaPort;
    private final InstanceDescriptor id;
    private final InstancePath instancePath;
    private final DeployLog deployLog;
    private final HealthLog healthLog;
    private final DeployableValidator deployableValidator;
    private final DeployableScriptInvoker invokeScript;
    private final UbaLog ubaLog;

    public NannyDeployCallable(String host, String upenaHost, int upenaPort,
        InstanceDescriptor id, InstancePath instancePath,
        DeployLog deployLog,
        HealthLog healthLog,
        DeployableValidator deployableValidator,
        DeployableScriptInvoker invokeScript,
        UbaLog ubaLog) {
        this.host = host;
        this.upenaHost = upenaHost;
        this.upenaPort = upenaPort;
        this.id = id;
        this.instancePath = instancePath;
        this.deployLog = deployLog;
        this.healthLog = healthLog;
        this.deployableValidator = deployableValidator;
        this.invokeScript = invokeScript;
        this.ubaLog = ubaLog;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            instancePath.writeInstanceDescriptor(host, upenaHost, upenaPort, id);
            if (deploy()) {
                if (!invokeScript.invoke(deployLog, instancePath, "init")) {
                    deployLog.log("Nanny", "failed to init service.", null);
                    healthLog.forcedHealthState("Service startup", "Failed to while calling init:" + Joiner.on("\n").join(deployLog.peek()), "Check logs.");
                    return false;
                }
            } else {
                deployLog.log("Nanny", "failed to deploy artifact.", null);
                healthLog.forcedHealthState("Service startup", "Failed to deploy:" + Joiner.on("\n").join(deployLog.peek()), "Check logs.");
                return false;
            }
            healthLog.forcedHealthState("Service deployed", "Service will be consider unhealthy until first health check is successful.", "");
            ubaLog.record("deployed", id.toString(), instancePath.lib().getParent());
            return true;
        } catch (IOException x) {
            deployLog.log("Nanny", "failed.", x);
            healthLog.forcedHealthState("Service startup", "Nanny failed." + Joiner.on("\n").join(deployLog.peek()), "Check logs.");
            return false;
        }
    }

    private boolean deploy() {
        File libDir = null;
        try {
            libDir = instancePath.lib();
            System.out.println("Clearing:" + libDir);
            FileUtils.deleteDirectory(libDir);
            System.out.println("Creating if absent:" + libDir);
            libDir.mkdirs();
        } catch (IOException x) {
            deployLog.log("Nanny", "failed to cleanup '" + libDir + "'.", x);
            return false;
        }

        File pluginlibDir = null;
        try {
            pluginlibDir = instancePath.pluginLib();
            System.out.println("Clearing:" + pluginlibDir);
            FileUtils.deleteDirectory(pluginlibDir);
            System.out.println("Creating if absent:" + pluginlibDir);
            pluginlibDir.mkdirs();
        } catch (IOException x) {
            deployLog.log("Nanny", "failed to cleanup '" + pluginlibDir + "'.", x);
            return false;
        }

        RepositorySystem system = RepositoryProvider.newRepositorySystem();
        RepositorySystemSession session = RepositoryProvider.newRepositorySystemSession(system);
        String[] repos = id.repository.split(",");
        List<RemoteRepository> remoteRepos = RepositoryProvider.newRepositories(system, session, repos);

        System.out.println("------------------------------------------------------------");
        System.out.println(" Resolving:" + id);
        System.out.println("------------------------------------------------------------");
        String[] deployablecoordinates = id.versionName.trim().split(",");
        boolean successfulDeploy = deploy(deployablecoordinates[0], remoteRepos, system, session, libDir);
        if (successfulDeploy) {
            if (deployablecoordinates.length > 1) {
                System.out.println("------------------------------------------------------------");
                System.out.println(" Deploying plugins:" + (deployablecoordinates.length - 1));
                System.out.println("------------------------------------------------------------");

                for (int i = 1; i < deployablecoordinates.length; i++) {
                    System.out.println("------------------------------------------------------------");
                    System.out.println(" Deploying plugin:" + deployablecoordinates[i]);
                    System.out.println("------------------------------------------------------------");
                    successfulDeploy |= deploy(deployablecoordinates[i], remoteRepos, system, session, pluginlibDir);
                }

                System.out.println("------------------------------------------------------------");
                System.out.println(" Deployed all plugins? " + successfulDeploy);
                System.out.println("------------------------------------------------------------");
            }
        }
        return successfulDeploy;
    }

    private boolean deploy(String deployablecoordinate, List<RemoteRepository> remoteRepos, RepositorySystem system, RepositorySystemSession session,
        File dir) {
        String[] versionParts = deployablecoordinate.trim().split(":");
        if (versionParts.length != 4) {
            System.out.println("deployable coordinates must be of the following form: groupId:artifactId:packaging:version");
            return false;
        }
        String groupId = versionParts[0];
        String artifactId = versionParts[1];
        String packaging = versionParts[2];
        String version = versionParts[3];

        Artifact artifact = new DefaultArtifact(groupId, artifactId, packaging, version);
        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.setRepositories(remoteRepos);

        ArtifactResult artifactResult;
        try {
            System.out.println("------------------------------------------------------------");
            System.out.println(" Resolving: " + deployablecoordinate);
            System.out.println("------------------------------------------------------------");
            artifactResult = system.resolveArtifact(session, artifactRequest);
            artifact = artifactResult.getArtifact();
            System.out.println(artifact + " resolved to  " + artifact.getFile());

        } catch (ArtifactResolutionException x) {
            deployLog.log("Nanny", "failed to resolve artifact:", x);
            return false;
        }

        try {

            if (packaging.equals("tar.gz") || packaging.equals("tgz")) {
                File tarGzip = instancePath.artifactFile("." + packaging);
                System.out.println("------------------------------------------------------------");
                System.out.println(" Upacking:" + tarGzip);
                System.out.println("------------------------------------------------------------");
                FileUtils.copyFile(artifact.getFile(), tarGzip, true);
                deployLog.log("Nanny", "deployed " + tarGzip, null);
                if (!explodeArtifact(tarGzip)) {
                    return false;
                }
            }
            artifact = new DefaultArtifact(groupId + ":" + artifactId + ":" + version);
            artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(artifact);
            artifactRequest.setRepositories(remoteRepos);
            artifactResult = system.resolveArtifact(session, artifactRequest);
            artifact = artifactResult.getArtifact();

            deployLog.log("Nanny", "deployed " + artifact.getFile() + " to " + dir, null);
            FileUtils.copyFileToDirectory(artifact.getFile(), dir, true);
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(new Dependency(artifact, ""));
            collectRequest.setRepositories(remoteRepos);
            CollectResult collectResult = system.collectDependencies(session, collectRequest);
            DeployArtifactDependencies deployArtifactDependencies = new DeployArtifactDependencies(deployLog, system, session, remoteRepos, dir);
            collectResult.getRoot().accept(deployArtifactDependencies);
            boolean successfulDeploy = deployArtifactDependencies.successfulDeploy();
            deployLog.log("Nanny", "SUCCESS " + successfulDeploy, null);
            return successfulDeploy;
        } catch (IOException | ArtifactResolutionException | DependencyCollectionException x) {
            deployLog.log("Nanny", "failed to deploy artifact:", x);
            return false;
        }
    }

    private boolean explodeArtifact(File tarGzip) {
        try {
            File serviceRoot = instancePath.serviceRoot();
            if (tarGzip.exists()) {
                Unzip.unGzip(true, serviceRoot, "artifact.tar", tarGzip, true);
            } else {
                deployLog.log("Nanny", "there is NO " + tarGzip + " so there is nothing we can do.", null);
                return false;
            }
            File tar = new File(serviceRoot, "artifact.tar");
            if (tar.exists()) {
                Untar.unTar(true, serviceRoot, tar, true);
                if (deployableValidator.validateDeployable(instancePath)) {
                    return true;
                } else {
                    deployLog.log("Nanny", "deployable is invalid.", null);
                    return false;
                }
            } else {
                deployLog.log("Nanny", "there is NO " + tar + " so there is nothing we can do.", null);
                return false;
            }
        } catch (IOException | ArchiveException x) {
            deployLog.log("Nanny", "encountered the following issues trying to explode artifact " + this, x);
            return false;
        }
    }
}
