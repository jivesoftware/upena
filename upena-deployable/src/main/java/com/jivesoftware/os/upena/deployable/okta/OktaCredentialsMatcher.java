package com.jivesoftware.os.upena.deployable.okta;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.okta.client.clients.AuthApiClient;
import com.jivesoftware.os.upena.deployable.okta.client.clients.FactorsApiClient;
import com.jivesoftware.os.upena.deployable.okta.client.clients.UserApiClient;
import com.jivesoftware.os.upena.deployable.okta.client.framework.ApiClientConfiguration;
import com.jivesoftware.os.upena.deployable.okta.client.models.auth.AuthResult;
import com.jivesoftware.os.upena.deployable.okta.client.models.factors.Factor;
import com.jivesoftware.os.upena.deployable.okta.client.models.users.User;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.SecureRandom;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;

/**
 * Created by jonathan.colt on 11/8/16.
 */
public class OktaCredentialsMatcher implements CredentialsMatcher {

    public static void main(String[] args) throws IOException {
        ApiClientConfiguration apiClientConfiguration = new ApiClientConfiguration("https://jive.okta.com", "00VITfHJrIusgZaf0AS0pCGGxV4HPHNV54gDrqP4NN");


        UserApiClient userApiClient = new UserApiClient(apiClientConfiguration);

        User user = userApiClient.getUser("jonathan.colt");
        System.out.println(user.getId());

        FactorsApiClient client = new FactorsApiClient(apiClientConfiguration);


        for (Factor factor : client.getUserLifecycleFactors(user.getId())) {
            System.out.println(factor.getFactorType() + " " + factor.getId() + " " + factor.getStatus());
        }





       /* AuthApiClient authApiClient = new AuthApiClient(apiClientConfiguration);
        AuthResult result = authApiClient.authenticate(
            "jonathan.colt",
            new String(""),
            "relay");*/

        //System.out.println(result.getIdToken());
    }


    public static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    public static OktaLog oktaLog;

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
                LOG.warn("Failed to load okta key.", x);
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

        OktaUsernamePasswordToken oktaUsernamePasswordToken = (OktaUsernamePasswordToken) token;

        AuthApiClient authApiClient = new AuthApiClient(apiClientConfiguration);
        if (oktaUsernamePasswordToken.getToken() != null) {
            try {
                String factorType = System.getProperty("okta.mfa.factorType");
                if (factorType == null) {
                    LOG.error("You must specifiy an okta.mfa.factorType");
                    return false;
                }

                UserApiClient userApiClient = new UserApiClient(apiClientConfiguration);

                User user = userApiClient.getUser(oktaUsernamePasswordToken.getUsername());
                FactorsApiClient client = new FactorsApiClient(apiClientConfiguration);
                String factorId = null;
                for (Factor factor : client.getUserLifecycleFactors(user.getId())) {
                    if (factor.getFactorType().equals(factorType)) {
                        factorId = factor.getId();
                        break;
                    }
                }

                if (factorId == null) {
                    LOG.error("The user:{} doesn't have a factor of type:{}", oktaUsernamePasswordToken.getUsername(), factorType);
                    return false;
                }


                AuthResult result = authApiClient.authenticateWithFactor(oktaUsernamePasswordToken.getToken(),
                    factorId,
                    oktaUsernamePasswordToken.getPassCode(),
                    oktaUsernamePasswordToken.getRelay(),
                    oktaUsernamePasswordToken.isRememberMe());

                if (result.getStatus().equals("SUCCESS")) {
                    oktaLog.record(oktaUsernamePasswordToken.getUsername(), "login", "Success", "oktaAPI");
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                oktaLog.record(((UsernamePasswordToken) token).getUsername(), "login", "Failed", "oktaAPI");
                LOG.error("Authentication failed:", e);
                return false;
            }

        } else {


            try {
                SecureRandom random = new SecureRandom();
                AuthResult result = authApiClient.authenticate(
                    oktaUsernamePasswordToken.getUsername(),
                    new String(oktaUsernamePasswordToken.getPassword()),
                    new BigInteger(130, random).toString(32));

                //LOG.info("result:" + result);
                //LOG.info("expires:" + result.getExpiresAt().toString());
                //LOG.info("status:" + result.getStatus().toString());

                if (result.getStatus().equals("MFA_REQUIRED")) {
                    throw new OktaMFARequiredException(oktaUsernamePasswordToken.getUsername(), result.getStateToken(), result.getRelayState());
                } else if (result.getStatus().equals("SUCCESS")) {
                    oktaLog.record(oktaUsernamePasswordToken.getUsername(), "login", "Success", "oktaAPI");
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                oktaLog.record(((UsernamePasswordToken) token).getUsername(), "login", "Failed", "oktaAPI");
                LOG.error("Authentication failed:", e);
                return false;
            }
        }
    }
}
