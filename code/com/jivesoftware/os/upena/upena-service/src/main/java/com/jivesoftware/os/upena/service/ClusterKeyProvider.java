package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.upena.service.UpenaTable.UpenaKeyProvider;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import java.nio.charset.Charset;

public class ClusterKeyProvider implements UpenaKeyProvider<ClusterKey, Cluster> {

    private final JenkinsHash jenkinsHash = new JenkinsHash();
    private final Charset UTF8 = Charset.forName("utf-8");

    @Override
    public ClusterKey getNodeKey(UpenaTable<ClusterKey, Cluster> table, Cluster value) {
        String k = Long.toString(Math.abs(jenkinsHash.hash(value.name.getBytes(UTF8), 1)));
        return new ClusterKey(k);
    }
}
