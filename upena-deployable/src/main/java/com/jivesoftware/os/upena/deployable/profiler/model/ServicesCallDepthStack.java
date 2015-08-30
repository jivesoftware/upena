/*
 * Copyright 2013 jonathan.colt.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jivesoftware.os.upena.deployable.profiler.model;

import com.jivesoftware.os.upena.deployable.profiler.sample.LatentSample;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author jonathan.colt
 */
public class ServicesCallDepthStack {

    private final ConcurrentHashMap<String, CallStack> depthStacks;

    public ServicesCallDepthStack() {
        this.depthStacks = new ConcurrentHashMap<>();
    }

    public List<String> getServiceNames() {
        LinkedList<String> serviceNames = new LinkedList<>(depthStacks.keySet());
        Collections.sort(serviceNames);
        return serviceNames;
    }

    public void call(LatentSample latentSample) {
        String key = latentSample.clusterName + " " + latentSample.serviceName + " " + latentSample.serviceVersion;
        getOrCreateCallStack(key).call(latentSample);
    }

    public CallDepth[] getCopy(String serviceName) {
        if (serviceName == null) {
            return new CallDepth[0];
        }
        return depthStacks.get(serviceName).getCopy();
    }

    private CallStack getOrCreateCallStack(String key) {
        CallStack got = depthStacks.get(key);
        if (got == null) {
            got = new CallStack();
            CallStack had = depthStacks.putIfAbsent(key, got);
            if (had != null) {
                got = had;
            }
        }
        return got;
    }
}
