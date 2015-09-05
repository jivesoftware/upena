/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.upena.deployable.profiler.model;

import com.jivesoftware.os.upena.deployable.profiler.sample.LatentSample;
import com.jivesoftware.os.upena.deployable.profiler.sample.LatentSample.LatentNode;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class CallStack {

    private final ConcurrentHashMap<Integer, CallDepth> callsAtDepth;
    public final AtomicBoolean enabled;
    public final AtomicLong lastSampleTimestampMillis = new AtomicLong();

    public CallStack(AtomicBoolean enabled) {
        this.callsAtDepth = new ConcurrentHashMap<>();
        this.enabled = enabled;
    }

    public boolean call(LatentSample latentSample) {
        lastSampleTimestampMillis.set(System.currentTimeMillis());
        if (enabled.get()) {
            LatentNode from = latentSample.from;
            LatentNode to = latentSample.to;

            CallDepth fromDepth = getCallDepth(from.stackDepth);
            CallClass fromClass = fromDepth.getOrCreateCallClass(getClassName(from));
            ClassMethod fromMethod = fromClass.getOrCreateClassMethod(from.methodName);
            fromMethod.update(from.stackDepth,
                from.callCount,
                from.callLatency,
                from.failedCount,
                from.failedLatency);

            if (to != null) {
                fromClass.calls(getClassName(to), to.methodName);

                CallDepth toDepth = getCallDepth(to.stackDepth);
                CallClass toClass = toDepth.getOrCreateCallClass(getClassName(to));
                ClassMethod toMethod = toClass.getOrCreateClassMethod(to.methodName);
                toMethod.update(to.stackDepth,
                    to.callCount,
                    to.callLatency,
                    to.failedCount,
                    to.failedLatency);
            }
            return true;
        } else {
            return false;
        }
    }

    private String getClassName(LatentNode node) {
        return node.interfaceName;
    }

    private CallDepth getCallDepth(int depth) {
        CallDepth got = callsAtDepth.get(depth);
        if (got == null) {
            got = new CallDepth();
            CallDepth had = callsAtDepth.putIfAbsent(depth, got);
            if (had != null) {
                return had;
            }
        }
        return got;
    }

    CallDepth[] getCopy() {
        List<Integer> keySet = new LinkedList<>(callsAtDepth.keySet());
        Collections.sort(keySet);
        CallDepth[] callDepths = new CallDepth[keySet.size()];
        for (int i = 0; i < callDepths.length; i++) {
            callDepths[i] = callsAtDepth.get(keySet.get(i));
        }
        return callDepths;
    }
}
