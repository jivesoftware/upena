package com.jivesoftware.os.upena.deployable.okta;

import org.apache.shiro.authc.AuthenticationException;

/**
 * Created by jonathan.colt on 12/13/16.
 */
public class OktaMFARequiredException extends AuthenticationException {

    private final String userName;
    private final String token;
    private final String relay;


    public OktaMFARequiredException(String userName, String token, String relay) {
        this.userName = userName;
        this.token = token;
        this.relay = relay;
    }

    public String getUserName() {
        return userName;
    }

    public String getToken() {
        return token;
    }

    public String getRelay() {
        return relay;
    }
}
