package com.jivesoftware.os.upena.main;

import com.jivesoftware.os.server.http.health.check.FatalHealthCheckResponse;
import com.jivesoftware.os.server.http.health.check.HealthCheck;
import com.jivesoftware.os.server.http.health.check.HealthCheckResponse;
import com.jivesoftware.os.server.http.health.check.HealthCheckResponseImpl;

public class ComponentHealthCheck implements HealthCheck {

    private final String name;
    private boolean healthy = false;
    private String message = "health hasn't been established.";
    private final boolean fatal;

    public ComponentHealthCheck(String name) {
        this.name = name;
        this.fatal = false;
    }

    public ComponentHealthCheck(String name, boolean fatal) {
        this.name = name;
        this.fatal = fatal;
    }

    @Override
    public HealthCheckResponse checkHealth() {
        return fatal ? new FatalHealthCheckResponse(name, healthy, message) : new HealthCheckResponseImpl(name, healthy, message);
    }

    public void setMessage(String messsage) {
        this.message = messsage;
    }

    void setHealthy(String messsage) {
        healthy = true;
        this.message = messsage;
    }

    void setUnhealthy(String message, Throwable cause) {
        healthy = false;
        StringBuilder sb = new StringBuilder();
        sb.append(message).append("\n");
        if (cause != null) {
            sb.append(cause.getMessage()).append("\n");
            for (StackTraceElement e : cause.getStackTrace()) {
                sb.append(e.toString()).append("\n");
            }
        }
        this.message = sb.toString();
    }
}