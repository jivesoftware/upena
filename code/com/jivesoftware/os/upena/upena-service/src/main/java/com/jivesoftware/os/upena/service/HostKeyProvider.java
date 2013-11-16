package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.upena.service.UpenaTable.UpenaKeyProvider;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import java.nio.charset.Charset;

public class HostKeyProvider implements UpenaKeyProvider<HostKey, Host> {

    private final JenkinsHash jenkinsHash = new JenkinsHash();
    private final Charset UTF8 = Charset.forName("utf-8");

    @Override
    public HostKey getNodeKey(UpenaTable<HostKey, Host> table, Host value) {
        String compositeKey = value.hostName + "|" + value.port + "|" + value.workingDirectory;
        String k = Long.toString(Math.abs(jenkinsHash.hash(compositeKey.getBytes(UTF8), 2)));
        return new HostKey(k);
    }
}
