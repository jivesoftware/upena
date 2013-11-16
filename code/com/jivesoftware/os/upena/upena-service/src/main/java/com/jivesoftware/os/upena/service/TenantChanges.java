package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.upena.routing.shared.TenantChanged;
import java.util.List;

public interface TenantChanges {

    public void changed(List<TenantChanged> change) throws Exception;

}
