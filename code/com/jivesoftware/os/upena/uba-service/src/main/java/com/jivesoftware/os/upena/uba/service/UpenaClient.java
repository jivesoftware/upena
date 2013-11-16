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
