package com.jivesoftware.os.upena.deployable.okta.client.models.users;

import com.jivesoftware.os.upena.deployable.okta.client.framework.ApiObject;

public class TempPassword extends ApiObject {

    private String tempPassword;

    /**
     * Gets tempPassword
     */
    public String getTempPassword() {
        return this.tempPassword;
    }

    /**
     * Sets tempPassword
     */
    public void setTempPassword(String val) {
        this.tempPassword = val;
    }
}