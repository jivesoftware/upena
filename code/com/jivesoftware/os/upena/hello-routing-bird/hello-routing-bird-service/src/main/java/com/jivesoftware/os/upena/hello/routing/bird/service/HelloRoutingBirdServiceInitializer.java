package com.jivesoftware.os.upena.hello.routing.bird.service;

import com.jivesoftware.os.upena.tenant.routing.http.client.TenantRoutingHttpClient;
import org.merlin.config.Config;
import org.merlin.config.defaults.StringDefault;

public class HelloRoutingBirdServiceInitializer {

    public interface HelloRoutingBirdServiceConfig extends Config {

        @StringDefault("Hello World")
        public String getGreeting();
    }

    public HelloRoutingBirdService initialize(HelloRoutingBirdServiceConfig config,
            TenantRoutingHttpClient<String> client) {
        return new HelloRoutingBirdService(config.getGreeting(), client);
    }
}