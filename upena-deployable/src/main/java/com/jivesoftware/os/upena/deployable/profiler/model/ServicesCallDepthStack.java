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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author jonathan.colt
 */
public class ServicesCallDepthStack {

    private final ConcurrentHashMap<String, CallStack> depthStacks;

    public ServicesCallDepthStack() {
        this.depthStacks = new ConcurrentHashMap<>();
    }

    public CallStack callStackForServiceName(String serviceName) {
        return depthStacks.get(serviceName);
    }

    public List<Map<String, String>> getServiceNames() {
        List<Map<String, String>> serviceNames = new ArrayList<>();
        for (Map.Entry<String, CallStack> e : depthStacks.entrySet()) {
            Map<String, String> serviceProperties = new HashMap<>();
            serviceProperties.put("name", e.getKey());
            serviceProperties.put("enabled", String.valueOf(e.getValue().enabled.get()));
            serviceProperties.put("age", String.valueOf(System.currentTimeMillis() - e.getValue().lastSampleTimestampMillis.get()));
            serviceNames.add(serviceProperties);
        }

        Collections.sort(serviceNames, (Map<String, String> o1, Map<String, String> o2) -> {
            return o1.get("name").compareTo(o2.get("name"));
        });
        return serviceNames;
    }

    public boolean call(LatentSample latentSample) {
        String key = latentSample.clusterName + " " + latentSample.serviceName + " " + latentSample.serviceVersion;
        return getOrCreateCallStack(key).call(latentSample);
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
            got = new CallStack(new AtomicBoolean(true));
            CallStack had = depthStacks.putIfAbsent(key, got);
            if (had != null) {
                got = had;
            }
        }
        return got;
    }
}
