package com.jivesoftware.os.uba.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DeployableUpload {

    public final List<String> instanceIds;
    public final String version;
    public final byte[] deployableFileBytes;
    public final String extension;

    @JsonCreator
    public DeployableUpload(@JsonProperty("instanceIds") List<String> instanceIds,
            @JsonProperty("version") String version,
            @JsonProperty("deployableFileBytes") byte[] deployableFileBytes,
            @JsonProperty("extension") String extension) {
        this.instanceIds = instanceIds;
        this.version = version;
        this.deployableFileBytes = deployableFileBytes;
        this.extension = extension;
    }

    @Override
    public String toString() {
        return "DeployableUpload{"
                + "instanceIds=" + instanceIds
                + ", version=" + version
                + ", deployableFileBytes=" + deployableFileBytes
                + ", extension=" + extension
                + '}';
    }

}