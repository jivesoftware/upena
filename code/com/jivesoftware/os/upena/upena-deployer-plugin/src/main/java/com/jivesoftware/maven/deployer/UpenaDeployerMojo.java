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
package com.jivesoftware.maven.deployer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.amza.shared.TimestampedValue;
import com.jivesoftware.os.jive.utils.http.client.HttpClient;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfig;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfiguration;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactory;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.jive.utils.http.client.rest.RequestHelper;
import com.jivesoftware.os.uba.shared.DeployableUpload;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterFilter;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostFilter;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceFilter;
import com.jivesoftware.os.upena.shared.ServiceKey;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 *
 *
 * @goal deployer
 *
 * @phase install
 */
public class UpenaDeployerMojo extends AbstractMojo {

    /**
     *
     * @parameter expression="${upena.host}" default-value="localhost"
     */
    private String composerHost;
    /**
     *
     * @parameter expression="${upena.port}" default-value="1175"
     */
    private String composerPort;
    /**
     *
     * @parameter expression="${upena.release}" default-value=""
     */
    private String release;
    /**
     *
     * @parameter expression="${upena.instance.number}" default-value="1"
     */
    private String instanceNumber;
    /**
     * @parameter default-value="${project}"
     */
    private MavenProject mavenProject;

    @Override
    public void execute()
            throws MojoExecutionException, MojoFailureException {

        Repository repository;
        try {

            File home = new File(System.getProperty("user.dir"));
            File parent = home;
            File repo = new File(parent, ".git");
            String startsWith = "";
            while (!repo.exists()) {
                startsWith = parent.getName() + "/" + startsWith;
                parent = parent.getParentFile();
                if (parent == null) {
                    return;
                }
                repo = new File(parent, ".git");
            }

            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            repository = builder.setGitDir(repo).readEnvironment().findGitDir().build();

        } catch (Exception ex) {
            getLog().error("Failed to run git release notes.", ex);
            throw new MojoFailureException("Failed to run git log.", ex);
        }

        File artifact;
        try {
            File basedir = mavenProject.getBasedir();
            File local = new File(basedir, "target" + File.separator + mavenProject.getArtifactId() + "-" + mavenProject.getVersion() + ".tar.gz");

            String localFile = local.getAbsolutePath();
            artifact = new File(localFile);
            getLog().info("Local file:" + artifact.getAbsolutePath());

        } catch (Exception x) {
            getLog().error(x);
            throw new MojoFailureException("failed to deploy!", x);
        }

        String serviceName = mavenProject.getArtifactId();
        StoredConfig config = repository.getConfig();
        String name = config.getString("user", null, "name");
        String email = config.getString("user", null, "email");

        String sha = "";
        try {
            final String currentBranchName = repository.getFullBranch();
            final ObjectId currentBranch = repository.resolve(currentBranchName);
            //getLog().info("git-branch:" + repository.getBranch());
            //getLog().info("git-origin:" + repository.getFullBranch());
            //getLog().info("git-sha:" + currentBranch.name());
            sha = currentBranch.name();
        } catch (IOException | RevisionSyntaxException x) {
            throw new MojoFailureException("failed to establish repo info.", x);
        }
        String version = sha + "-" + System.currentTimeMillis();

        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider().createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(composerHost, Integer.parseInt(composerPort));
        RequestHelper requestHelper = new RequestHelper(httpClient, new ObjectMapper());

        ReleaseGroupKey releaseGroupKey = null;
        ReleaseGroup releaseGroup = null;
        try {
            String filterName = release;
            String filterEmail = release;
            if (filterName == null || filterName.length() == 0) {
                filterName = name;
                filterEmail = email;
            }

            ReleaseGroupFilter filter = new ReleaseGroupFilter(filterName,
                    filterEmail,
                    null,
                    "",
                    0,
                    Integer.MAX_VALUE);
            ReleaseGroupFilter.Results results = requestHelper.executeRequest(filter, "/upena/releaseGroup/find", ReleaseGroupFilter.Results.class, null);
            if (results != null && !results.isEmpty()) {
                Map.Entry<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> firstEntry = results.firstEntry();
                releaseGroupKey = firstEntry.getKey();
                releaseGroup = firstEntry.getValue().getValue();
            } else {
                getLog().info("------------------------------------------------------------------------");
                getLog().info("No releaseGroup with name:" + name + ". Automatically adding new releaseGroup.");
                releaseGroupKey = requestHelper.executeRequest(new ReleaseGroup(name,
                        email,
                        version,
                        ""), "/upena/releaseGroup/add", ReleaseGroupKey.class, null);
            }
        } catch (Exception x) {
            throw new MojoFailureException("failed to establish releaseGroup:" + name + " <" + email + ">", x);
        }

        ServiceKey serviceKey = null;
        try {
            ServiceFilter filter = new ServiceFilter(serviceName, null, 0, Integer.MAX_VALUE);
            ServiceFilter.Results results = requestHelper.executeRequest(filter, "/upena/service/find", ServiceFilter.Results.class, null);
            if (results != null && !results.isEmpty()) {
                Map.Entry<ServiceKey, TimestampedValue<Service>> firstEntry = results.firstEntry();
                serviceKey = firstEntry.getKey();

            } else {
                getLog().info("------------------------------------------------------------------------");
                getLog().info("No service named:" + serviceName + ". Automatically adding new service.");
                serviceKey = requestHelper.executeRequest(new Service(serviceName, ""),
                        "/upena/service/add", ServiceKey.class, null);
            }
        } catch (Exception x) {
            throw new MojoFailureException("failed to establish service:" + serviceName, x);
        }

        String clusterName = "develop";
        ClusterKey clusterKey = null;
        try {
            ClusterFilter filter = new ClusterFilter(clusterName, null, 0, Integer.MAX_VALUE);
            ClusterFilter.Results results = requestHelper.executeRequest(filter, "/upena/cluster/find", ClusterFilter.Results.class, null);
            if (results != null && !results.isEmpty()) {
                Map.Entry<ClusterKey, TimestampedValue<Cluster>> firstEntry = results.firstEntry();
                clusterKey = firstEntry.getKey();
            } else {
                getLog().info("------------------------------------------------------------------------");
                getLog().info("No cluster named:" + clusterName + ". Automatically adding new cluster.");
                Map<ServiceKey, ReleaseGroupKey> defaultReleaseGroups = new HashMap<>();
                defaultReleaseGroups.put(serviceKey, releaseGroupKey);
                clusterKey = requestHelper.executeRequest(new Cluster(clusterName, "", defaultReleaseGroups),
                        "/upena/cluster/add", ClusterKey.class, null);
            }
        } catch (Exception x) {
            throw new MojoFailureException("failed to establish cluster:" + clusterName, x);
        }

        DeployPlan deployPlanner = new DeployPlan(artifact, version, releaseGroup);
        InstanceResults results = null;
        try {
            getLog().info("------------------------------------------------------------------------");
            getLog().info("Looking for declared instance for service:" + serviceName + ".");
            InstanceFilter filter = new InstanceFilter(clusterKey, null, serviceKey, releaseGroupKey, null, 0, Integer.MAX_VALUE);
            results = requestHelper.executeRequest(filter, "/upena/instance/find", InstanceResults.class, null);
            if (results != null && !results.isEmpty()) {
                getLog().info("------------------------------------------------------------------------");
                getLog().info("Found (" + results.size() + ") instance of service:" + serviceName + ".");
                for (Entry<String, TimestampedValue<Instance>> e : results.entrySet()) {
                    TimestampedValue<Instance> instance = e.getValue();
                    if (instance.getTombstoned()) {
                        continue;
                    }
                    if (instance.getValue().locked) {
                        getLog().info("Skipping instance:" + serviceName + "." + instance.getValue().instanceId + " because it has been LOCKED.");
                    } else {
                        Host instanceHost = requestHelper.executeRequest(instance.getValue().hostKey, "/upena/host/get", Host.class, null);
                        deployPlanner.add(instance.getValue().hostKey.getKey(), instanceHost, e.getKey(), instance.getValue());
                    }
                }

            } else {
                getLog().info("------------------------------------------------------------------------");
                getLog().info("No declared instance for service:" + serviceName + ". Automatically adding new instance.");
                try {
                    HostFilter hostFilter = new HostFilter(null, null, null, null, clusterKey, 0, Integer.MAX_VALUE);
                    HostResults hostResults = requestHelper.executeRequest(hostFilter, "/upena/host/find", HostResults.class, null);
                    if (hostResults != null && !hostResults.isEmpty()) {
                        Map.Entry<String, TimestampedValue<Host>> firstEntry = hostResults.firstEntry();
                        HostKey hostKey = new HostKey(firstEntry.getKey());

                        InstanceKey instanceKey = requestHelper.executeRequest(
                                new Instance(clusterKey, hostKey, serviceKey, releaseGroupKey, Integer.parseInt(instanceNumber), true, false),
                                "/upena/instance/add", InstanceKey.class, null);

                        Instance instance = requestHelper.executeRequest(instanceKey, "/upena/instance/get", Instance.class, null);
                        if (instance != null) {
                            deployPlanner.add(firstEntry.getKey(), firstEntry.getValue().getValue(), instanceKey.getKey(), instance);
                        } else {
                            throw new MojoFailureException("Failed to create new instance for service:" + serviceName);
                        }

                    } else {
                        getLog().info("------------------------------------------------------------------------");
                        getLog().info("There are no hosts assosicated with cluster:" + clusterKey + ".");
                        throw new MojoFailureException("No host available to deploy to.");
                    }
                } catch (NumberFormatException | MojoFailureException x) {
                    throw new MojoFailureException("failed to establish service:" + serviceName, x);
                }

            }
        } catch (MojoFailureException x) {
            throw new MojoFailureException("failed to establish service:" + serviceName, x);
        }

        //getLog().info("------------------------------------------------------------------------");
        //getLog().info("Beginning the upload. This may take a bit please be patient.");
        //long time = System.currentTimeMillis();
        //deployPlanner.upload();
        //getLog().info("------------------------------------------------------------------------");
        //getLog().info("Upload SUCCESSFUL. Elapse:" + (System.currentTimeMillis() - time));

        getLog().info("------------------------------------------------------------------------");
        getLog().info("Beginning the deploy.");
        long time = System.currentTimeMillis();
        deployPlanner.deploy(serviceName, requestHelper, version);

        getLog().info("------------------------------------------------------------------------");
        getLog().info("Deploy was SUCCESSFUL. Elapse:" + (System.currentTimeMillis() - time));

    }

