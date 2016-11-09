package com.jivesoftware.os.upena.deployable.okta.client.exceptions;

import java.io.IOException;

public class SdkException extends IOException {

    private String errorSummary;

    public SdkException(String errorSummary) {
        super(String.format("SdkException - errorSummary: %s", errorSummary));
    }

    public String getErrorSummary() {
        return this.errorSummary;
    }

}
