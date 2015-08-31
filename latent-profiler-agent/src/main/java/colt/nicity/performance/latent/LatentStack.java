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

/**
 *
 */
public class LatentStack {

    private final LatentGraph latentGraph;
    private final Latent[] stack;
    private final String[] tracerIds;
    private final long[] enters;
    private int depth;

    public LatentStack(LatentGraph latentGraph, int maxDepth) {
        this.latentGraph = latentGraph;
        stack = new Latent[maxDepth];
        tracerIds = new String[maxDepth];
        enters = new long[maxDepth];
        depth = 0;
    }

    public void enter(Latent latent, String tracerId) {
        enters[depth] = System.nanoTime();
        stack[depth] = latent;
        tracerIds[depth] = tracerId;
        depth++;
    }

    public long exit(Latent latent, long latency) {
        depth--;
        latency = sample(latency, enters[depth]);
        latentGraph.record(stack, tracerIds, depth);
        if (stack[depth] == latent) {
            stack[depth] = null;
        } else {
            System.out.println("Should be impossible. Likely some one forgot to us try {} finally {}");
        }
        return latency;
    }

    public long failed(Latent latent, long latency) {
        return exit(latent,latency);
    }

    public long failed(Latent latent,long latency, Throwable t) {
        return exit(latent,latency);
    }
    
    private long sample(long latency,long entered) {
        if (latency > 0) {
            latency /= 2;
        } // decay
        long elapse = System.nanoTime() - entered;
        latency += elapse;
        return latency;
    }
}
