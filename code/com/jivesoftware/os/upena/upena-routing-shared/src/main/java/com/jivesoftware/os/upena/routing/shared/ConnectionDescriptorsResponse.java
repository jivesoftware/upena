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
package com.jivesoftware.os.upena.routing.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

public class ConnectionDescriptorsResponse {

    private final int returnCode;
    private final List<String> messages;
    private final String userId;
    private final List<ConnectionDescriptor> connections;

    @JsonCreator
    public ConnectionDescriptorsResponse(@JsonProperty("returnCode") int returnCode,
            @JsonProperty("messages") List<String> messages,
            @JsonProperty("userId") String userId,
            @JsonProperty("connections") List<ConnectionDescriptor> connections) {
        this.returnCode = returnCode;
        this.messages = messages;
        this.userId = userId;
        this.connections = connections;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public List<String> getMessages() {
        return messages;
    }

    public String getUserId() {
        return userId;
    }

    public List<ConnectionDescriptor> getConnections() {
        return connections;
    }

    @Override
    public String toString() {
        return "ConnectionDescriptorsResponse{"
                + "returnCode=" + returnCode
                + ", messages=" + messages
                + ", userId=" + userId
                + ", connections=" + connections
                + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + this.returnCode;
        hash = 89 * hash + Objects.hashCode(this.messages);
        hash = 89 * hash + Objects.hashCode(this.userId);
        hash = 89 * hash + Objects.hashCode(this.connections);
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
        final ConnectionDescriptorsResponse other = (ConnectionDescriptorsResponse) obj;
        if (this.returnCode != other.returnCode) {
            return false;
        }
        if (!Objects.equals(this.messages, other.messages)) {
            return false;
        }
        if (!Objects.equals(this.userId, other.userId)) {
            return false;
        }
        if (!Objects.equals(this.connections, other.connections)) {
            return false;
        }
        return true;
    }
}
