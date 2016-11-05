/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.upena.deployable.profiler.visualize;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class VStrategies {

    private static final NameUtils nameUtils = new NameUtils();

    public static enum ValueStrat {

        totaltime(new ValueStrategy() {

            @Override
            public long value(VisualizeProfile.InterfaceArea callArea) {
                return TimeUnit.MILLISECONDS.toNanos(1)
                    + (Math.max(callArea.callClass.getSuccesslatency(), callArea.callClass.getFailedlatency())
                    * callArea.callClass.getCalled());
            }

            @Override
            public String name(long value) {
                return value + "millis";
            }
        }),
        latency(new ValueStrategy() {

            @Override
            public long value(VisualizeProfile.InterfaceArea callArea) {
                return TimeUnit.MILLISECONDS.toNanos(1) + Math.max(callArea.callClass.getSuccesslatency(), callArea.callClass.getFailedlatency());
            }

            @Override
            public String name(long value) {
                return nameUtils.latencyString(value);
            }
        }),
        called(new ValueStrategy() {

            @Override
            public long value(VisualizeProfile.InterfaceArea callArea) {
                return Math.max(callArea.callClass.getCalled(), callArea.callClass.getFailed());
            }

            @Override
            public String name(long value) {
                return value + " called";
            }
        }),
        calledBy(new ValueStrategy() {

            @Override
            public long value(VisualizeProfile.InterfaceArea callArea) {
                return callArea.calledByCount;
            }

            @Override
            public String name(long value) {
                return value + " calledByCount";
            }
        }),
        callsTo(new ValueStrategy() {

            @Override
            public long value(VisualizeProfile.InterfaceArea callArea) {
                return callArea.callsTo.size();
            }

            @Override
            public String name(long value) {
                return value + " callsTo";
            }
        }),
        callComplexity(new ValueStrategy() {

            @Override
            public long value(VisualizeProfile.InterfaceArea callArea) {
                return callArea.calledByCount * callArea.callsTo.size();
            }

            @Override
            public String name(long value) {
                return value + " callComplexity";
            }
        }),
        constant(new ValueStrategy() {

            @Override
            public long value(VisualizeProfile.InterfaceArea callArea) {
                return 1;
            }

            @Override
            public String name(long value) {
                return "";
            }
        });

        final ValueStrategy strategy;

        private ValueStrat(ValueStrategy valueStrategy) {
            this.strategy = valueStrategy;
        }

        public ValueStrategy getStrategy() {
            return strategy;
        }

    }

    public static enum StackStrat {

        totaltime(new ValueStrategy() {

            @Override
            public long value(VisualizeProfile.InterfaceArea callArea) {
                return TimeUnit.MILLISECONDS.toNanos(1)
                    + (Math.max(callArea.callClass.getSuccesslatency(), callArea.callClass.getFailedlatency())
                    * callArea.callClass.getCalled());
            }

            @Override
            public String name(long value) {
                return nameUtils.latencyString(value);
            }
        }),
        latency(new ValueStrategy() {

            @Override
            public long value(VisualizeProfile.InterfaceArea callArea) {
                return TimeUnit.MILLISECONDS.toNanos(1) + Math.max(callArea.callClass.getSuccesslatency(), callArea.callClass.getFailedlatency());
            }

            @Override
            public String name(long value) {
                return nameUtils.latencyString(value);
            }
        }),
        called(new ValueStrategy() {
            @Override
            public long value(VisualizeProfile.InterfaceArea callArea) {
                return Math.max(callArea.callClass.getCalled(), callArea.callClass.getFailed());
            }

            @Override
            public String name(long value) {
                return value + " called";
            }
        }),
        calledBy(new ValueStrategy() {

            @Override
            public long value(VisualizeProfile.InterfaceArea callArea) {
                return callArea.calledByCount;
            }

            @Override
            public String name(long value) {
                return value + " calledBy";
            }
        }),
        callsTo(new ValueStrategy() {

            @Override
            public long value(VisualizeProfile.InterfaceArea callArea) {
                return callArea.callsTo.size();
            }

            @Override
            public String name(long value) {
                return value + " callsTo";
            }
        }),
        callComplexity(new ValueStrategy() {

            @Override
            public long value(VisualizeProfile.InterfaceArea callArea) {
                return callArea.calledByCount * callArea.callsTo.size();
            }

            @Override
            public String name(long value) {
                return value + " callComplexity";
            }
        }), constant(new ValueStrategy() {
            @Override
            public long value(VisualizeProfile.InterfaceArea callArea) {
                return 1;
            }

            @Override
            public String name(long value) {
                return "";
            }
        });

        private final ValueStrategy strategy;

        private StackStrat(ValueStrategy strategy) {
            this.strategy = strategy;
        }

        public ValueStrategy getStrategy() {
            return strategy;
        }

    }

    public static enum Colorings {

        heat(new Heat()),
        grays(new Grayscale()),
        name(new ClassColoring());

        private final Coloring coloring;

        private Colorings(Coloring coloring) {
            this.coloring = coloring;
        }

        public Coloring getColoring() {
            return coloring;
        }

    }

    public static enum BarStrat {

        totaltime(new BarStrategy() {
            @Override
            public Object value(VisualizeProfile.InterfaceArea callArea) {
                long tn = Math.max(callArea.callClass.getSuccesslatency(), callArea.callClass.getFailedlatency()) * callArea.callClass.getCalled();
                return nameUtils.nanosToIndex(tn);
            }

            @Override
            public String name(Object value) {
                long v = (Long) value;
                return nameUtils.nanoIndexToString(v);
            }
        }),
        latency(new BarStrategy() {

            @Override
            public Object value(VisualizeProfile.InterfaceArea callArea) {
                long tn = TimeUnit.MILLISECONDS.toNanos(1) + Math.max(callArea.callClass.getSuccesslatency(), callArea.callClass.getFailedlatency());
                return nameUtils.nanosToIndex(tn);
            }

            @Override
            public String name(Object value) {
                long v = (Long) value;
                return nameUtils.nanoIndexToString(v);
            }
        }), called(new BarStrategy() {
            @Override
            public Object value(VisualizeProfile.InterfaceArea callArea) {
                long called = Math.max(callArea.callClass.getCalled(), callArea.callClass.getFailed());
                return nameUtils.calledOrderOfMagnitudeIndex(called);
            }

            @Override
            public String name(Object value) {
                long v = (Long) value;
                return nameUtils.orderOfMagnitudeIndexToString(v);
            }
        }),
        calledBy(new BarStrategy() {

            @Override
            public Object value(VisualizeProfile.InterfaceArea callArea) {
                return 1L + (long) (callArea.calledByCount);
            }

            @Override
            public String name(Object value) {
                return ((Long) value - 1L) + " calledBy";
            }
        }),
        callsTo(new BarStrategy() {
            @Override
            public Object value(VisualizeProfile.InterfaceArea callArea) {
                return 1L + (long) (callArea.callsTo.size());
            }

            @Override
            public String name(Object value) {
                return ((Long) value - 1L) + " callsTo";
            }
        }),
        callComplexity(new BarStrategy() {
            @Override
            public Object value(VisualizeProfile.InterfaceArea callArea) {
                return (long) (1 + callArea.calledByCount * callArea.callsTo.size());
            }

            @Override
            public String name(Object value) {
                return ((Long) value - 1L) + " callComplexity";
            }
        }),
        stackDepths(new BarStrategy() {

            @Override
            public Object value(VisualizeProfile.InterfaceArea callArea) {
                return 1 + (long) (callArea.depths.size());
            }

            @Override
            public String name(Object value) {
                return ((Long) value - 1L) + " stackDepths";
            }
        }),
        className(new BarStrategy() {
            @Override
            public Object value(VisualizeProfile.InterfaceArea callArea) {
                return callArea.getName();
            }

            @Override
            public String name(Object value) {
                return nameUtils.shortName(value.toString());
            }
        });

        private final BarStrategy strategy;

        private BarStrat(BarStrategy strategy) {
            this.strategy = strategy;
        }

        public BarStrategy getStrategy() {
            return strategy;
        }

    }

    public static enum ClassNameStrat {

        none(new BarStrategy() {
            @Override
            public Object value(VisualizeProfile.InterfaceArea callArea) {
                return null;
            }

            @Override
            public String name(Object value) {
                return null;
            }
        }),
        className(new BarStrategy() {
            @Override
            public Object value(VisualizeProfile.InterfaceArea callArea) {
                return callArea.getName();
            }

            @Override
            public String name(Object value) {
                return nameUtils.shortName(value.toString());
            }
        }),
        fullClassName(new BarStrategy() {
            @Override
            public Object value(VisualizeProfile.InterfaceArea callArea) {
                return callArea.getName();
            }

            @Override
            public String name(Object value) {
                return value.toString();
            }
        });

        private final BarStrategy strategy;

        private ClassNameStrat(BarStrategy strategy) {
            this.strategy = strategy;
        }

        public BarStrategy getStrategy() {
            return strategy;
        }
    }

    public static enum StackOrder {

        ascending, desending
    }

    public static enum Background {

        alpha, white, black
    }

}
