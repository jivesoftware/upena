package com.jivesoftware.os.upena.hello.echo.bird.deployable;

import com.jivesoftware.jive.symphony.uba.config.extractor.ConfigBinder;
import com.jivesoftware.os.jive.utils.base.service.ServiceHandle;
import com.jivesoftware.os.server.http.jetty.jersey.server.InitializeRestfulServer;
import com.jivesoftware.os.server.http.jetty.jersey.server.JerseyEndpoints;
import com.jivesoftware.os.server.http.jetty.jersey.server.RestfulManageServer;
import com.jivesoftware.os.server.http.jetty.jersey.server.util.Resource;
import com.jivesoftware.os.upena.hello.routing.bird.service.HelloRoutingBirdService;
import com.jivesoftware.os.upena.hello.routing.bird.service.HelloRoutingBirdServiceInitializer;
import com.jivesoftware.os.upena.hello.routing.bird.service.HelloRoutingBirdServiceInitializer.HelloRoutingBirdServiceConfig;
import com.jivesoftware.os.upena.hello.routing.bird.service.endpoints.HelloRoutingBirdServiceRestEndpoints;
import com.jivesoftware.os.upena.main.RoutableDeployableConfig;
import com.jivesoftware.os.upena.main.TenantRoutingBirdBuilder;
import com.jivesoftware.os.upena.routing.shared.TenantRoutingProvider;
import com.jivesoftware.os.upena.routing.shared.TenantsServiceConnectionDescriptorProvider;
import com.jivesoftware.os.upena.tenant.routing.http.client.TenantRoutingHttpClient;
import com.jivesoftware.os.upena.tenant.routing.http.client.TenantRoutingHttpClientInitializer;
import com.jivesoftware.os.upena.tenant.routing.http.server.endpoints.TenantRoutingRestEndpoints;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

    public void run(String[] args) throws Exception {

        Properties properties = new Properties();
        for (String arg : args) {
            File f = new File(arg);
            if (f.exists()) {
                properties.load(new FileInputStream(f));
            }
        }
        ConfigBinder configBinder = new ConfigBinder(properties);
        RoutableDeployableConfig routableDeployableConfig = configBinder.bind(RoutableDeployableConfig.class);

        TenantRoutingBirdBuilder tenantRoutingBirdBuilder = new TenantRoutingBirdBuilder(routableDeployableConfig.getRoutesHost(),
                routableDeployableConfig.getRoutesPort());
        tenantRoutingBirdBuilder.setInstanceId(routableDeployableConfig.getInstanceGUID());
        TenantRoutingProvider tenantRoutingProvider = tenantRoutingBirdBuilder.build();

        String applicationName = "manage " + routableDeployableConfig.getServiceName() + " " + routableDeployableConfig.getCluster();
        RestfulManageServer restfulManageServer = new RestfulManageServer(routableDeployableConfig.getManagePort(),
                applicationName,
                args[0], // HACK
                128,
                10000);

        restfulManageServer.addEndpoint(TenantRoutingRestEndpoints.class);
        restfulManageServer.addInjectable(TenantRoutingProvider.class, tenantRoutingProvider);
        restfulManageServer.initialize();
        restfulManageServer.start();


        TenantsServiceConnectionDescriptorProvider connections = tenantRoutingProvider.getConnections("hello-echo-bird-deployable", "main");
        TenantRoutingHttpClientInitializer<String> tenantRoutingHttpClientInitializer = new TenantRoutingHttpClientInitializer<>();
        TenantRoutingHttpClient<String> client = tenantRoutingHttpClientInitializer.initialize(connections);
        HelloRoutingBirdService helloRoutingBirdService = new HelloRoutingBirdServiceInitializer()
                .initialize(configBinder.bind(HelloRoutingBirdServiceConfig.class), client);

        JerseyEndpoints jerseyEndpoints = new JerseyEndpoints()
                .addEndpoint(HelloRoutingBirdServiceRestEndpoints.class)
                .addInjectable(helloRoutingBirdService);

        Resource resource = new Resource().setDirectoryListingAllowed(true);

        ServiceHandle serviceHandle = new InitializeRestfulServer(routableDeployableConfig.getPort(),
                applicationName,
                128,
                10000)
                .addContextHandler("/", jerseyEndpoints)
                .addResource(resource)
                .build();
        serviceHandle.start();

    }
}
