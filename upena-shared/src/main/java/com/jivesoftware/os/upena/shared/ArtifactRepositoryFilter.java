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
public class ArtifactRepositoryFilter implements KeyValueFilter<ArtifactRepositoryKey, ArtifactRepository>, Serializable {

    public final String name;
    public final String description;
    public final String url;
    public final int start;
    public final int count;
    public int hit;

    @JsonCreator
    public ArtifactRepositoryFilter(@JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("url") String url,
        @JsonProperty("start") int start,
        @JsonProperty("count") int count) {
        this.name = name;
        this.description = description;
        this.url = url;
        this.start = start;
        this.count = count;
    }

    @Override
    public String toString() {
        return "ArtifactRepositoryFilter{" + "name=" + name + ", description=" + description + ", url=" + url + ", start=" + start + ", count=" + count + ", hit=" + hit + '}';
    }

    @Override
    public ConcurrentNavigableMap<ArtifactRepositoryKey, TimestampedValue<ArtifactRepository>> createCollector() {
        return new ArtifactRepositoryFilter.Results();
    }

    public static class Results extends ConcurrentSkipListMap<ArtifactRepositoryKey, TimestampedValue<ArtifactRepository>> {
    }

    @Override
    public boolean filter(ArtifactRepositoryKey key, ArtifactRepository value) {
        if (name != null && value.name != null) {
            if (!value.name.contains(name)) {
                return false;
            }
        }
        if (description != null && value.description != null) {
            if (!value.description.contains(description)) {
                return false;
            }
        }
        if (url != null && value.url != null) {
            if (!value.url.contains(url)) {
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
