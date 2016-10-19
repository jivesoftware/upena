package com.jivesoftware.os.uba.shared;

/**
 *
 * @author jonathan.colt
 */
public interface PasswordStore {

    String password(String key) throws Exception;
}
