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
package com.jivesoftware.os.uba.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DeployableUpload {

    public final List<String> instanceIds;
    public final String version;
    public final byte[] deployableFileBytes;
    public final String extension;

    @JsonCreator
    public DeployableUpload(@JsonProperty("instanceIds") List<String> instanceIds,
            @JsonProperty("version") String version,
            @JsonProperty("deployableFileBytes") byte[] deployableFileBytes,
            @JsonProperty("extension") String extension) {
        this.instanceIds = instanceIds;
        this.version = version;
        this.deployableFileBytes = deployableFileBytes;
        this.extension = extension;
    }

    @Override
    public String toString() {
        return "DeployableUpload{"
                + "instanceIds=" + instanceIds
                + ", version=" + version
                + ", deployableFileBytes=" + deployableFileBytes
                + ", extension=" + extension
                + '}';
    }

}