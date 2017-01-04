package com.jivesoftware.os.upena.deployable;

import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * Created by jonathan.colt on 1/4/17.
 */
public class HeaderDecoration {

    private static String hstsma = "max-age=" + System.getProperty("http.strict.transport.security.max.age", "0") + "; includeSubDomains";

    public static ResponseBuilder decorate(ResponseBuilder responseBuilder) {
        responseBuilder.header("X-FRAME-OPTIONS", "DENY");
        responseBuilder.header("Strict-Transport-Security", hstsma);
        return responseBuilder;
    }
}
