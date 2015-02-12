package com.jivesoftware.os.upena.deployable.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;

/**
 *
 * @author jonathan.colt
 */
public interface HasServletContextHandler {

    Handler getHandler(Server server, String context, String applicationName);
}
