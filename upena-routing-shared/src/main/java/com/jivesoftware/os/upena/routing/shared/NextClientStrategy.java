package com.jivesoftware.os.upena.routing.shared;

/**
 *
 */
public interface NextClientStrategy {

    int[] getClients(ConnectionDescriptor[] connectionDescriptors);

    void usedClientAtIndex(int index);
}
