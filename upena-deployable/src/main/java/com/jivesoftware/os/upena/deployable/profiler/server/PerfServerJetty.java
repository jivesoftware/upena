package colt.nicity.performance.server;

/**
 * Hello world!
 *
 */
public class PerfServerJetty {
    /*
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);

        ViewColor.onBlack();
        final ServicesCallDepthStack callDepthStack = new ServicesCallDepthStack();
        NameUtils nameUtils = new NameUtils();

        final VisualizeProfile vCallDepthStack = new VisualizeProfile(nameUtils, callDepthStack, new Heat(), 1000, 800);
        UV.exitFrame(new VLatency(vCallDepthStack), "Latent Server");

        FilterHolder filterHolder = new FilterHolder(CrossOriginFilter.class);
        filterHolder.setInitParameter("allowedOrigins", "*");
        filterHolder.setInitParameter("allowedMethods", "GET, POST");

        ServletContextHandler root = new ServletContextHandler(server, "/", ServletContextHandler.NO_SESSIONS);
        //root.setErrorHandler(new SegmentsErrorHandler(objectMapper, new Log4jLoggerServerErrorRecorder(objectMapper)));

        final String JERSEY_RESOURCE_PACKAGES = "colt.nicity.performance.server.endpoints";
        Map<String, Object> jerseyProps = new HashMap<String, Object>() {
            {
                put(PackagesResourceConfig.PROPERTY_PACKAGES, JERSEY_RESOURCE_PACKAGES);
                put(PackagesResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, PostReplaceFilter.class.getName());
                //put(PackagesResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES, CacheControlResourceFilterFactory.class.getName());
            }
        };
        PackagesResourceConfig jerseyResourceConfig = new PackagesResourceConfig(jerseyProps);
        jerseyResourceConfig.getSingletons().add(new SingletonTypeInjectableProvider<javax.ws.rs.core.Context, AtomicBoolean>(AtomicBoolean.class, new AtomicBoolean(false)) {
        });
        jerseyResourceConfig.getSingletons().add(
            new SingletonTypeInjectableProvider<javax.ws.rs.core.Context, PerfService>(PerfService.class, new PerfService(callDepthStack)) {
        });

        ServletHolder servletHolder = new ServletHolder(new ServletContainer(jerseyResourceConfig));

        root.addServlet(servletHolder, "/*");

        server.start();
        server.join();

    }*/
}
