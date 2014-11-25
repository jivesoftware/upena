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

import com.jivesoftware.os.jive.utils.shell.utils.Untar;
import com.jivesoftware.os.jive.utils.shell.utils.Unzip;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
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
    private final DeployableValidator deployableValidator;
    private final DeployableScriptInvoker invokeScript;

    public NannyDeployCallable(String host, String upenaHost, int upenaPort,
        InstanceDescriptor id, InstancePath instancePath,
        DeployLog deployLog, DeployableValidator deployableValidator,
        DeployableScriptInvoker invokeScript) {
        this.host = host;
        this.upenaHost = upenaHost;
        this.upenaPort = upenaPort;
        this.id = id;
        this.instancePath = instancePath;
        this.deployLog = deployLog;
        this.deployableValidator = deployableValidator;
        this.invokeScript = invokeScript;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            if (deploy()) {
                instancePath.writeInstanceDescriptor(host, upenaHost, upenaPort, id);
                if (!invokeScript.invoke(deployLog, instancePath, "init")) {
                    deployLog.log("Nanny", "failed to init service.", null);
                    return false;
                }
            } else {
                deployLog.log("Nanny", "failed to deploy artifact.", null);
                return false;
            }
            return true;
        } catch (IOException x) {
            deployLog.log("Nanny", "failed.", x);
            return false;
        }
    }

    private boolean deploy() {
        File libDir;
        try {
            libDir = instancePath.lib();
            FileUtils.deleteDirectory(libDir);
            libDir.mkdirs();
        } catch (IOException x) {
            deployLog.log("Nanny", "failed to cleanup lib dir.", x);
            return false;
        }

        RepositorySystem system = RepositoryProvider.newRepositorySystem();
        RepositorySystemSession session = RepositoryProvider.newRepositorySystemSession(system);
        String[] repos = id.repository.split(",");
        List<RemoteRepository> remoteRepos = RepositoryProvider.newRepositories(system, session, repos);

        System.out.println("------------------------------------------------------------");
        System.out.println(" Resolving:" + id);
        System.out.println("------------------------------------------------------------");
        String[] versionParts = id.versionName.split(":");
        String groupId = versionParts[0];
        String artifactId = versionParts[1];
        String version = versionParts[2];

        Artifact artifact = new DefaultArtifact(groupId, artifactId, "tar.gz", version);
        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.setRepositories(remoteRepos);

        ArtifactResult artifactResult;
        try {
            System.out.println("Resolving " + artifact);
            artifactResult = system.resolveArtifact(session, artifactRequest);
            System.out.println("artifactResult=" + artifactResult);
            artifact = artifactResult.getArtifact();
            System.out.println(artifact + " resolved to  " + artifact.getFile());

        } catch (ArtifactResolutionException x) {
            deployLog.log("Nanny", "failed to resolve artifact:", x);
            return false;
        }

        try {
            File tarGzip = instancePath.artifactFile(".tar.gz");
            System.out.println("------------------------------------------------------------");
            System.out.println(" Upacking:" + tarGzip);
            System.out.println("------------------------------------------------------------");
            FileUtils.copyFile(artifact.getFile(), tarGzip, true);
            deployLog.log("Nanny", "deployed " + tarGzip, null);
            if (!explodeArtifact(tarGzip)) {
                return false;
            }
            artifact = new DefaultArtifact(groupId + ":" + artifactId + ":" + version);
            artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(artifact);
            artifactRequest.setRepositories(remoteRepos);
            artifactResult = system.resolveArtifact(session, artifactRequest);
            artifact = artifactResult.getArtifact();

            deployLog.log("Nanny", "deployed " + artifact.getFile() + " to " + libDir, null);
            FileUtils.copyFileToDirectory(artifact.getFile(), libDir, true);
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(new Dependency(artifact, ""));
            collectRequest.setRepositories(remoteRepos);
            CollectResult collectResult = system.collectDependencies(session, collectRequest);
            DeployArtifactDependencies deployArtifactDependencies = new DeployArtifactDependencies(deployLog, system, session, remoteRepos, libDir);
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
