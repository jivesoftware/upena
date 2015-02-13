package com.jivesoftware.os.upena.deployable.server;

import java.lang.management.ManagementFactory;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.resource.Resource;
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

    public RestfulServer(int port, String applicationName, int maxNumberOfThreads, int maxQueuedRequests) {
        this.applicationName = applicationName;
        this.server = makeServer(maxNumberOfThreads, maxQueuedRequests);
        this.handlers = new ContextHandlerCollection();

//        Constraint constraint = new Constraint();
//        constraint.setName(Constraint.__FORM_AUTH);;
//        constraint.setRoles(new String[]{"user", "admin", "moderator"});
//        constraint.setAuthenticate(true);
//
//        ConstraintMapping constraintMapping = new ConstraintMapping();
//        constraintMapping.setConstraint(constraint);
//        constraintMapping.setPathSpec("/*");
//
//        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
//        securityHandler.addConstraintMapping(constraintMapping);
//
//        HashLoginService loginService = new HashLoginService();
//        loginService.putUser("username", new Password("password"), new String[]{"user"});
//        securityHandler.setLoginService(loginService);
//
//        FormAuthenticator authenticator = new FormAuthenticator("/login", "/login", false);
//        securityHandler.setAuthenticator(authenticator);
//
//        ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS | ServletContextHandler.SECURITY);
//
//        context.addServlet(new ServletHolder(new DefaultServlet() {
//            @Override
//            protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//                response.getWriter().append("hello " + request.getUserPrincipal().getName());
//            }
//        }), "/*");
//
//        context.addServlet(new ServletHolder(new DefaultServlet() {
//            @Override
//            protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//                response.getWriter().append("<html><div style=\"text-align:center\"><table style=\"display: inline-table;\">"
//                    + "<tr><td><img src=\"/static/img/icon.png\" alt=\"Fun with deployment\"><img></td></tr>"
//                    + "<tr><td><form method='POST' action='/j_security_check'>"
//                    + "<input type='text' name='j_username'/>"
//                    + "<input type='password' name='j_password'/>"
//                    + "<input type='submit' value='Login'/></form></td></tr></table></div></html>");
//            }
//        }), "/login");
//        context.setSecurityHandler(securityHandler);
//        handlers.addHandler(context);
        handlers.addHandler(new DefaultHandler());

        server.addEventListener(new MBeanContainer(ManagementFactory.getPlatformMBeanServer()));
        server.setHandler(handlers);
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

    public void addClasspathReasource(String path) throws Exception {

        Resource newResource = Resource.newResource(this.getClass().getResource(path).toURI());
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setBaseResource(newResource);
        ContextHandler ctx = new ContextHandler("/");
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
