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
package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserFilter implements KeyValueFilter<UserKey, User>, Serializable {

    public final String name;
    public final String email;
    public final int start;
    public final int count;
    public int hit;

    @JsonCreator
    public UserFilter(@JsonProperty("name") String name,
        @JsonProperty("email") String email,
        @JsonProperty("start") int start,
        @JsonProperty("count") int count) {
        this.name = name;
        this.email = email;
        this.start = start;
        this.count = count;
    }

    @Override
    public String toString() {
        return "ClusterFilter{"
            + "name=" + name
            + ", email=" + email
            + ", start=" + start
            + ", count=" + count
            + '}';
    }

    @Override
    public ConcurrentNavigableMap<UserKey, TimestampedValue<User>> createCollector() {
        return new Results();
    }

    public static class Results extends ConcurrentSkipListMap<UserKey, TimestampedValue<User>> {
    }

    @Override
    public boolean filter(UserKey key, User value) {
        if (name != null && value.name != null) {
            if (!value.name.contains(name)) {
                return false;
            }
        }
        if (email != null && value.email != null) {
            if (!value.email.contains(email)) {
                return false;
            }
        }
        hit++;
        if (hit < start) {
            return false;
        }
        if (hit > start + count) {
            return false;
        }
        return true;
    }

    @Override
    public void reset() {
        hit = 0;
    }
}
