package com.jivesoftware.os.upena.deployable;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author jonathan.colt
 */
public class JVMAttachAPI {

    public static void main(String[] args) throws Exception {
        // attach to target VM
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        VirtualMachine vm = VirtualMachine.attach(list.get(0));

        // get system properties in target VM
        Properties props = vm.getSystemProperties();

        // construct path to management agent
        String home = props.getProperty("java.home");
        String agent = home + File.separator + "lib" + File.separator
            + "management-agent.jar";

        // load agent into target VM
        vm.loadAgent(agent, "com.sun.management.jmxremote.port=5000");

        // detach
        vm.detach();
    }
}
