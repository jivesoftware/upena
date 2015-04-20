package com.jivesoftware.os.upena.deployable.server;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.Collections;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 *
 * @author jonathan.colt
 */
public class RestfulServer {

    // Copying the default behavior inside jetty
    private static final int ACCEPTORS = Math.max(1, (Runtime.getRuntime().availableProcessors()) / 2);
    private static final int SELECTORS = Runtime.getRuntime().availableProcessors();

    private static final int MIN_THREADS = 8;
    private static final int IDLE_TIMEOUT = 60000;

    private static Server makeServer(final int maxNumberOfThreads, final int maxQueuedRequests) {
        final int maxThreads = maxNumberOfThreads + ACCEPTORS + SELECTORS;
        final BlockingArrayQueue<Runnable> queue = new BlockingArrayQueue<>(MIN_THREADS, MIN_THREADS, maxQueuedRequests);
        return new Server(new QueuedThreadPool(maxThreads, MIN_THREADS, IDLE_TIMEOUT, queue));
    }

    private final Server server;
    private final String applicationName;
    private final ContextHandlerCollection handlers;

    public RestfulServer(int port, final String applicationName, int maxNumberOfThreads, int maxQueuedRequests) {
        this.applicationName = applicationName;
        this.server = makeServer(maxNumberOfThreads, maxQueuedRequests);

        HashLoginService loginService = new HashLoginService();
        loginService.putUser("admin", new Password("admin"), new String[]{"user", "admin"});
        server.addBean(loginService);

        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        server.setHandler(security);

        Constraint constraint = new Constraint();
        constraint.setName("auth");
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[]{"user", "admin"});

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/ui/*");
        mapping.setConstraint(constraint);

        security.setConstraintMappings(Collections.singletonList(mapping));
        security.setAuthenticator(new BasicAuthenticator());
        security.setLoginService(loginService);

        this.handlers = new ContextHandlerCollection();
        security.setHandler(handlers);

        server.addEventListener(new MBeanContainer(ManagementFactory.getPlatformMBeanServer()));
        server.addConnector(makeConnector(port));

    }

    private Connector makeConnector(int port) {
        ServerConnector connector = new ServerConnector(server, ACCEPTORS, SELECTORS);
        connector.setPort(port);
        return connector;
    }

    public void addContextHandler(String context, HasServletContextHandler contextHandler) {
        if (context == null || contextHandler == null) {
            return;
        }
        Handler handler = contextHandler.getHandler(server, context, applicationName);
        handlers.addHandler(handler);
    }

    public void addClasspathResource(String path) throws Exception {
        addResourcesDir(path, "static");
    }

    private void addResourcesDir(String path, String dir) throws IOException, URISyntaxException {
        Resource newResource = Resource.newResource(this.getClass().getResource(path + "/" + dir).toURI());
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setBaseResource(newResource);
        resourceHandler.setCacheControl("cache-control: public, max-age=31536000");
        ContextHandler ctx = new ContextHandler("/" + dir);
        ctx.setHandler(resourceHandler);
        handlers.addHandler(ctx);
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

}
