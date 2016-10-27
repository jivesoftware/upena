package com.jivesoftware.os.upena.deployable;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.net.URI;
import java.util.concurrent.Callable;
import javax.ws.rs.core.Response;
import org.apache.shiro.authz.AuthorizationException;

/**
 *
 * @author jonathan.colt
 */
public class ShiroRequestHelper {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public Response call(String name, Callable<Response> callable) {
        try {
            return callable.call();
        } catch (AuthorizationException a) {
            // TODO figure out who is call this.
            return Response.temporaryRedirect(URI.create("/ui/auth/unauthorized")).build();
        } catch (Exception e) {
            LOG.error("action GET", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
