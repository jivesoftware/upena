package com.jivesoftware.os.upena.deployable.okta;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.okta.client.clients.AuthApiClient;
import com.jivesoftware.os.upena.deployable.okta.client.framework.ApiClientConfiguration;
import com.jivesoftware.os.upena.deployable.okta.client.models.auth.AuthResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;

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

        String userRolesDirectory = System.getProperty("okta.user.roles.directory");
        if (userRolesDirectory == null) {
            return false;
        }

        String apiKey = null;
        File oktaApiKey = new File(userRolesDirectory, "oktaApi.key");
        if (oktaApiKey.exists()) {
            try {
                apiKey = Files.readAllLines(oktaApiKey.toPath()).get(0);
            } catch (IOException x) {
                LOG.warn("Failed to load okta key.",x);
                return false;
            }
        }

        if (apiKey == null) {
            apiKey = System.getProperty("okta.api.key");
            if (baseUrl == null) {
                return false;
            }
        }

        ApiClientConfiguration apiClientConfiguration = new ApiClientConfiguration(baseUrl, apiKey);
        AuthApiClient authApiClient = new AuthApiClient(apiClientConfiguration);

        try {

            UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) token;
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
