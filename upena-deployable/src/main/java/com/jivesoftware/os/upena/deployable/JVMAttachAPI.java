package com.jivesoftware.os.upena.deployable;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jonathan.colt
 */
public class JVMAttachAPI {

    public static void main(String[] args) throws Exception {
        new JVMAttachAPI().run();
    }

    public interface ThreadDump {

        boolean histo(String thread, String frameLocation);
    }

    public void threadDump(String hostName, int port, ThreadDump threadDump) throws Exception {
        VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
        AttachingConnector socketConnector = null;
        List<AttachingConnector> attachingConnectors = virtualMachineManager.attachingConnectors();
        for (AttachingConnector attachingConnector : attachingConnectors) {
            if (attachingConnector.transport().name().equals("dt_socket")) {
                socketConnector = attachingConnector;
                break;
            }
        }
        if (socketConnector != null) {
            Map<String, Connector.Argument> defaultArguments = socketConnector.defaultArguments();
            for (Map.Entry<String, Connector.Argument> entry : defaultArguments.entrySet()) {
                System.out.println(entry.getKey() + " " + entry.getValue().getClass() + " " + entry.getValue());
            }

            Connector.StringArgument hostNameArg = (Connector.StringArgument) defaultArguments.get("hostname");
            hostNameArg.setValue(hostName);
            Connector.IntegerArgument portArg = (Connector.IntegerArgument) defaultArguments.get("port");
            portArg.setValue(port);
            VirtualMachine vm = socketConnector.attach(defaultArguments);
            System.out.println("Attached to process '" + vm.name() + "'");
            vm.suspend();
            List<ThreadReference> allThreads = vm.allThreads();
            for (ThreadReference thread : allThreads) {
                if (thread.status() == 1) {
                    //thread.suspend();
                    threadDump.histo(thread.toString(), null);
                    System.out.println(thread.toString());
                    try {
                        for (StackFrame frame : thread.frames()) {
                            System.out.println("> " + frame.location().toString());
                            threadDump.histo(null, frame.location().toString());
                        }
                    } catch (Exception x) {
                    }
                    //thread.resume();
                }
            }
            vm.resume();
            vm.dispose();
        } else {
            throw new Exception("Failed to connect to " + hostName + ":" + port);
        }
    }

    public interface MemoryHisto {

        boolean histo(String name, long count);
    }

    public void memoryHisto(String hostName, int port, MemoryHisto memoryHisto) throws Exception {
        VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
        AttachingConnector socketConnector = null;
        List<AttachingConnector> attachingConnectors = virtualMachineManager.attachingConnectors();
        for (AttachingConnector attachingConnector : attachingConnectors) {
            if (attachingConnector.transport().name().equals("dt_socket")) {
                socketConnector = attachingConnector;
                break;
            }
        }
        if (socketConnector != null) {
            Map<String, Connector.Argument> defaultArguments = socketConnector.defaultArguments();
            for (Map.Entry<String, Connector.Argument> entry : defaultArguments.entrySet()) {
                System.out.println(entry.getKey() + " " + entry.getValue().getClass() + " " + entry.getValue());
            }

            Connector.StringArgument hostNameArg = (Connector.StringArgument) defaultArguments.get("hostname");
            hostNameArg.setValue(hostName);
            Connector.IntegerArgument portArg = (Connector.IntegerArgument) defaultArguments.get("port");
            portArg.setValue(port);
            VirtualMachine vm = socketConnector.attach(defaultArguments);
            System.out.println("Attached to process '" + vm.name() + "'");

            try {
                vm.suspend();
                List<ReferenceType> referenceTypes = vm.allClasses();
                long[] instanceCounts = vm.instanceCounts(referenceTypes);
                for (int i = 0; i < instanceCounts.length; i++) {
                    ReferenceType rt = referenceTypes.get(i);
                    memoryHisto.histo(rt.name(), instanceCounts[i]);
                }
            } finally {
                vm.resume();
            }
        } else {
            throw new Exception("Failed to connect to " + hostName + ":" + port);
        }
    }

    public void run() throws Exception {
        String hostName = "soa-prime-data5.phx1.jivehosted.com";
        int port = 10003;
        VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
        AttachingConnector socketConnector = null;
        List<AttachingConnector> attachingConnectors = virtualMachineManager.attachingConnectors();
        for (AttachingConnector attachingConnector : attachingConnectors) {
            if (attachingConnector.transport().name().equals("dt_socket")) {
                socketConnector = attachingConnector;
                break;
            }
        }
        if (socketConnector != null) {
            Map<String, Connector.Argument> defaultArguments = socketConnector.defaultArguments();
            for (Map.Entry<String, Connector.Argument> entry : defaultArguments.entrySet()) {
                System.out.println(entry.getKey() + " " + entry.getValue().getClass() + " " + entry.getValue());
            }

            Connector.StringArgument hostNameArg = (Connector.StringArgument) defaultArguments.get("hostname");
            hostNameArg.setValue(hostName);
            Connector.IntegerArgument portArg = (Connector.IntegerArgument) defaultArguments.get("port");
            portArg.setValue(port);
            VirtualMachine vm = socketConnector.attach(defaultArguments);
            System.out.println("Attached to process '" + vm.name() + "'");

            vm.suspend();

            try {
                List<ReferenceType> referenceTypes = vm.allClasses();
                long[] instanceCounts = vm.instanceCounts(referenceTypes);
            } finally {
                vm.resume();
            }
        }

    }

    static String[] STATUS = new String[]{
        "ZOMBIE",
        "RUNNING",
        "SLEEPING",
        "MONITOR",
        "WAIT",
        "NOT_STARTED"
    };
}
