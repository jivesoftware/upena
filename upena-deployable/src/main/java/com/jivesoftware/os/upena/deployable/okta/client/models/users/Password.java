package com.jivesoftware.os.upena.deployable.okta.client.models.users;

import com.jivesoftware.os.upena.deployable.okta.client.framework.ApiObject;

public class Password extends ApiObject {

    private String value;

    /**
     * Gets value
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Sets value
     */
    public void setValue(String val) {
        this.value = val;
    }
}