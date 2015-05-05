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

import com.jivesoftware.os.jive.utils.http.client.rest.RequestHelper;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptorsRequest;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptorsResponse;

public class UpenaClient {

    private final RequestHelper requestHelper;

    UpenaClient(RequestHelper requestHelper) {
        this.requestHelper = requestHelper;
    }

    public InstanceDescriptorsResponse instanceDescriptor(InstanceDescriptorsRequest instanceDescriptorsRequest) throws Exception {
        return requestHelper.executeRequest(instanceDescriptorsRequest, "/upena/request/instanceDescriptors", InstanceDescriptorsResponse.class, null);
    }
}
