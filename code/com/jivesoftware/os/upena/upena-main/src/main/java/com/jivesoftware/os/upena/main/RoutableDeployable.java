package com.jivesoftware.os.upena.main;

import com.jivesoftware.jive.symphony.uba.config.extractor.ConfigBinder;
import com.jivesoftware.os.server.http.jetty.jersey.server.RestfulManageServer;

public interface RoutableDeployable {

    void run(ConfigBinder configBinder, RestfulManageServer restfulManageServer, ShutdownHook shutdownHook) throws Exception;
}