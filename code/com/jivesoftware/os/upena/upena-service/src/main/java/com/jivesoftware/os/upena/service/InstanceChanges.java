package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.upena.routing.shared.InstanceChanged;
import java.util.List;

public interface InstanceChanges {

    public void changed(List<InstanceChanged> change) throws Exception;

}
