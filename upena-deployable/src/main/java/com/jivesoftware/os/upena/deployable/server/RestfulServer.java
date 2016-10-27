package com.jivesoftware.os.upena.deployable.server;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 *
 * @author jonathan.colt
 */
public class RestfulServer {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    // Copying the default behavior inside jetty
    private static final int ACCEPTORS = Math.max(1, (Runtime.getRuntime().availableProcessors()) / 2);
    private static final int SELECTORS = Runtime.getRuntime().availableProcessors();

    private static final int MIN_THREADS = 8;
    private static final int IDLE_TIMEOUT = 60000;

    private final Server server;
    private final QueuedThreadPool queuedThreadPool;
    private final String applicationName;
    private final ContextHandlerCollection handlers;

    public RestfulServer(int port, final String applicationName, int maxNumberOfThreads, int maxQueuedRequests) {
        this.applicationName = applicationName;
        int maxThreads = maxNumberOfThreads + ACCEPTORS + SELECTORS;
        BlockingArrayQueue<Runnable> queue = new BlockingArrayQueue<>(MIN_THREADS, MIN_THREADS, maxQueuedRequests);
        this.queuedThreadPool = new QueuedThreadPool(maxThreads, MIN_THREADS, IDLE_TIMEOUT, queue);
        this.server = new Server(queuedThreadPool);
        this.handlers = new ContextHandlerCollection();

        server.setHandler(handlers);
        server.addEventListener(new MBeanContainer(ManagementFactory.getPlatformMBeanServer()));
        server.addConnector(makeConnector(port));

    }

    public int getIdleThreads() {
        return queuedThreadPool.getIdleThreads();
    }

    public boolean isLowOnThreads() {
        return queuedThreadPool.isLowOnThreads();
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
        resourceHandler.setCacheControl("public, max-age=31536000");
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
