package com.jivesoftware.os.upena.main;

public interface ComponentHealthCheckProvider {

    ComponentHealthCheck create(String name, boolean fatal);
}