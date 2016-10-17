package com.jivesoftware.os.upena.shared;

/**
 *
 * @author jonathan.colt
 */
public enum ReleaseGroupPropertyKey {
    matcher(null, "200-299"),
    protocol(null, "HTTP"),
    healthCheckPath(null, "/health/check"),
    healthCheckProtocol(null, "HTTP"),
    healthCheckTimeoutSeconds(null, "5"),
    healthyThresholdCount(null, "5"),
    unhealthyThresholdCount(null, "2"),
    healthCheckIntervalSeconds(null, "30"),
    deregistrationDelayTimeoutSeconds("deregistration_delay.timeout_seconds", "300"),
    stickinessEnabled("stickiness.enabled", "false"),
    stickinessType("stickiness.type", "lb_cookie"),
    stickinessLBCookieDurationSeconds("stickiness.lb_cookie.duration_seconds", "86400"),
    loadBalanced("load.balanced", "false");

    private final String key;
    private final String defaultValue;

    private ReleaseGroupPropertyKey(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public static ReleaseGroupPropertyKey forKey(String key) {
        if (key == null) {
            return null;
        }
        for (ReleaseGroupPropertyKey releaseGroupPropertyKey : values()) {
            if (releaseGroupPropertyKey.key().equals(key)) {
                return releaseGroupPropertyKey;
            }
        }
        return null;
    }

    public String key() {
        if (key != null) {
            return key;
        }
        return name();
    }

    public String getDefaultValue() {
        return defaultValue;
    }

}
