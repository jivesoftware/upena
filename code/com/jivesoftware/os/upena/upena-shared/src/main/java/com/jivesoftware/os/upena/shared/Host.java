package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Host implements Stored<Host> {

    public final String name;
    public final String hostName;
    public final int port;
    public final String workingDirectory;
    public final ClusterKey clusterKey;

    @JsonCreator
    public Host(@JsonProperty("name") String name,
            @JsonProperty("hostName") String hostName,
            @JsonProperty("port") int port,
            @JsonProperty("workingDirectory") String workingDirectory,
            @JsonProperty("clusterKey") ClusterKey clusterKey) {
        this.name = name;
        this.hostName = hostName;
        this.port = port;
        this.workingDirectory = workingDirectory;
        this.clusterKey = clusterKey;
    }

    @Override
    public String toString() {
        return "Host{"
                + "name=" + name
                + ", hostName=" + hostName
                + ", port=" + port
                + ", workingDirectory=" + workingDirectory
                + ", clusterKey=" + clusterKey
                + '}';
    }

    @Override
    public int compareTo(Host o) {
        int i = hostName.compareTo(o.hostName);
        if (i == 0) {
            i = Integer.compare(port, o.port);
        }
        if (i == 0) {
            i = workingDirectory.compareTo(o.workingDirectory);
        }
        return i;
    }
}