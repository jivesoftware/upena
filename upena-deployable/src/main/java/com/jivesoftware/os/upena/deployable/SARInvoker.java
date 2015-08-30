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
package com.jivesoftware.os.upena.deployable;

import com.jivesoftware.os.upena.uba.service.ShellOut;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class SARInvoker {

    public interface SAROutput {

        void line(String line);

        void error(Throwable t);

        void success(boolean success);
    }

    private final ExecutorService execThreads;

    public SARInvoker(ExecutorService execThreads) {
        this.execThreads = execThreads;
    }

    public void stop() {
        execThreads.shutdownNow();
    }

    public Boolean invoke(String[] args, final SAROutput saro) {
        try {
            String[] command = new String[1 + args.length];
            command[0] = "/usr/bin/sar";
            System.arraycopy(args, 0, command, 1, args.length);
            ShellOut.ShellOutput shellOutput = new ShellOut.ShellOutput() {
                @Override
                public void line(String line) {
                    saro.line(line);
                }

                @Override
                public void close() {
                }
            };
            final ShellOut shellOut = new ShellOut(new File("./"), Arrays.asList(command), shellOutput, shellOutput);
            Future<Integer> future = execThreads.submit(shellOut::exec);
            boolean succes = future.get() == 0;
            saro.success(succes);
            return succes;

        } catch (InterruptedException | ExecutionException x) {
            saro.error(x);
            return false;
        }
    }
}
