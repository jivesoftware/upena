package com.jivesoftware.os.upena.main;

import com.jivesoftware.os.upena.routing.shared.TenantRoutingProvider;


public interface DeployableFoundationService {

    void initialize(TenantRoutingProvider tenantRoutingConnectionDescriptorsProvider,
            ComponentHealthCheckProvider componentHealthCheckProvider,
            KillSwitchProvider killSwitchProvider) throws Exception;
}