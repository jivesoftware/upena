package com.jivesoftware.os.upena.deployable.okta;

import org.apache.shiro.authc.UsernamePasswordToken;

/**
 * Created by jonathan.colt on 12/13/16.
 */
public class OktaUsernamePasswordToken extends UsernamePasswordToken {

    private final String passCode;
    private final String token;
    private final String relay;

    public OktaUsernamePasswordToken(String passCode,
        String token,
        String relay,
        String userName,
        String password,
        boolean isRemeberMe,
        String host) {

        super(userName, password, isRemeberMe, host);
        this.token = token;
        this.relay = relay;
        this.passCode = passCode;
    }

    public String getToken() {
        return token;
    }

    public String getRelay() {
        return relay;
    }

    public String getPassCode() {
        return passCode;
    }
}
