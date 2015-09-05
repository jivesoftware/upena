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
package colt.nicity.performance.latent;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class Latency {

    private static Latency latency;

    synchronized public static Latency singleton() {
        if (latency == null) {
            LatentGraph latentGraph = new LatentGraph();
            latency = new Latency(latentGraph, 1024);
        }
        return latency;
    }
    private final ThreadLocal<LatentStack> latentStacks;
    private final LatentGraph latentGraph;
    private final Enabled enable = new Enabled(true);

    private Latency(final LatentGraph latentGraph, final int maxDepth) {
        this.latentGraph = latentGraph;

        latentStacks = new ThreadLocal<LatentStack>() {
            @Override
            protected LatentStack initialValue() {
                return new LatentStack(latentGraph, maxDepth);
            }
        };
    }

    public LatentGraph getLatentGraph(boolean enabled) {
        if (!this.enable.enabled != enabled) {
            singleton.clear();
        }
        this.enable.enabled = enabled;
        return latentGraph;
    }

    private ConcurrentHashMap<String, Latent> singleton = new ConcurrentHashMap<>();
    public Latent enter(Latent latent, String interfaceName, String className, String methodName, String tracerId) {
        if (latent == null) {
            String key = className + "." + methodName;
            Latent got = singleton.get(key);
            if (got == null) {
                got = new Latent(enable, latentStacks, interfaceName, className, methodName);
                Latent had = singleton.putIfAbsent(key, got);
                if (had != null) {
                    got = had;
                }
            }
            latent = got;
        }
        latent.enter(tracerId);
        return latent;
    }

    public static class Enabled {

        public boolean enabled;

        public Enabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
