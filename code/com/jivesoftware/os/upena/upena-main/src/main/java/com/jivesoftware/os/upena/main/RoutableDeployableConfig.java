package com.jivesoftware.os.upena.main;

import org.merlin.config.Config;
import org.merlin.config.defaults.Default;
import org.merlin.config.defaults.IntDefault;

public interface RoutableDeployableConfig extends Config {

    @Default("unspecified")
    String getInstanceGUID();

    @Default("local")
    String getCluster();

    @Default("localhost")
    String getHost();

    @Default("unspecified")
    String getServiceName();

    @Default("unspecified")
    String getVersion();

    @IntDefault(-1)
    Integer getInstanceId();

    @IntDefault(-1)
    Integer getManagePort();

    @IntDefault(-1)
    Integer getPort();

    @Default("localhost")
    String getRoutesHost();

    @IntDefault(-1)
    Integer getRoutesPort();
}