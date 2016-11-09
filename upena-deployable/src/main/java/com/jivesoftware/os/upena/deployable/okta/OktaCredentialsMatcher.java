package com.jivesoftware.os.upena.deployable.okta;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.okta.client.clients.AuthApiClient;
import com.jivesoftware.os.upena.deployable.okta.client.framework.ApiClientConfiguration;
import com.jivesoftware.os.upena.deployable.okta.client.models.auth.AuthResult;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;

import java.io.IOException;

/**
 * Created by jonathan.colt on 11/8/16.
 */
public class OktaCredentialsMatcher implements CredentialsMatcher {

    public static final MetricLogger LOG = MetricLoggerFactory.getLogger();


    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {

        String baseUrl = System.getProperty("okta.base.url");
        if (baseUrl == null) {
            return false;
        }
        String apiKey = System.getProperty("okta.api.key");
        if (baseUrl == null) {
            return false;
        }
        ApiClientConfiguration apiClientConfiguration = new ApiClientConfiguration(baseUrl, apiKey);
        AuthApiClient authApiClient = new AuthApiClient(apiClientConfiguration);

        try {


            UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) token;
            //LOG.info("user:" + usernamePasswordToken.getUsername());
            //LOG.info("password:" + new String(usernamePasswordToken.getPassword()));

            AuthResult result = authApiClient.authenticate(
                usernamePasswordToken.getUsername(),
                new String(usernamePasswordToken.getPassword()),
                "relayState");
            LOG.info("result:" + result);
            LOG.info("expires:" + result.getExpiresAt().toString());
            LOG.info("status:" + result.getStatus().toString());
            return result.getStatus().equals("SUCCESS");
        } catch (IOException e) {
            LOG.error("Authentication failed:", e);
            return false;
        }
    }
}