    public static class InstanceResults extends java.util.concurrent.ConcurrentSkipListMap<String, TimestampedValue<Instance>> {
    }

    public static class HostResults extends java.util.concurrent.ConcurrentSkipListMap<String, TimestampedValue<Host>> {
    }

    RequestHelper buildRequestHelper(String host, int port) {
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider().createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(host, port);
        RequestHelper requestHelper = new RequestHelper(httpClient, new ObjectMapper());
        return requestHelper;
    }

    class DeployPlan {

        private final File artifact;
        private final String version;
        private final ReleaseGroup releaseGroup;
        private final ConcurrentHashMap<String, DeployHost> deployHosts = new ConcurrentHashMap<>();

        public DeployPlan(File artifact, String version, ReleaseGroup releaseGroup) {
            this.artifact = artifact;
            this.version = version;
            this.releaseGroup = releaseGroup;
        }

        public void add(String hostKey, Host host, String instanceKey, Instance instance) {
            DeployHost deployHost = deployHosts.get(hostKey);
            if (deployHost == null) {
                deployHost = new DeployHost(host, releaseGroup);
                DeployHost had = deployHosts.putIfAbsent(hostKey, deployHost);
                if (had != null) {
                    deployHost = had;
                }
            }
            deployHost.add(instanceKey, instance);
        }

