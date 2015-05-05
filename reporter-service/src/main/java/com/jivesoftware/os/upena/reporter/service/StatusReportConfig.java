package com.jivesoftware.os.upena.reporter.service;

import org.merlin.config.Config;
import org.merlin.config.defaults.LongDefault;

public interface StatusReportConfig extends Config {

    @LongDefault(10000L)
    public Long getAnnouceEveryNMills();
}
