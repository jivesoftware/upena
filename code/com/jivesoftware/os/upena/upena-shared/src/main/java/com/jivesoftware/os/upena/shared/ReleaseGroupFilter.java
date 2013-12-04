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
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class ReleaseGroupFilter implements KeyValueFilter<ReleaseGroupKey, ReleaseGroup>, Serializable {

    public final String name;
    public final String email;
    public final String version;
    public final String description;
    public final int start;
    public final int count;
    public int hit;

    @JsonCreator
    public ReleaseGroupFilter(@JsonProperty("name") String name,
            @JsonProperty("email") String email,
            @JsonProperty("version") String version,
            @JsonProperty("description") String description,
            @JsonProperty("start") int start,
            @JsonProperty("count") int count) {
        this.name = name;
        this.email = email;
        this.version = version;
        this.description = description;
        this.start = start;
        this.count = count;
    }

    @Override
    public String toString() {
        return "ReleaseGroupFilter{"
                + "name=" + name
                + ", email=" + email
                + ", version=" + version
                + ", description=" + description
                + ", start=" + start
                + ", count=" + count
                + ", hit=" + hit
                + '}';
    }

    @Override
    public ConcurrentNavigableMap<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> createCollector() {
        return new ReleaseGroupFilter.Results();
    }

    public static class Results extends ConcurrentSkipListMap<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> {
    }

    @Override
    public boolean filter(ReleaseGroupKey key, ReleaseGroup value) {
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
        if (version != null && value.version != null) {
            if (!value.version.contains(version)) {
                return false;
            }
        }
        if (description != null && value.description != null) {
            if (!value.description.contains(description)) {
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
