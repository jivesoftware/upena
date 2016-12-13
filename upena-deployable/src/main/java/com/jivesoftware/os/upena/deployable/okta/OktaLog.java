package com.jivesoftware.os.upena.deployable.okta;

/**
 * Created by jonathan.colt on 12/13/16.
 */
public interface OktaLog {
    void record(String who, String what, String why, String how);
}
