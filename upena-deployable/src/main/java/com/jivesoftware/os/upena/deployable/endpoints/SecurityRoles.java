package com.jivesoftware.os.upena.deployable.endpoints;

/**
 *
 * @author jonathan.colt
 */
public class SecurityRoles {

    public static final String admin = "admin";
    public static final String r = "readOnly";
    public static final String rw = "readWrite";

    public static String cluster(String clusterName, String role) {
        return "c." + clusterName + "." + role;
    }

    public static String cluster(String clusterName, String service, String role) {
        return "c." + clusterName + ".s." + service + "." + role;
    }

}
