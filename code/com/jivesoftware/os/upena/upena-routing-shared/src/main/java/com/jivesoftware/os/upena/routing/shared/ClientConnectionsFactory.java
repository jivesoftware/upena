package com.jivesoftware.os.upena.routing.shared;

public interface ClientConnectionsFactory<C> {

    C createClient(ConnectionDescriptors connections);
}
