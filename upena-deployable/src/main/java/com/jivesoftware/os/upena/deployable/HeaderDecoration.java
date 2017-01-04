package com.jivesoftware.os.upena.deployable;

import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * Created by jonathan.colt on 1/4/17.
 */
public class HeaderDecoration {

    public static ResponseBuilder decorate(ResponseBuilder responseBuilder) {
        responseBuilder.header("X-FRAME-OPTIONS", "DENY");
        responseBuilder.header("X-FRAME-OPTIONS", "DENY");
        //responseBuilder.header("Strict-Transport-Security","max-age=86400; includeSubDomains");
        return responseBuilder;
    }
}
