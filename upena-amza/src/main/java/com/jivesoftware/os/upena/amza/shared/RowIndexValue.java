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
package com.jivesoftware.os.upena.amza.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Arrays;

public class RowIndexValue implements Serializable {

    private final long timestamp;
    private final boolean tombstoned;
    private final byte[] value;

    @JsonCreator
    public RowIndexValue(
            @JsonProperty("value") byte[] value,
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("tombstoned") boolean tombstoned) {
        this.timestamp = timestamp;
        this.value = value;
        this.tombstoned = tombstoned;
    }

    public long getTimestampId() {
        return timestamp;
    }

    public boolean getTombstoned() {
        return tombstoned;
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "RowIndexValue{" + "timestamp=" + timestamp + ", tombstoned=" + tombstoned + ", value=" + value + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (int) (this.timestamp ^ (this.timestamp >>> 32));
        hash = 97 * hash + (this.tombstoned ? 1 : 0);
        hash = 97 * hash + Arrays.hashCode(this.value);
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
        final RowIndexValue other = (RowIndexValue) obj;
        if (this.timestamp != other.timestamp) {
            return false;
        }
        if (this.tombstoned != other.tombstoned) {
            return false;
        }
        if (!Arrays.equals(this.value, other.value)) {
            return false;
        }
        return true;
    }

}
