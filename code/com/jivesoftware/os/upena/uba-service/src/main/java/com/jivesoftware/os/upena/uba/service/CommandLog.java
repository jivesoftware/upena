package com.jivesoftware.os.upena.uba.service;

import java.util.List;

/**
 *
 * @author jonathan.colt
 */
public interface CommandLog {
    void log(String context, String message, Throwable t);

    void captured(String context, String message, Throwable t);

    void clear();

    List<String> copyLog();
}
