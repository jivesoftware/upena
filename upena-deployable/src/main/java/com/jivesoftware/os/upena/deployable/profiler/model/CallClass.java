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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author jonathan.colt
 */
public class CallClass {

    private final String name;
    private final Set<Call> calls;
    private final ConcurrentHashMap<String, ClassMethod> classMethods;

    public CallClass(String className) {
        this.classMethods = new ConcurrentHashMap<>();
        this.calls = new HashSet<>();
        this.name = (className == null) ? "unnamedClass" : className;
    }

    public String getName() {
        return name;
    }

    public Set<Call> getCalls() {
        return calls;
    }

    public ConcurrentHashMap<String, ClassMethod> getClassMethods() {
        return classMethods;
    }

    public void calls(String className, String methodName) {
        calls.add(new Call(className, methodName));
    }

    public ClassMethod getOrCreateClassMethod(String methodName) {
        ClassMethod classMethod = classMethods.get(methodName);
        if (classMethod == null) {
            classMethod = new ClassMethod(methodName);
            ClassMethod had = classMethods.putIfAbsent(methodName, classMethod);
            if (had != null) {
                return had;
            }
        }
        return classMethod;
    }

    public Set<String> getMethods() {
        return classMethods.keySet();
    }

    public ClassMethod getMethod(String methodName) {
        return classMethods.get(methodName);
    }

    public long getCalled() {
        long v = 0;
        for (ClassMethod classMethod : classMethods.values()) {
            v += classMethod.called;
        }
        return v;
    }

    public long getSuccesslatency() {
        long v = 0;
        for (ClassMethod classMethod : classMethods.values()) {
            v += classMethod.successlatency;
        }
        return v;
    }

    public long getFailed() {
        long v = 0;
        for (ClassMethod classMethod : classMethods.values()) {
            v += classMethod.failed;
        }
        return v;
    }

    public long getFailedlatency() {
        long v = 0;
        for (ClassMethod classMethod : classMethods.values()) {
            v += classMethod.failedlatency;
        }
        return v;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 11 * hash + (this.name != null ? this.name.hashCode() : 0);
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
        final CallClass other = (CallClass) obj;
        if (this.name != other.name && (this.name == null || !this.name.equals(other.name))) {
            return false;
        }
        return true;
    }
}
