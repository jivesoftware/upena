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
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import java.nio.charset.Charset;

public class ReleaseGroupKeyProvider implements UpenaKeyProvider<ReleaseGroupKey, ReleaseGroup> {

    private final JenkinsHash jenkinsHash = new JenkinsHash();
    private final Charset UTF8 = Charset.forName("utf-8");

    @Override
    public ReleaseGroupKey getNodeKey(UpenaTable<ReleaseGroupKey, ReleaseGroup> table, ReleaseGroup value) {
        String compositeKey = value.name + "|" + value.email;
            String k = Long.toString(Math.abs(jenkinsHash.hash(compositeKey.getBytes(UTF8), 4)));
            return new ReleaseGroupKey(k);
    }
}
