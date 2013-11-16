package com.jivesoftware.os.uba.shared;

import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
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