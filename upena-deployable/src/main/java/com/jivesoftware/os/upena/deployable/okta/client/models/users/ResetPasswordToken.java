package com.jivesoftware.os.upena.deployable.okta.client.models.users;

import com.jivesoftware.os.upena.deployable.okta.client.framework.ApiObject;

public class ResetPasswordToken extends ApiObject {

    private String resetPasswordUrl;

    /**
     * Gets resetPasswordUrl
     */
    public String getResetPasswordUrl() {
        return this.resetPasswordUrl;
    }

    /**
     * Sets resetPasswordUrl
     */
    public void setResetPasswordUrl(String val) {
        this.resetPasswordUrl = val;
    }
}