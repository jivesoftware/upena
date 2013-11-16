package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.upena.service.UpenaTable.UpenaKeyProvider;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceKey;
import java.nio.charset.Charset;

public class InstanceKeyProvider implements UpenaKeyProvider<InstanceKey, Instance> {

    private final JenkinsHash jenkinsHash = new JenkinsHash();
    private final Charset UTF8 = Charset.forName("utf-8");

    @Override
    public InstanceKey getNodeKey(UpenaTable<InstanceKey, Instance> table, Instance value) {
        String compositeKey = value.hostKey + "|" + value.clusterKey + "|" + value.serviceKey + "|" + value.releaseGroupKey + "|" + value.instanceId;
        String k = Long.toString(Math.abs(jenkinsHash.hash(compositeKey.getBytes(UTF8), 5)));
        return new InstanceKey(k);
    }
}
