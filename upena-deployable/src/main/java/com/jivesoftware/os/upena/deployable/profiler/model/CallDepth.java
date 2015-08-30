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

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author jonathan.colt
 */
public class CallDepth {

    private final ConcurrentHashMap<String, CallClass> classNameToCallClass;

    public CallDepth() {
        this.classNameToCallClass = new ConcurrentHashMap<>();
    }

    public CallClass getOrCreateCallClass(String className) {
        CallClass node = classNameToCallClass.get(className);
        if (node == null) {
            node = new CallClass(className);
            CallClass had = classNameToCallClass.putIfAbsent(className, node);
            if (had != null) {
                return had;
            }
        }
        return node;
    }

    public CallClass[] getCopy() {
        return classNameToCallClass.values().toArray(new CallClass[0]);
    }
}
