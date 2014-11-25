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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jonathan.colt
 */
public class HealthLog implements CommandLog {

    private final CommandLog delegateLog;
    private final ArrayList<String> log = new ArrayList<>();
    private final ArrayList<String> commited = new ArrayList<>();

    public HealthLog(CommandLog delegateLog) {
        this.delegateLog = delegateLog;
    }

    @Override
    synchronized public void log(String context, String message, Throwable t) {
        delegateLog.log(context, message, t);
    }

    @Override
    synchronized public void captured(String context, String message, Throwable t) {
        log.add(message);
    }

    @Override
    synchronized public void commit() {
        commited.clear();
        commited.addAll(log);
        log.clear();
    }

    @Override
    synchronized public List<String> commitedLog() {
        return new ArrayList<>(commited);
    }

}