        private void upload() throws MojoFailureException {

            String name = artifact.getName();
            String extension = "";
            for (String ext : new String[]{".tgz", ".tar.gz", "jar", "zip"}) {
                if (name.endsWith(ext)) {
                    extension = ext;
                    break;
                }
            }

            if (!artifact.exists()) {
                getLog().error("Failed because artifact:" + artifact + " is missing.");
                throw new MojoFailureException("Failed because artifact:" + artifact + " is missing.");
            }

            byte[] fileBytes;
            try {
                fileBytes = FileUtils.readFileToByteArray(artifact);
            } catch (Exception x) {
                getLog().error("failed to load artifact:" + artifact + " into a byte[]. ");
                throw new MojoFailureException("failed to load artifact:" + artifact + " into a byte[]. ", x);
            }

            for (DeployHost deployHost : deployHosts.values()) {
                deployHost.upload(fileBytes, version, extension);
            }

        }

        private void deploy(String serviceName, RequestHelper requestHelper, String version) throws MojoFailureException {
            for (DeployHost deployHost : deployHosts.values()) {
                deployHost.deploy(serviceName, requestHelper, version);
            }
        }
    }

    class DeployHost {

        private final Host host;
        private final ReleaseGroup releaseGroup;
        private final ConcurrentHashMap<String, Instance> instances = new ConcurrentHashMap<>();

