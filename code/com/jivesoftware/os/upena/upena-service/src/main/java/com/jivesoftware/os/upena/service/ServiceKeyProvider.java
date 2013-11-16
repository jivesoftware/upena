package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.upena.service.UpenaTable.UpenaKeyProvider;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import java.nio.charset.Charset;

public class ServiceKeyProvider implements UpenaKeyProvider<ServiceKey, Service> {

    private final JenkinsHash jenkinsHash = new JenkinsHash();
    private final Charset UTF8 = Charset.forName("utf-8");

    @Override
    public ServiceKey getNodeKey(UpenaTable<ServiceKey, Service> table, Service value) {
        String k = Long.toString(jenkinsHash.hash(value.name.getBytes(UTF8), 3));
        return new ServiceKey(k);
    }
}
