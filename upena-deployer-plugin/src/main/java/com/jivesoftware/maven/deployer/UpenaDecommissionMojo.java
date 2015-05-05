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
import com.jivesoftware.os.jive.utils.http.client.HttpClient;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfig;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfiguration;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactory;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.jive.utils.http.client.rest.RequestHelper;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterFilter;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceFilter;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
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
 * @goal decommission
 *
 * @phase validate
 */
public class UpenaDecommissionMojo extends AbstractMojo {

    /**
     *
     * @parameter property="upena.host" default-value="localhost"
     */
    private String composerHost;
    /**
     *
     * @parameter property="upena.port" default-value="1175"
     */
    private String composerPort;

    /**
     *
     * @parameter property="upena.release" default-value=""
     */
    private String release;

    /**
     * @parameter default-value="${project}"
     */
    private MavenProject mavenProject;

    @Override
    public void execute()
            throws MojoExecutionException, MojoFailureException {

        long time = System.currentTimeMillis();
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

        String serviceName = mavenProject.getArtifactId();
        StoredConfig config = repository.getConfig();
        String name = config.getString("user", null, "name");
        String email = config.getString("user", null, "email");

        String sha = "";
        try {
            final String currentBranchName = repository.getFullBranch();
            final ObjectId currentBranch = repository.resolve(currentBranchName);
//            getLog().info("git-branch:" + repository.getBranch());
//            getLog().info("git-origin:" + repository.getFullBranch());
//            getLog().info("git-sha:" + currentBranch.name());
            sha = currentBranch.name();
        } catch (IOException | RevisionSyntaxException x) {
            throw new MojoFailureException("failed to establish repo info.", x);
        }

        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider().createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(composerHost, Integer.parseInt(composerPort));
        RequestHelper requestHelper = new RequestHelper(httpClient, new ObjectMapper());

        String clusterName = "develop";

        ReleaseGroupKey releaseGroupKey = locateReleaseGroupKey(name, email, requestHelper);
        ServiceKey serviceKey = locateServiceKey(serviceName, requestHelper);
        ClusterKey clusterKey = locateClusterKey(clusterName, requestHelper, serviceKey, releaseGroupKey);
        if (releaseGroupKey != null && serviceKey != null && clusterKey != null) {

            InstanceResults results = null;
            try {
                getLog().info("------------------------------------------------------------------------");
                getLog().info(" Looking for declared instance for service:" + serviceName + ".");
                getLog().info("------------------------------------------------------------------------");
                InstanceFilter filter = new InstanceFilter(clusterKey, null, serviceKey, releaseGroupKey, null, 0, Integer.MAX_VALUE);
                results = requestHelper.executeRequest(filter, "/upena/instance/find", InstanceResults.class, null);
                if (results != null && !results.isEmpty()) {
                    for (Entry<String, TimestampedValue<Instance>> e : results.entrySet()) {
                        TimestampedValue<Instance> instance = e.getValue();
                        if (instance.getTombstoned()) {
                            continue;
                        }

                        InstanceKey removedKey = new InstanceKey(e.getKey());
                        Boolean removed = null;
                        try {
                            removed = requestHelper.executeRequest(removedKey,
                                    "/upena/instance/remove", Boolean.class, null);
                        } catch (Exception x) {
                            throw new MojoFailureException("failed to locate declared instance of service:" + serviceName, x);
                        }
                        if (removed != null) {
                            getLog().info(" Removed instanceKey:" + removedKey + " for service:" + serviceName);
                        } else {
                            throw new MojoFailureException("failed to remove instance:" + removedKey);
                        }
                    }
                } else {
                    getLog().info(" Instance is already decommissioned.");
                }
            } catch (Exception x) {
                throw new MojoFailureException("failed to locate declared instance of service:" + serviceName, x);
            }
        }

        getLog().info(" Decommission was SUCCESSFUL. Elapse:" + (System.currentTimeMillis() - time));
        getLog().info("------------------------------------------------------------------------");

    }

    protected ReleaseGroupKey locateReleaseGroupKey(String name, String email, RequestHelper requestHelper) throws MojoFailureException {
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
                    null,
                    null,
                    0,
                    Integer.MAX_VALUE);
            ReleaseGroupFilter.Results results = requestHelper.executeRequest(filter, "/upena/releaseGroup/find", ReleaseGroupFilter.Results.class, null);
            if (results != null && !results.isEmpty()) {
                Map.Entry<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> firstEntry = results.firstEntry();
                return firstEntry.getKey();
            } else {
                return null;
            }
        } catch (Exception x) {
            throw new MojoFailureException("failed to establish releaseGroup:" + name + " <" + email + ">", x);
        }
    }

    protected ClusterKey locateClusterKey(String clusterName,
            RequestHelper requestHelper,
            ServiceKey serviceKey,
            ReleaseGroupKey releaseGroupKey) throws MojoFailureException {
        try {
            ClusterFilter filter = new ClusterFilter(clusterName, null, 0, Integer.MAX_VALUE);
            ClusterFilter.Results results = requestHelper.executeRequest(filter, "/upena/cluster/find", ClusterFilter.Results.class, null);
            if (results != null && !results.isEmpty()) {
                Map.Entry<ClusterKey, TimestampedValue<Cluster>> firstEntry = results.firstEntry();
                return firstEntry.getKey();
            } else {
                return null;
            }
        } catch (Exception x) {
            throw new MojoFailureException("failed to establish cluster:" + clusterName, x);
        }
    }

    protected ServiceKey locateServiceKey(String serviceName, RequestHelper requestHelper) throws MojoFailureException {
        try {
            ServiceFilter filter = new ServiceFilter(serviceName, null, 0, Integer.MAX_VALUE);
            ServiceFilter.Results results = requestHelper.executeRequest(filter, "/upena/service/find", ServiceFilter.Results.class, null);
            if (results != null && !results.isEmpty()) {
                Map.Entry<ServiceKey, TimestampedValue<Service>> firstEntry = results.firstEntry();
                return firstEntry.getKey();

            } else {
                return null;
            }
        } catch (Exception x) {
            throw new MojoFailureException("failed to establish service:" + serviceName, x);
        }
    }

    public static class InstanceResults extends java.util.concurrent.ConcurrentSkipListMap<String, TimestampedValue<Instance>> {
    }

    RequestHelper buildRequestHelper(String host, int port) {
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider().createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(host, port);
        RequestHelper requestHelper = new RequestHelper(httpClient, new ObjectMapper());
        return requestHelper;
    }

}
