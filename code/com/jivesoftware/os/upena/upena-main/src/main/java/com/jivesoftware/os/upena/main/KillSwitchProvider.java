package com.jivesoftware.os.upena.main;

import com.jivesoftware.os.server.http.jetty.jersey.endpoints.killswitch.KillSwitch;

public interface KillSwitchProvider {

    KillSwitch create(String name, boolean initial);
}