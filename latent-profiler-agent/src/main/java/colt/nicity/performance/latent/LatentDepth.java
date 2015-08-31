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
 * @author jonathan.colt
 */
public class LatentDepth {
    private final Latent latent;
    private final int depth;

    public LatentDepth(Latent latent, int depth) {
        this.latent = latent;
        this.depth = depth;
    }

    public Latent getLatent() {
        return latent;
    }

    public int getDepth() {
        return depth;
    }
    

    @Override
    public String toString() {
        return "LatentDepth{" + "latent=" + latent + ", depth=" + depth + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.latent != null ? this.latent.hashCode() : 0);
        hash = 59 * hash + this.depth;
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
        final LatentDepth other = (LatentDepth) obj;
        if (this.latent != other.latent && (this.latent == null || !this.latent.equals(other.latent))) {
            return false;
        }
        if (this.depth != other.depth) {
            return false;
        }
        return true;
    }
    
}
