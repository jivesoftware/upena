package com.jivesoftware.os.upena.deployable.okta;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

/**
 * Created by jonathan.colt on 11/7/16.
 */
public class OktaRealm extends AuthorizingRealm {

    public static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    public static OktaLog oktaLog;

    public OktaRealm() {
        super();
    }

    public String getName() {
        return "OktaRealm";
    }

    protected SimpleAccount getAccount(String username) {

        SimpleAccount account1 = new SimpleAccount(username, "password", getName());

        String userRolesDirectory = System.getProperty("okta.user.roles.directory");
        if (userRolesDirectory == null) {
            return null;
        }

        File roles = new File(userRolesDirectory, username);
        if (roles.exists()) {
            try {
                List<String> lines = Files.readAllLines(roles.toPath());
                for (String line : lines) {
                    if (line.trim().startsWith("#")) {
                        continue;
                    }
                    if (line.trim().toLowerCase().startsWith("role") && line.contains(":")) {
                        String role = line.trim().split(":")[1].trim();
                        account1.addRole(role);
                    }

                    if (line.trim().toLowerCase().startsWith("perm") && line.contains(":")) {
                        String perm = line.trim().split(":")[1].trim();
                        account1.addStringPermission(perm);
                    }
                }

            } catch (Exception e) {
                LOG.error("Failed parsing {}", new Object[] { roles }, e);
            }
        } else {
            account1.addStringPermission("read");
        }

        return account1;
    }


    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        //we can safely cast to a UsernamePasswordToken here, because this class 'supports' UsernamePasswordToken
        //objects.  See the Realm.supports() method if your application will use a different type of token.
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        return getAccount(upToken.getUsername());
    }

    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        //get the principal this realm cares about:
        String username = (String) getAvailablePrincipal(principals);
        //call the underlying EIS for the account data:
        return getAccount(username);
    }
}
