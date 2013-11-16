package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.amza.service.AmzaService;
import java.net.SocketException;

public class UpenaInitializer {

    public UpenaService initialize(InstanceChanges instanceChanges,
            TenantChanges tenantChanges,
            AmzaService amzaService) throws SocketException, Exception {
        UpenaStore upenaStore = new UpenaStore(amzaService, instanceChanges, tenantChanges);
        upenaStore.attachWatchers();
        UpenaService composerService = new UpenaService(upenaStore);
        return composerService;
    }
}