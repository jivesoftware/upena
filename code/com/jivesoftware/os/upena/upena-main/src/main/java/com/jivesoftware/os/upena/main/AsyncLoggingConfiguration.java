/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.upena.main;

import org.merlin.config.Config;
import org.merlin.config.defaults.BooleanDefault;
import org.merlin.config.defaults.IntDefault;

public interface AsyncLoggingConfiguration extends Config {

    @BooleanDefault(true)
    public Boolean getUseAsyncLogger();

    @BooleanDefault(true)
    public Boolean getIsBlocking();

    @IntDefault(128)
    public int getBufferSize();
}