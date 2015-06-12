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
package com.jivesoftware.os.uba.shared;

import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import java.util.List;

/**
 *
 * @author jonathan
 */
public class NannyReport {

    public String state;
    public InstanceDescriptor instanceDescriptor;
    public List<String> messages;

    public NannyReport() {
    }

    public NannyReport(String state, InstanceDescriptor instanceDescriptor, List<String> messages) {
        this.state = state;
        this.instanceDescriptor = instanceDescriptor;
        this.messages = messages;
    }
}