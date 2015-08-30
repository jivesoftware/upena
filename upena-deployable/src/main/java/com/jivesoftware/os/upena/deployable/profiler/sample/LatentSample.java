/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.upena.deployable.profiler.sample;

/**
 *
 */
public class LatentSample {

    public String clusterName;
    public String serviceName;
    public String serviceVersion;
    public long sampleTimestampEpochMillis;
    public LatentNode from;
    public LatentNode to;

    public static class LatentNode {

        public int stackDepth;
        public String interfaceName;
        public String className;
        public String methodName;
        public long failedCount;
        public long failedLatency;
        public long callCount;
        public long callLatency;

        @Override
        public String toString() {
            return "LatentNode{"
                + "stackDepth=" + stackDepth
                + ", interfaceName=" + interfaceName
                + ", className=" + className
                + ", methodName=" + methodName
                + ", failedCount=" + failedCount
                + ", failedLatency=" + failedLatency
                + ", callCount=" + callCount
                + ", callLatency=" + callLatency
                + '}';
        }
    }

    @Override
    public String toString() {
        return "LatentSample{"
            + "clusterName=" + clusterName
            + ", serviceName=" + serviceName
            + ", serviceVersion=" + serviceVersion
            + ", sampleTimestampEpochMillis=" + sampleTimestampEpochMillis
            + ", from=" + from
            + ", to=" + to
            + '}';
    }

}
