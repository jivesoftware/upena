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
public class Latent {

    private final ThreadLocal<LatentStack> latentStacks;
    private final String interfaceName;
    private final String className;
    private final String methodName;
    private long called;
    private long successlatency;
    private long failed;
    private long failedlatency;
    private final int hashCode;

    Latent(ThreadLocal<LatentStack> latentStacks, String interfaceName, String className, String methodName) {
        this.latentStacks = latentStacks;
        this.interfaceName = interfaceName;
        this.className = className;
        this.methodName = methodName;
        hashCode = interfaceName.hashCode() + className.hashCode() + methodName.hashCode();
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public long getCalled() {
        return called;
    }

    public long getSuccesslatency() {
        return successlatency;
    }

    public long getFailed() {
        return failed;
    }

    public long getFailedlatency() {
        return failedlatency;
    }

    void enter(String tracerId) {
        latentStacks.get().enter(this, tracerId);
    }

    public void exit() {
        called++;
        successlatency = latentStacks.get().exit(this, successlatency);
    }

    public void failed() {
        failed++;
        failedlatency = latentStacks.get().failed(this,failedlatency);
    }


    @Override
    public String toString() {
        return className + "." + methodName + " latency=" + Math.max(successlatency, failedlatency);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Latent other = (Latent) obj;
        if ((this.interfaceName == null) ? (other.interfaceName != null) : !this.interfaceName.equals(other.interfaceName)) {
            return false;
        }
        if ((this.className == null) ? (other.className != null) : !this.className.equals(other.className)) {
            return false;
        }
        if ((this.methodName == null) ? (other.methodName != null) : !this.methodName.equals(other.methodName)) {
            return false;
        }
        return true;
    }
}
