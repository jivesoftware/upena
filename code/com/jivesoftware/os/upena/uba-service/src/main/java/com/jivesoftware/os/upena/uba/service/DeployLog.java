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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.collections.buffer.CircularFifoBuffer;

public class DeployLog implements CommandLog {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final AtomicReference<String> state = new AtomicReference<>("idle");
    private final CircularFifoBuffer messages = new CircularFifoBuffer(1000);
    private final ArrayList<String> commitedLog = new ArrayList<>();

    @Override
    synchronized public void captured(String context, String message, Throwable t) {
        log(context, message, t);
    }

    @Override
    synchronized public void log(String context, String message, Throwable t) {
        if (t != null) {
            LOG.warn(context + " " + message, t);
            messages.add(message);
            Writer result = new StringWriter();
            PrintWriter printWriter = new PrintWriter(result);
            t.printStackTrace(printWriter);
            messages.add(result.toString());
            state.set(message + " " + result.toString());
        } else {
            LOG.info(context + " " + message);
            messages.add(message);
            state.set(message);
        }
    }

    List<String> peek() {
        ArrayList peek = new ArrayList<>();
        for (Iterator it = messages.iterator(); it.hasNext();) {
            Object object = it.next();
            if (object != null) {
                peek.add(object.toString());
            }
        }
        return peek;
    }

    @Override
    synchronized public void commit() {
        state.set("Log cleared");
        commitedLog.clear();
        for (Iterator it = messages.iterator(); it.hasNext();) {
            Object object = it.next();
            if (object != null) {
                commitedLog.add(object.toString());
            }
        }
        messages.clear();
    }

    public String getState() {
        return state.get();
    }

    @Override
    synchronized public List<String> commitedLog() {
        return new ArrayList<>(commitedLog);
    }

}
