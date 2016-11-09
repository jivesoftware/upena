package com.jivesoftware.os.upena.deployable.okta.client.models.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.jivesoftware.os.upena.deployable.okta.client.framework.ApiObject;
import com.jivesoftware.os.upena.deployable.okta.client.models.links.LinksUnion;
import org.joda.time.DateTime;

import java.util.Map;

public class AuthResult extends ApiObject {

    public static class Status {
        public String PASSWORD_EXPIRED = "PASSWORD_EXPIRED";       //The user credentials are valid but expired; the user must change them
        public String RECOVERY = "RECOVERY";                       //The user is in the middle of the forgot-password flow
        public String PASSWORD_RESET = "PASSWORD_RESET";           //The user has answered their recovery question and needs to set a new password
        public String LOCKED_OUT = "LOCKED_OUT";                   //The user account is locked out; self-service unlock or admin unlock is required
        public String MFA_ENROLL = "MFA_ENROLL";                   //The user credentials are valid, but MFA is required and no factors are set up yet
        public String MFA_ENROLL_ACTIVATE = "MFA_ENROLL_ACTIVATE"; //User enrolled for MFA but needs to be activated using the activation code(s)
        public String MFA_REQUIRED = "MFA_REQUIRED";               //The user credentials are valid, but MFA is required
        public String MFA_CHALLENGE = "MFA_CHALLENGE";             //The MFA passCode has been sent, and we are waiting for the user to enter it back
        public String SUCCESS = "SUCCESS";                         //The user is authenticated
    }

    private String stateToken;

    private String status;

    private DateTime expiresAt;

    private String relayState;

    private String factorResult;

    private String factorResultMessage;

    private String recoveryToken;

    private String sessionToken;

    private String idToken;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @JsonProperty(value = "_links")
    private Map<String, LinksUnion> links;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @JsonProperty(value = "_embedded")
    private Map<String, Object> embedded;

    /**
     * Gets stateToken
     */
    public String getStateToken() {
        return this.stateToken;
    }

    /**
     * Sets stateToken
     */
    public void setStateToken(String val) {
        this.stateToken = val;
    }

    /**
     * Gets status
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * Sets status
     */
    public void setStatus(String val) {
        this.status = val;
    }

    /**
     * Gets expiresAt
     */
    public DateTime getExpiresAt() {
        return this.expiresAt;
    }

    /**
     * Sets expiresAt
     */
    public void setExpiresAt(DateTime val) {
        this.expiresAt = val;
    }

    /**
     * Gets relayState
     */
    public String getRelayState() {
        return this.relayState;
    }

    /**
     * Sets relayState
     */
    public void setRelayState(String val) {
        this.relayState = val;
    }

    /**
     * Gets factorResult
     */
    public String getFactorResult() {
        return this.factorResult;
    }

    /**
     * Sets factorResult
     */
    public void setFactorResult(String val) {
        this.factorResult = val;
    }

    /**
     * Gets factorResultMessage
     */
    public String getFactorResultMessage() {
        return this.factorResultMessage;
    }

    /**
     * Sets factorResultMessage
     */
    public void setFactorResultMessage(String val) {
        this.factorResultMessage = val;
    }

    /**
     * Gets recoveryToken
     */
    public String getRecoveryToken() {
        return this.recoveryToken;
    }

    /**
     * Sets recoveryToken
     */
    public void setRecoveryToken(String val) {
        this.recoveryToken = val;
    }

    /**
     * Gets sessionToken
     */
    public String getSessionToken() {
        return this.sessionToken;
    }

    /**
     * Sets sessionToken
     */
    public void setSessionToken(String val) {
        this.sessionToken = val;
    }

    /**
     * Gets idToken
     */
    public String getIdToken() {
        return this.idToken;
    }

    /**
     * Sets idToken
     */
    public void setIdToken(String val) {
        this.idToken = val;
    }

    /**
     * Gets links
     */
    public Map<String, LinksUnion> getLinks() {
        return this.links;
    }

    /**
     * Sets links
     */
    public void setLinks(Map<String, LinksUnion> val) {
        this.links = val;
    }

    /**
     * Gets embedded
     */
    public Map<String, Object> getEmbedded() {
        return this.embedded;
    }

    /**
     * Sets embedded
     */
    public void setEmbedded(Map<String, Object> val) {
        this.embedded = val;
    }
}