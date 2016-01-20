/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.upena.deployable.profiler.visualize;

import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.AColor;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.MinMaxFloat;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.MinMaxLong;

/**
 *
 */
public class Grayscale implements Coloring {

    @Override
    public AColor value(VisualizeProfile.InterfaceArea callArea, long maxV) {
        double rank = 1d - (MinMaxLong.zeroToOne(0, maxV, callArea.value));
        return new AColor(1f - MinMaxFloat.zeroToOne(-0.5f, 1.5f, (float) rank));
    }
}
