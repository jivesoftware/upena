package com.jivesoftware.os.upena.uba.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.FileUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

public class DeployArtifactDependencies implements DependencyVisitor {

    private final DeployLog deployLog;
    private final RepositorySystem system;
    private final RepositorySystemSession session;
    private final List<RemoteRepository> remoteRepos;
    private final File libDir;
    private final AtomicBoolean deployed = new AtomicBoolean(true);

    public DeployArtifactDependencies(DeployLog deployLog,
            RepositorySystem system,
            RepositorySystemSession session,
            List<RemoteRepository> remoteRepos,
            File libDir) {
        this.deployLog = deployLog;
        this.system = system;
        this.session = session;
        this.remoteRepos = remoteRepos;
        this.libDir = libDir;
    }

    public boolean successfulDeploy() {
        return deployed.get();
    }

    @Override
    public boolean visitEnter(DependencyNode node) {
        Artifact artifact = node.getArtifact();
        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.setRepositories(remoteRepos);

        ArtifactResult artifactResult = null;
        try {
            artifactResult = system.resolveArtifact(session, artifactRequest);
        } catch (ArtifactResolutionException ex) {
            deployLog.log("Deloyer", "failed to resolve " + artifact, ex);
            deployed.set(false);
        }
        if (artifactResult != null) {
            artifact = artifactResult.getArtifact();
            try {
                FileUtils.copyFileToDirectory(artifact.getFile(), libDir, true);
                deployLog.log("Deployer", "deployed " + artifact.getFile() + " to " + libDir, null);
            } catch (IOException ex) {
                deployLog.log("Deployer", "failed to deploy " + artifact.getFile() + " to " + libDir, ex);
                deployed.set(false);
            }
        }
        return true;
    }

    @Override
    public boolean visitLeave(DependencyNode node) {
        return true;
    }
}
