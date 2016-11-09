package com.jivesoftware.os.upena.deployable.okta.client.models.users;

import com.jivesoftware.os.upena.deployable.okta.client.framework.ApiObject;

public class Provider extends ApiObject {

    private String type;

    private String name;

    /**
     * Gets type
     */
    public String getType() {
        return this.type;
    }

    /**
     * Sets type
     */
    public void setType(String val) {
        this.type = val;
    }

    /**
     * Gets name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets name
     */
    public void setName(String val) {
        this.name = val;
    }
}