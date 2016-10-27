package com.jivesoftware.os.upena.deployable.lookup;

import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelperUtils;
import com.jivesoftware.os.upena.deployable.UpenaSSLConfig;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterFilter;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostFilter;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceFilter;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 *
 */
public class AsyncLookupService {

    private final UpenaSSLConfig upenaSSLConfig;
    private final UpenaStore upenaStore;

    public AsyncLookupService(UpenaSSLConfig upenaSSLConfig, UpenaStore upenaStore) {
        this.upenaSSLConfig = upenaSSLConfig;
        this.upenaStore = upenaStore;
    }

    public static class ClusterResults extends ConcurrentSkipListMap<ClusterKey, TimestampedValue<Cluster>> {

        @Override
        public ConcurrentSkipListMap<ClusterKey, TimestampedValue<Cluster>> clone() {
            return super.clone(); //To change body of generated methods, choose Tools | Templates.
        }
    }

    public Map<ClusterKey, TimestampedValue<Cluster>> findClusters(String remoteHost,
        int remotePort,
        String contains) throws Exception {

        ClusterFilter clusterFilter = new ClusterFilter(contains, null, 0, 100_000);
        if (remotePort != -1) {
            HttpRequestHelper helper = HttpRequestHelperUtils.buildRequestHelper(upenaSSLConfig.sslEnable,
                upenaSSLConfig.allowSelfSignedCerts,
                upenaSSLConfig.signer,
                remoteHost, remotePort);
            return helper.executeRequest(clusterFilter, "/upena/cluster/find", ClusterResults.class, new ClusterResults());
        } else {
            return upenaStore.clusters.find(false, clusterFilter);
        }
    }

    public static class HostResults extends ConcurrentSkipListMap<HostKey, TimestampedValue<Host>> {

        @Override
        public ConcurrentSkipListMap<HostKey, TimestampedValue<Host>> clone() {
            return super.clone(); //To change body of generated methods, choose Tools | Templates.
        }
    }

    public Map<HostKey, TimestampedValue<Host>> findHosts(String remoteHost,
        int remotePort, String contains) throws Exception {
        HostFilter hostsFilter = new HostFilter(contains, null, null, null, null, 0, 100_000);
        if (remotePort != -1) {
            HttpRequestHelper helper = HttpRequestHelperUtils.buildRequestHelper(upenaSSLConfig.sslEnable, upenaSSLConfig.allowSelfSignedCerts,
                upenaSSLConfig.signer,
                remoteHost, remotePort);
            return helper.executeRequest(hostsFilter, "/upena/host/find", HostResults.class, new HostResults());
        } else {
            return upenaStore.hosts.find(false, hostsFilter);
        }
    }

    public static class ServiceResults extends ConcurrentSkipListMap<ServiceKey, TimestampedValue<Service>> {

        @Override
        public ConcurrentSkipListMap<ServiceKey, TimestampedValue<Service>> clone() {
            return super.clone(); //To change body of generated methods, choose Tools | Templates.
        }
    }

    public Map<ServiceKey, TimestampedValue<Service>> findServices(String remoteHost,
        int remotePort, String contains) throws Exception {
        ServiceFilter serviceFilter = new ServiceFilter(contains, null, 0, 100_000);
        if (remotePort != -1) {
            HttpRequestHelper helper = HttpRequestHelperUtils.buildRequestHelper(upenaSSLConfig.sslEnable, upenaSSLConfig.allowSelfSignedCerts,
                upenaSSLConfig.signer,
                remoteHost, remotePort);
            return helper.executeRequest(serviceFilter, "/upena/service/find", ServiceResults.class, new ServiceResults());
        } else {
            return upenaStore.services.find(false, serviceFilter);
        }
    }

    public static class ReleaseGroupResults extends ConcurrentSkipListMap<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> {

        @Override
        public ConcurrentSkipListMap<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> clone() {
            return super.clone(); //To change body of generated methods, choose Tools | Templates.
        }
    }

    public Map<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> findReleases(String remoteHost,
        int remotePort, String contains) throws Exception {
        ReleaseGroupFilter releasesFilter = new ReleaseGroupFilter(contains, null, null, null, null, 0, 100_000);
        if (remotePort != -1) {
            HttpRequestHelper helper = HttpRequestHelperUtils.buildRequestHelper(upenaSSLConfig.sslEnable, upenaSSLConfig.allowSelfSignedCerts,
                upenaSSLConfig.signer,
                remoteHost, remotePort);
            return helper.executeRequest(releasesFilter, "/upena/releaseGroup/find", ReleaseGroupResults.class, new ReleaseGroupResults());
        } else {
            return upenaStore.releaseGroups.find(false, releasesFilter);
        }
    }
}