        public DeployHost(Host host, ReleaseGroup releaseGroup) {
            this.host = host;
            this.releaseGroup = releaseGroup;
        }

        public void add(String instanceKey, Instance instance) {
            instances.put(instanceKey, instance);
        }

        private void upload(byte[] fileBytes, String version, String extension) throws MojoFailureException {
            String hostColonPort = host.hostName + ":" + host.port;
            getLog().info("Uploading version:" + version + " size:" + fileBytes.length + " bytes to " + hostColonPort);
            DeployableUpload deployableUpload = new DeployableUpload(new ArrayList<>(instances.keySet()), version, fileBytes, extension);
            RequestHelper client = buildRequestHelper(host.hostName, host.port);
            String result = client.executeRequest(deployableUpload, "/uba/upload", String.class, null);
            if (result == null) {
                getLog().error("failed to upload artifact to host:" + hostColonPort);
                throw new MojoFailureException("failed to upload artifact to host:" + hostColonPort);
            } else {
                getLog().info("Uploaded to " + hostColonPort + " " + result);

            }
        }

        private void deploy(String serviceName, RequestHelper requestHelper, String version) throws MojoFailureException {
            Map<String, ReleaseGroup> cacheReleaseGroups = new HashMap<>();
            getLog().info("------------------------------------------------------------------------");

            for (Entry<String, Instance> e : instances.entrySet()) {
                Instance instance = e.getValue();
                ReleaseGroup rg = cacheReleaseGroups.get(instance.releaseGroupKey.getKey());
                if (rg == null) {
                    ReleaseGroup update = new ReleaseGroup(releaseGroup.name,
                            releaseGroup.email,
                            mavenProject.getGroupId() + ":" + mavenProject.getArtifactId() + ":" + version,
                            "Created by deployer plugin. " + new Date());

                    ReleaseGroupKey releaseGroupKey = requestHelper
                            .executeRequest(update, "/upena/releaseGroup/update?key=" + instance.releaseGroupKey.getKey(), ReleaseGroupKey.class, null);

                    if (releaseGroupKey == null) {
                        throw new MojoFailureException("Failed to update releaseGroupKey:" + e.getKey());
                    }
                    cacheReleaseGroups.put(instance.releaseGroupKey.getKey(), update);
                    rg = update;
                    getLog().info(" Updated release group:" + rg.name + " to version: " + rg.version);

                }

                getLog().info(" Instance: " + serviceName + "." + instance.instanceId + " will be availiable shortly "
                        + "on host:" + host.hostName + " port:" + instance.ports.get("main").port);
            }
            getLog().info("------------------------------------------------------------------------");

        }
    }
}