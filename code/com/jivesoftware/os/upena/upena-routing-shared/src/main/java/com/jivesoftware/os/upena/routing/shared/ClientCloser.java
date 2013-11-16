package com.jivesoftware.os.upena.routing.shared;

public interface ClientCloser<C> {

    void closeClient(C client);
}
