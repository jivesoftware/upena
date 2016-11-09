package com.jivesoftware.os.upena.deployable.okta.client.models.users;


import com.jivesoftware.os.upena.deployable.okta.client.framework.ApiObject;

public class ActivationResponse extends ApiObject {

    private String activationUrl;

    /**
     * Gets activationUrl
     */
    public String getActivationUrl() {
        return this.activationUrl;
    }

    /**
     * Sets activationUrl
     */
    public void setActivationUrl(String val) {
        this.activationUrl = val;
    }
}