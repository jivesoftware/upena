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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class LatentGraph {

    private final Set<LatentEdge> graph = Collections.newSetFromMap(new ConcurrentHashMap<LatentEdge, Boolean>());

    public LatentGraph() {
    }

    public void record(Latent[] latents, String[] tracerIds, int depth) {
        LatentDepth latentDepth = new LatentDepth(latents[depth], depth);
        if (depth == 0) {
            graph.add(new LatentEdge(latentDepth, null));
        } else {
            graph.add(new LatentEdge(new LatentDepth(latents[depth - 1], depth - 1), latentDepth));
        }
    }

    public Boolean latentDepths(LatentDepthCallback latentDepthCallback) {
        for (LatentEdge edge : graph) {
            if (latentDepthCallback != null && edge != null) {
                LatentDepth from = edge.getFrom();
                LatentDepth to = edge.getTo();
                Boolean enabled = latentDepthCallback.calls(from, to);
                if (enabled != null && !enabled) {
                    return false;
                }
            }
        }
        return true;
    }

    public void clear() {
        graph.clear();
    }

    static class LatentEdge {

        private final LatentDepth from;
        private final LatentDepth to;

        public LatentEdge(LatentDepth from, LatentDepth to) {
            this.from = from;
            this.to = to;
        }

        public LatentDepth getFrom() {
            return from;
        }

        public LatentDepth getTo() {
            return to;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + (this.from != null ? this.from.hashCode() : 0);
            hash = 97 * hash + (this.to != null ? this.to.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LatentEdge other = (LatentEdge) obj;
            if (this.from != other.from && (this.from == null || !this.from.equals(other.from))) {
                return false;
            }
            if (this.to != other.to && (this.to == null || !this.to.equals(other.to))) {
                return false;
            }
            return true;
        }
    }
}
