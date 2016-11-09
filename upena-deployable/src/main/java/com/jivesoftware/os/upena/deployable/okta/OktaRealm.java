package com.jivesoftware.os.upena.deployable.okta;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.okta.client.clients.UserGroupApiClient;
import com.jivesoftware.os.upena.deployable.okta.client.framework.ApiClientConfiguration;
import com.jivesoftware.os.upena.deployable.okta.client.models.usergroups.UserGroup;
import com.jivesoftware.os.upena.deployable.okta.client.models.users.User;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Created by jonathan.colt on 11/7/16.
 */
public class OktaRealm extends AuthorizingRealm {

    public static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public OktaRealm() {
        super();
    }

    public String getName() {
        return "OktaRealm";
    }

    private Map<String, SimpleAccount> userAccount = new ConcurrentHashMap<>(16);

    /**
     * Simulates a call to an underlying data store - in a 'real' application, this call would communicate with
     * an underlying data store via an EIS API (JDBC, JPA, Hibernate, etc).
     * <p>
     * Note that when implementing your own realm, there is no need to check against a password (or other credentials)
     * in this method. The {@link org.apache.shiro.realm.AuthenticatingRealm AuthenticatingRealm} superclass will do
     * that automatically via the use of a configured
     * {@link org.apache.shiro.authc.credential.CredentialsMatcher CredentialsMatcher} (see this example's corresponding
     * {@code shiro.ini} file to see a configured credentials matcher).
     * <p>
     * All that is required is that the account information include directly the credentials found in the EIS.
     *
     * @param username the username for the account data to retrieve
     * @return the Account information corresponding to the specified username:
     */
    protected SimpleAccount getAccount(String username) {
        //just create a dummy.  A real app would construct one based on EIS access.

        SimpleAccount account = userAccount.computeIfAbsent(username, new Function<String, SimpleAccount>() {
            @Override
            public SimpleAccount apply(String s) {

                SimpleAccount account = new SimpleAccount(username, "password", getName());

                String baseUrl = System.getProperty("okta.base.url");
                if (baseUrl == null) {
                    return null;
                }
                String apiKey = System.getProperty("okta.api.key");
                if (baseUrl == null) {
                    return null;
                }
                ApiClientConfiguration apiClientConfiguration = new ApiClientConfiguration(baseUrl, apiKey);
                UserGroupApiClient userGroupApiClient = new UserGroupApiClient(apiClientConfiguration);


                try {

                    String groupId = null;
                    List<UserGroup> userGroupsWithLimit = userGroupApiClient.getUserGroupsWithQueryAndLimit("local-upena-readonly", 1);
                    for (UserGroup userGroup : userGroupsWithLimit) {
                        LOG.info(userGroup.toString());
                        if ("local-upena-readonly".equals(userGroup.getProfile().getName())) {
                            LOG.info(userGroup.getProfile().getName());
                            groupId = userGroup.getId();
                        }
                    }

                    if (groupId != null) {
                        List<User> users = userGroupApiClient.getUsers(groupId);
                        for (User user : users) {
                            LOG.info("user in group:" + user.getProfile().getLogin());
                        }
                    }


                } catch (IOException e) {
                    LOG.warn("Failed getting groups:", e);
                }


                account.addRole("admin");
                account.addRole("readwrite");
                account.addRole("readwrite");
                //account.addRole("user");
                //account.addRole("admin");

                account.addStringPermission("*");
                account.addStringPermission("read");
                account.addStringPermission("write");

                //account.addStringPermission("blogEntry:edit"); //this user is allowed to 'edit' _any_ blogEntry
                //account.addStringPermission("printer:print:laserjet2000"); //allowed to 'print' to the 'printer' identified
                return account;
            }
        });


        //LOG.info("getAccount " + account+" roles:"+account.getRoles()+" perms:"+account.getStringPermissions());


        return account;
    }


    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        //we can safely cast to a UsernamePasswordToken here, because this class 'supports' UsernamePasswordToken
        //objects.  See the Realm.supports() method if your application will use a different type of token.
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;


        //LOG.info("doGetAuthenticationInfo:" + token);


        return getAccount(upToken.getUsername());
    }

    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        //get the principal this realm cares about:
        String username = (String) getAvailablePrincipal(principals);

        //LOG.info("doGetAuthorizationInfo:" + principals);

        //call the underlying EIS for the account data:
        return getAccount(username);
    }
}
