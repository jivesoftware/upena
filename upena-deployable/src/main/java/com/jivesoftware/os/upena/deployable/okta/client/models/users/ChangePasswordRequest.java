package com.jivesoftware.os.upena.deployable.okta.client.models.users;

import com.jivesoftware.os.upena.deployable.okta.client.framework.ApiObject;

public class ChangePasswordRequest extends ApiObject {

    private Password oldPassword;

    private Password newPassword;

    /**
     * Gets oldPassword
     */
    public Password getOldPassword() {
        return this.oldPassword;
    }

    /**
     * Sets oldPassword
     */
    public void setOldPassword(Password val) {
        this.oldPassword = val;
    }

    /**
     * Gets newPassword
     */
    public Password getNewPassword() {
        return this.newPassword;
    }

    /**
     * Sets newPassword
     */
    public void setNewPassword(Password val) {
        this.newPassword = val;
    }
}