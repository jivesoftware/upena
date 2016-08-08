/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.upena.uba.service;

/**
 *
 */
public class ClusterServiceInstance implements Comparable<ClusterServiceInstance> {

    public String hostName;
    public String serviceName;
    public String instanceName;
    public int port;
    public int jmxPort;
    public int debuggingPort;

    @Override
    public int compareTo(ClusterServiceInstance other) {
        int c = this.hostName.compareTo(other.hostName);
        if (c != 0) {
            return c;
        }
        c = this.serviceName.compareTo(other.serviceName);
        if (c != 0) {
            return c;
        }
        c = this.instanceName.compareTo(other.instanceName);
        if (c != 0) {
            return c;
        }
        c = new Integer(port).compareTo(other.port);
        if (c != 0) {
            return c;
        }
        c = new Integer(jmxPort).compareTo(other.jmxPort);
        if (c != 0) {
            return c;
        }
        c = new Integer(debuggingPort).compareTo(other.debuggingPort);
        return c;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (this.hostName != null ? this.hostName.hashCode() : 0);
        hash = 59 * hash + (this.serviceName != null ? this.serviceName.hashCode() : 0);
        hash = 59 * hash + (this.instanceName != null ? this.instanceName.hashCode() : 0);
        hash = 59 * hash + this.port;
        hash = 59 * hash + this.jmxPort;
        hash = 59 * hash + this.debuggingPort;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ClusterServiceInstance other = (ClusterServiceInstance) obj;
        if ((this.hostName == null) ? (other.hostName != null) : !this.hostName.equals(other.hostName)) {
            return false;
        }
        if ((this.serviceName == null) ? (other.serviceName != null) : !this.serviceName.equals(other.serviceName)) {
            return false;
        }
        if ((this.instanceName == null) ? (other.instanceName != null) : !this.instanceName.equals(other.instanceName)) {
            return false;
        }
        if (this.port != other.port) {
            return false;
        }
        if (this.jmxPort != other.jmxPort) {
            return false;
        }
        if (this.debuggingPort != other.debuggingPort) {
            return false;
        }
        return true;
    }
}
