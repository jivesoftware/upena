/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.upena.deployable.profiler.model;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class ClassMethod {

    private final String methodName;
    long called;
    long successlatency;
    long failed;
    long failedlatency;
    private final Set<Integer> stackDepths;

    public ClassMethod(String methodName) {
        this.stackDepths = new HashSet<>();
        this.methodName = methodName;
    }

    final void update(int stackDepth, long called, long successlatency, long failed, long failedlatency) {
        this.stackDepths.add(stackDepth);
        this.called = called;
        this.successlatency = successlatency;
        this.failed = failed;
        this.failedlatency = failedlatency;
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

    public Set<Integer> getStackDepths() {
        return stackDepths;
    }

}
