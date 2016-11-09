package com.jivesoftware.os.upena.deployable.okta.client.exceptions;


import com.jivesoftware.os.upena.deployable.okta.client.framework.ErrorResponse;

public class RateLimitExceededException extends ApiException {

    public RateLimitExceededException(ErrorResponse errorResponse) {
        super(429, errorResponse);
    }

}
