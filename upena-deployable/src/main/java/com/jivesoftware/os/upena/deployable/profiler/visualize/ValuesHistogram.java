/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */

package com.jivesoftware.os.upena.deployable.profiler.visualize;

/**
 *
 */
class ValuesHistogram {
    int[] histogram = new int[16];
    int max = 0;



    void reset() {
        max = 0;
        for (int i = 0; i < histogram.length; i++) {
            histogram[i] = 0;
        }
    }

    void value(double value) {
        int bucket = (int) (value * (histogram.length - 1));
        histogram[bucket]++;
        max = Math.max(max, histogram[bucket]);
    }

}
