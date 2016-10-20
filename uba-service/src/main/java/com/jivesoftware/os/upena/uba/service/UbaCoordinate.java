package com.jivesoftware.os.upena.uba.service;

/**
 *
 * @author jonathan.colt
 */
public class UbaCoordinate {

    public final String datacenter;
    public final String rack;
    public final String publicHostName;
    public final String host;
    public final String upenaHost;
    public final int upenaPort;

    public UbaCoordinate(String datacenter, String rack, String publicHostName, String host, String upenaHost, int upenaPort) {
        this.datacenter = datacenter;
        this.rack = rack;
        this.publicHostName = publicHostName;
        this.host = host;
        this.upenaHost = upenaHost;
        this.upenaPort = upenaPort;
    }
}
