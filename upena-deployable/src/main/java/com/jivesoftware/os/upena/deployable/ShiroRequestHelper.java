package com.jivesoftware.os.upena.deployable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.math.BigInteger;
import java.net.URI;
import java.security.SecureRandom;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import org.apache.shiro.authz.AuthorizationException;

/**
 *
 * @author jonathan.colt
 */
public class ShiroRequestHelper {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final long csrfTokenMaxAgeMillis;
    private final Cache<String, Long> csrfTokens = CacheBuilder.newBuilder().concurrencyLevel(32).maximumSize(1000).build();

    public ShiroRequestHelper(long csrfTokenMaxAgeMillis) {
        this.csrfTokenMaxAgeMillis = csrfTokenMaxAgeMillis;
    }

    public Response csrfCall(String csrfToken, String name, CsrfCallable callable) {
        try {
            Long expiration = null;
            if (csrfToken != null) {
                expiration = csrfTokens.asMap().remove(csrfToken);
            }

            if (expiration == null || expiration < System.currentTimeMillis()) {
                return HeaderDecoration.decorate(Response.status(Status.FORBIDDEN)).build();
            }

            ResponseBuilder builder = callable.call(generateToken());
            builder = HeaderDecoration.decorate(builder);
            return builder.build();
        } catch (AuthorizationException a) {
            // TODO figure out who is call this.
            return Response.temporaryRedirect(URI.create("/ui/auth/unauthorized")).build();
        } catch (Exception e) {
            LOG.error("action GET", e);
            return Response.serverError().build();
        }
    }


    public Response call(String name, CsrfCallable callable) {
        try {
            ResponseBuilder builder = callable.call(generateToken());
            HeaderDecoration.decorate(builder);
            return builder.build();
        } catch (AuthorizationException a) {
            // TODO figure out who is call this.
            return Response.temporaryRedirect(URI.create("/ui/auth/unauthorized")).build();
        } catch (Exception e) {
            LOG.error("action GET", e);
            return Response.serverError().build();
        }
    }



    private String generateToken() {
        SecureRandom random = new SecureRandom();
        String token = new BigInteger(130, random).toString(32);
        csrfTokens.put(token, System.currentTimeMillis() + csrfTokenMaxAgeMillis);
        return token;
    }

    public interface CsrfCallable {
        ResponseBuilder call(String csrfToken) throws Exception;
    }

}
