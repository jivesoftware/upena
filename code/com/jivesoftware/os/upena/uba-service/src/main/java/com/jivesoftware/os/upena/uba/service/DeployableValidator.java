/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.upena.uba.service;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.io.File;

public class DeployableValidator {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public boolean validateDeployable(InstancePath instancePath) {
        File serviceRoot = instancePath.serviceRoot();
        String[] expectedScriptNames = new String[]{"init", "config", "start", "status", "kill"};
        for (String scriptName : expectedScriptNames) {
            File scripFile = new File(serviceRoot, "bin/" + scriptName);
            if (!scripFile.exists()) {
                LOG.error("Invalid deployable: bin/" + scriptName + " doesn't exists.");
                return false;
            }
            if (!scripFile.isFile()) {
                LOG.error("Invalid deployable: bin/" + scriptName + " expected to be a file.");
                return false;
            }
            scripFile.setExecutable(true);
        }
        return true;
    }
}
