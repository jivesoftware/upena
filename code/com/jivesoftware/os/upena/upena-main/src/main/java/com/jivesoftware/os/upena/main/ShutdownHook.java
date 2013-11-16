package com.jivesoftware.os.upena.main;

import com.google.common.collect.ImmutableSet;
import com.jivesoftware.os.jive.utils.base.service.ServiceHandle;
import com.jivesoftware.os.server.http.jetty.jersey.server.RestfulManageServer;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

public class ShutdownHook {

    final ImmutableSet.Builder<ServiceHandle> handles = ImmutableSet.builder();
    final RestfulManageServer alreadyCovered;

    ShutdownHook(RestfulManageServer alreadyCovered) {
        this.alreadyCovered = alreadyCovered;
    }

    public void register(ServiceHandle... handles) {
        checkArgument(handles.length > 0, "What would you like to shut down?");
        for (ServiceHandle handle : handles) {
            checkArgument(handle != alreadyCovered, "I've got that one.");
        }
        this.handles.add(handles);
    }

    Set<ServiceHandle> handles()  {
        return handles.build();
    }
}
