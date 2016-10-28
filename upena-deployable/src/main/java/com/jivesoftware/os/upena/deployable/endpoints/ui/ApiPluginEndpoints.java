package com.jivesoftware.os.upena.deployable.endpoints.ui;

import java.net.URI;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Singleton
@Path("/ui/api")
public class ApiPluginEndpoints {

    public ApiPluginEndpoints() {
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response api() {

        return Response.temporaryRedirect(URI.create("/static/vendor/swaggerui/")).build();
    }

}
