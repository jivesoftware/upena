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

/**
 *
 */
public class ClassColoring implements Coloring {

    @Override
    public AColor value(VisualizeProfile.InterfaceArea callArea, long maxV) {
        return AColor.getHashSolid(callArea.callClass.getName());
    }
}
