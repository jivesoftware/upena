package com.jivesoftware.os.upena.deployable.lookup;

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

/**
 *
 */
public class AsyncLookupService {

    private final UpenaStore upenaStore;

    public AsyncLookupService(UpenaStore upenaStore) {
        this.upenaStore = upenaStore;
    }

    public Map<ClusterKey, TimestampedValue<Cluster>> findClusters(String contains) throws Exception {
        ClusterFilter clusterFilter = new ClusterFilter(contains, null, 0, 10000);
        return upenaStore.clusters.find(clusterFilter);
    }

    public Map<HostKey, TimestampedValue<Host>> findHosts(String contains) throws Exception {
        HostFilter hostsFilter = new HostFilter(contains, null, null, null, null, 0, 10000);
        return upenaStore.hosts.find(hostsFilter);
    }

    public Map<ServiceKey, TimestampedValue<Service>> findServices(String contains) throws Exception {
        ServiceFilter serviceFilter = new ServiceFilter(contains, null, 0, 10000);
        return upenaStore.services.find(serviceFilter);
    }

    public Map<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> findReleases(String contains) throws Exception {
        ReleaseGroupFilter releasesFilter = new ReleaseGroupFilter(contains, null, null, null, null, 0, 10000);
        return upenaStore.releaseGroups.find(releasesFilter);
    }
}
