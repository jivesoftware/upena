package com.jivesoftware.os.upena.deployable.server;


/**
 *
 * @author jonathan.colt
 */
public class InitializeRestfulServer {

    private final RestfulServer server;

    public InitializeRestfulServer(
        int port,
        String applicationName,
        int maxNumberOfThreads,
        int maxQueuedRequests) {
        server = new RestfulServer(port, applicationName, maxNumberOfThreads, maxQueuedRequests);
    }

    public InitializeRestfulServer addContextHandler(String context, HasServletContextHandler contextHandler) {
        server.addContextHandler(context, contextHandler);
        return this;
    }

    public InitializeRestfulServer addContextHandler(String context, JerseyEndpoints contextHandler) {
        contextHandler.humanReadableJson();
        server.addContextHandler(context, contextHandler);
        return this;
    }

    public InitializeRestfulServer addClasspathResource(String path) throws Exception {
        server.addClasspathReasource(path);
        return this;
    }

    public RestfulServer build() {
        return server;
    }
}
