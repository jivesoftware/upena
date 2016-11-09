package com.jivesoftware.os.upena.deployable.okta.client.models.factors;


import com.jivesoftware.os.upena.deployable.okta.client.framework.ApiObject;

public class FactorCatalogEntry extends ApiObject {

    private String factorType;

    private String provider;

    /**
     * Gets factorType
     */
    public String getFactorType() {
        return this.factorType;
    }

    /**
     * Sets factorType
     */
    public void setFactorType(String val) {
        this.factorType = val;
    }

    /**
     * Gets provider
     */
    public String getProvider() {
        return this.provider;
    }

    /**
     * Sets provider
     */
    public void setProvider(String val) {
        this.provider = val;
    }
}