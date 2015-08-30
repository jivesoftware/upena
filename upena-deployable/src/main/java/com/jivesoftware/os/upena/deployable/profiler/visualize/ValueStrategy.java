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
public interface ValueStrategy {

    long value(VisualizeProfile.InterfaceArea callArea);

    String name(long value);

}
