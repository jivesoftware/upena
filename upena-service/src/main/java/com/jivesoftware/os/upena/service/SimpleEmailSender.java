package com.jivesoftware.os.upena.service;

/**
 *
 * @author jonathan.colt
 */
public interface SimpleEmailSender {

    void send(String recipient, String subject, String message) throws Exception;
}
