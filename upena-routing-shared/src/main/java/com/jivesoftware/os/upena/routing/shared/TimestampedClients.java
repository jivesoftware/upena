package com.jivesoftware.os.upena.routing.shared;

/**
 *
 */
public interface TimestampedClients<C, E extends Throwable> {

    <R> R call(NextClientStrategy strategy, ClientCall<C, R, E> httpCall) throws E;

    long getTimestamp();

    C[] getClients();
}
