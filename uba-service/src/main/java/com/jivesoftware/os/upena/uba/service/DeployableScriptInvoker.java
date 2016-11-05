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

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class DeployableScriptInvoker {

    private final ExecutorService execThreads;

    public DeployableScriptInvoker(ExecutorService execThreads) {
        this.execThreads = execThreads;
    }

    public void stop() {
        execThreads.shutdownNow();
    }

    public boolean exists(InstancePath instancePath, final String script) {
        File scriptFile = instancePath.script(script);
        return scriptFile.exists();
    }

    public String scriptPath(InstancePath instancePath, String script) {
        return instancePath.toHumanReadableName() + " bin/" + script;
    }

    public Boolean invoke(final CommandLog commandLog, final InstancePath instancePath, final String script) {
        final String context = "Service:" + instancePath.toHumanReadableName() + " bin/" + script;
        try {
            commandLog.log(context, "invoking:" + script, null);
            File scriptFile = instancePath.script(script);
            String[] command = new String[]{scriptFile.getAbsolutePath()};
            ShellOut.ShellOutput shellOutput = new ShellOut.ShellOutput() {
                @Override
                public void line(String line) {
                    commandLog.captured(context, line, null);
                }

                @Override
                public void close() {
                }
            };
            final ShellOut shellOut = new ShellOut(instancePath.serviceRoot(), Arrays.asList(command), shellOutput, shellOutput);
            Future<Integer> future = execThreads.submit(new Callable<Integer>() {

                @Override
                public Integer call() throws Exception {
                    return shellOut.exec();
                }
            });
            return future.get() == 0;
        } catch (InterruptedException | ExecutionException x) {
            commandLog.log(context, "failure:" + script, x);
            return false;
        }
    }
}
