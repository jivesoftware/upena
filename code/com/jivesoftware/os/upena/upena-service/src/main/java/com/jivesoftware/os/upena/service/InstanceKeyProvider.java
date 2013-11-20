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
package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.upena.service.UpenaTable.UpenaKeyProvider;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceKey;
import java.nio.charset.Charset;

public class InstanceKeyProvider implements UpenaKeyProvider<InstanceKey, Instance> {

    private final JenkinsHash jenkinsHash = new JenkinsHash();
    private final Charset UTF8 = Charset.forName("utf-8");

    @Override
    public InstanceKey getNodeKey(UpenaTable<InstanceKey, Instance> table, Instance value) {
        String compositeKey = value.hostKey + "|" + value.clusterKey + "|" + value.serviceKey + "|" + value.releaseGroupKey + "|" + value.instanceId;
        String k = Long.toString(Math.abs(jenkinsHash.hash(compositeKey.getBytes(UTF8), 5)));
        return new InstanceKey(k);
    }
}
