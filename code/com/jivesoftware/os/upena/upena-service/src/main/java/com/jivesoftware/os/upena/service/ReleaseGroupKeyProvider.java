package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.upena.service.UpenaTable.UpenaKeyProvider;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import java.nio.charset.Charset;

public class ReleaseGroupKeyProvider implements UpenaKeyProvider<ReleaseGroupKey, ReleaseGroup> {

    private final JenkinsHash jenkinsHash = new JenkinsHash();
    private final Charset UTF8 = Charset.forName("utf-8");

    @Override
    public ReleaseGroupKey getNodeKey(UpenaTable<ReleaseGroupKey, ReleaseGroup> table, ReleaseGroup value) {
        String compositeKey = value.name + "|" + value.email;
            String k = Long.toString(Math.abs(jenkinsHash.hash(compositeKey.getBytes(UTF8), 4)));
            return new ReleaseGroupKey(k);
    }
}
