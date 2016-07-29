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

@JsonIgnoreProperties(ignoreUnknown = true)
public enum ChaosStrategyKey {

    RANDOMIZE_PORT("1", "Randomize port"),
    RANDOMIZE_HOSTNAME("2", "Randomize host name");

    public final String key;
    public final String name;

    @JsonCreator
    ChaosStrategyKey(@JsonProperty("key") String key,
                     @JsonProperty("name") String name) {
        this.key = key;
        this.name = name;
    }

    public static boolean isValid(String key) {
        if (key == null || key.isEmpty())
            return false;

        for (ChaosStrategyKey v : values()) {
            if (v.key.equals(key)) {
                return true;
            }
        }

        return false;
    }

    public static ChaosStrategyKey valueOfKey(String key) {
        for (ChaosStrategyKey v : values()) {
            if (v.key.equals(key)) {
                return v;
            }
        }
        throw new IllegalArgumentException("No enum const " + ChaosStrategyKey.class + "@key." + key);
    }

    @Override
    public String toString() {
        return "ChaosStrategyKey{" +
                "key=" + key +
                ", name='" + name + '\'' +
                '}';
    }
}
