package com.jivesoftware.os.upena.routing.shared;

public interface ClientCall<C, R, E extends Throwable> {

    R call(C client) throws E;
}
