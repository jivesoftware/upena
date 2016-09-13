package com.jivesoftware.os.upena.deployable;

import com.google.common.base.Joiner;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.BooleanType;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Field;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.LongValue;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.VoidValue;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import sun.tools.attach.HotSpotVirtualMachine;

/**
 *
 * @author jonathan.colt
 */
public class JDIAPI {

    public static enum ThreadDumpLineType {
        thread, monitor, location, eod;
    }

    private static String[] primitiveTypeNames = {"boolean", "byte", "char",
        "short", "int", "long", "float", "double"};

    public interface ThreadDump {

        boolean line(ThreadDumpLineType type, String value);
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
            try {
                List<ThreadReference> allThreads = vm.allThreads();
                for (ThreadReference thread : allThreads) {
                    if (thread.status() == 1) {
                        dump(vm, thread, threadDump);
                    }
                }
                for (ThreadReference thread : allThreads) {
                    if (thread.status() == 3 || thread.status() == 4) {
                        dump(vm, thread, threadDump);
                    }
                }
                for (ThreadReference thread : allThreads) {
                    if (thread.status() == 2) {
                        dump(vm, thread, threadDump);
                    }
                }
            } finally {
                vm.resume();
                vm.dispose();
            }
        } else {
            throw new Exception("Failed to connect to " + hostName + ":" + port);
        }
    }

    /*
        public static final int THREAD_STATUS_UNKNOWN = -1;
    public static final int THREAD_STATUS_ZOMBIE = 0;
    public static final int THREAD_STATUS_RUNNING = 1;
    public static final int THREAD_STATUS_SLEEPING = 2;
    public static final int THREAD_STATUS_MONITOR = 3;
    public static final int THREAD_STATUS_WAIT = 4;
    public static final int THREAD_STATUS_NOT_STARTED = 5;

     */
    static String[] STATUS = new String[]{
        "ZOMBIE",
        "RUNNING",
        "SLEEPING",
        "MONITOR",
        "WAIT",
        "NOT_STARTED"
    };

    private void dump(VirtualMachine vm, ThreadReference thread, ThreadDump threadDump) {

        int status = thread.status();
        String threadString = thread.name();
        threadString += " [" + (status < 0 ? "UNKNOWN" : STATUS[status]) + "] [" + thread.threadGroup().name() + "]";
        threadDump.line(ThreadDumpLineType.thread, threadString);
        try {
            if (vm.canGetOwnedMonitorInfo()) {
                ObjectReference currentContendedMonitor = thread.currentContendedMonitor();
                if (currentContendedMonitor != null) {
                    threadDump.line(ThreadDumpLineType.monitor, "currentContendedMonitor:" + currentContendedMonitor.uniqueID() + " [" + currentContendedMonitor
                        .referenceType()
                        .name() + "]");
                }
                List<ObjectReference> ownedMonitors = thread.ownedMonitors();
                if (ownedMonitors != null && !ownedMonitors.isEmpty()) {
                    for (ObjectReference ownedMonitor : ownedMonitors) {
                        threadDump
                            .line(ThreadDumpLineType.monitor, "ownedMonitor:" + ownedMonitor.uniqueID() + " [" + ownedMonitor.referenceType().name() + "]");
                    }
                }
            }

            for (StackFrame frame : thread.frames()) {
                threadDump.line(ThreadDumpLineType.location, frame.location().toString());
            }
            threadDump.line(ThreadDumpLineType.eod, null);
        } catch (Exception x) {
        }
    }

    public interface MemoryHisto {

        boolean histo(String name);
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
                if (vm instanceof HotSpotVirtualMachine) {
                    String LIVE_OBJECTS_OPTION = "-live";
                    String ALL_OBJECTS_OPTION = "-all";
                    InputStream in = ((HotSpotVirtualMachine) vm).heapHisto(ALL_OBJECTS_OPTION);
                    String drain = drain(in);
                    for (String string : drain.split("\\r?\\n")) {
                        memoryHisto.histo(string);
                    }

                } else {
                    List<Connector> connectors = virtualMachineManager.allConnectors();
                    for (Connector connector : connectors) {
                        memoryHisto.histo(connector.getClass().getCanonicalName());

                    }
                    memoryHisto.histo(vm.getClass().getCanonicalName());
                    List<ReferenceType> referenceTypes = vm.allClasses();
                    long[] instanceCounts = vm.instanceCounts(referenceTypes);
                    for (int i = 0; i < instanceCounts.length; i++) {
                        ReferenceType rt = referenceTypes.get(i);
                        memoryHisto.histo(rt.name() + "=" + instanceCounts[i]);
                    }
                }
            } finally {
                vm.resume();
                vm.dispose();
            }
        } else {
            throw new Exception("Failed to connect to " + hostName + ":" + port);
        }
    }

    private static String drain(InputStream in) throws IOException {
        // read to EOF and just print output
        StringBuilder sb = new StringBuilder();
        byte b[] = new byte[256];
        int n;
        do {
            n = in.read(b);
            if (n > 0) {
                sb.append(new String(b, 0, n, "UTF-8"));
            }
        } while (n > 0);
        in.close();
        return sb.toString();
    }

    private final ConcurrentHashMap<String, BreakpointDebugger> breakpointDebugger = new ConcurrentHashMap<>();

    public BreakpointDebugger create(String hostName, int port) {
        return breakpointDebugger.computeIfAbsent(hostName.trim() + ":" + port, (key) -> new BreakpointDebugger(hostName, port));
    }

    public static class BreakpointDebugger {

        public interface BreakpointState {

            boolean state(String host,
                int port,
                double progress,
                long timestamp,
                String breakpointClass,
                int lineNumber,
                String className,
                String fieldName,
                Callable<String> value,
                List<String> fields,
                List<Callable<String>> fieldValues,
                Exception x) throws Exception;
        }

        public interface StackFrames {

            boolean frames(String breakpointClass, int lineNumber, String stackFrameClass, int stackFrameLineNumber);
        }

        public static class Breakpoint {

            private final String className;
            private final int lineNumber;

            private Breakpoint(String className, int lineNumber) {
                this.className = className;
                this.lineNumber = lineNumber;
            }

            public String getClassName() {
                return className;
            }

            public int getLineNumber() {
                return lineNumber;
            }

            @Override
            public int hashCode() {
                int hash = 7;
                hash = 83 * hash + Objects.hashCode(this.className);
                hash = 83 * hash + this.lineNumber;
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                final Breakpoint other = (Breakpoint) obj;
                if (this.lineNumber != other.lineNumber) {
                    return false;
                }
                if (!Objects.equals(this.className, other.className)) {
                    return false;
                }
                return true;
            }

        }

        private final AtomicBoolean attached = new AtomicBoolean();
        private final String hostName;
        private final int port;

        private final Set<Breakpoint> breakpoints = Collections.newSetFromMap(new ConcurrentHashMap<Breakpoint, Boolean>());
        private final Set<Breakpoint> attachedBreakpoints = Collections.newSetFromMap(new ConcurrentHashMap<Breakpoint, Boolean>());
        private final AtomicLong version = new AtomicLong();
        private final List<String> log = new ArrayList<>();

        public BreakpointDebugger(String hostName, int port) {
            this.hostName = hostName;
            this.port = port;
        }

        public String getHostName() {
            return hostName;
        }

        public int getPort() {
            return port;
        }

        public boolean attached() {
            return attached.get();
        }

        public void dettach() {
            attached.set(false);
        }

        public List<String> getLog() {
            return log;
        }

        public Set<Breakpoint> getBreakpoints() {
            return breakpoints;
        }

        public Set<Breakpoint> getAttachedBreakpoints() {
            return attachedBreakpoints;
        }

        public boolean isAttached(Breakpoint breakpoint) {
            return attachedBreakpoints.contains(breakpoint);
        }

        public void addBreakpoint(String className, int lineNumber) {
            if (breakpoints.add(new Breakpoint(className, lineNumber))) {
                version.incrementAndGet();
            }
        }

        public void removeBreakpoint(String className, int lineNumber) {
            if (breakpoints.remove(new Breakpoint(className, lineNumber))) {
                version.incrementAndGet();
            }
        }

        public void clearBreakpoints() {
            breakpoints.clear();
            version.incrementAndGet();
        }

        public void log(String message) {
            log.add(message);
            while (log.size() > 100) {
                log.remove(0);
            }
        }

        public void run(BreakpointState breakpointState, StackFrames stackFrames) throws Exception {
            if (!attached.compareAndSet(false, true)) {
                log.add("Breakpoint debugger already attached...");
                return;
            }
            log.clear();

            VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
            AttachingConnector socketConnector = null;
            List<AttachingConnector> attachingConnectors = virtualMachineManager.attachingConnectors();
            for (AttachingConnector attachingConnector : attachingConnectors) {
                if (attachingConnector.transport().name().equals("dt_socket")) {
                    socketConnector = attachingConnector;
                    log.add("Found socket connector..");
                    break;
                }
            }
            if (socketConnector != null) {
                VirtualMachine vm = null;
                try {
                    Map<String, Connector.Argument> defaultArguments = socketConnector.defaultArguments();
                    Connector.StringArgument hostNameArg = (Connector.StringArgument) defaultArguments.get("hostname");
                    hostNameArg.setValue(hostName);
                    Connector.IntegerArgument portArg = (Connector.IntegerArgument) defaultArguments.get("port");
                    portArg.setValue(port);
                    vm = socketConnector.attach(defaultArguments);
                    log.add("Attached to " + vm.description());

                    while (attached.get()) {
                        log.clear();
                        attachedBreakpoints.clear();
                        long breakPointsVersion = version.get();
                        List<Location> breakpointLocations = new ArrayList<>();
                        for (Breakpoint breakpoint : breakpoints) {
                            List<ReferenceType> refTypes = vm.classesByName(breakpoint.className);
                            Location breakpointLocation = null;
                            for (ReferenceType refType : refTypes) {
                                if (breakpointLocation != null) {
                                    break;
                                }
                                List<Location> locs = refType.allLineLocations();
                                for (Location loc : locs) {
                                    if (loc.lineNumber() == breakpoint.lineNumber) {
                                        breakpointLocation = loc;
                                        break;
                                    }
                                }
                            }
                            if (breakpointLocation != null) {
                                breakpointLocations.add(breakpointLocation);
                                attachedBreakpoints.add(breakpoint);
                            }
                        }

                        if (!breakpointLocations.isEmpty()) {
                            EventRequestManager evtReqMgr = vm.eventRequestManager();
                            for (Location breakpointLocation : breakpointLocations) {
                                BreakpointRequest bReq = evtReqMgr.createBreakpointRequest(breakpointLocation);
                                bReq.setSuspendPolicy(BreakpointRequest.SUSPEND_ALL);
                                bReq.enable();
                            }

                            EventQueue evtQueue = vm.eventQueue();
                            while (attached.get()) {
                                if (breakPointsVersion < version.get()) {
                                    evtReqMgr.deleteAllBreakpoints();
                                    log.add("Cleared all breakpoints");
                                    break;
                                }
                                log.clear();
                                EventSet evtSet = evtQueue.remove(1000);
                                if (evtSet == null) {
                                    if (attached.get()) {
                                        continue;
                                    } else {
                                        log.add("Dettaching from breakpoints");
                                        break;
                                    }
                                }
                                log.add("Consuming events");
                                EventIterator evtIter = evtSet.eventIterator();
                                while (evtIter.hasNext()) {
                                    try {
                                        Event evt = evtIter.next();
                                        EventRequest evtReq = evt.request();
                                        if (evtReq instanceof BreakpointRequest) {
                                            long start = System.currentTimeMillis();
                                            BreakpointRequest bpReq = (BreakpointRequest) evtReq;
                                            Location location = bpReq.location();
                                            String breakpointClass = location.sourcePath().replace('/', '.'); // Grrrr
                                            breakpointClass = breakpointClass.substring(0, breakpointClass.lastIndexOf(".")); // Grrr
                                            int breakpointLineNumber = location.lineNumber();

                                            log.add("Breakpoint triggered:" + breakpointClass + ":" + breakpointLineNumber);
                                            BreakpointEvent brEvt = (BreakpointEvent) evt;
                                            ThreadReference threadRef = brEvt.thread();
                                            int frameCount = threadRef.frameCount();
                                            for (int i = frameCount - 1; i > 0; i--) {
                                                try {
                                                    StackFrame stackFrame = threadRef.frame(i);
                                                    Location stackLocation = stackFrame.location();
                                                    String stackFrameClass = stackLocation.sourcePath().replace('/', '.'); // Grrrr
                                                    stackFrameClass = stackFrameClass.substring(0, stackFrameClass.lastIndexOf(".")); // Grrr
                                                    int stackFrameLineNumber = location.lineNumber();
                                                    stackFrames.frames(breakpointClass, breakpointLineNumber, stackFrameClass, stackFrameLineNumber);
                                                } catch (Exception x) {
                                                    stackFrames.frames(breakpointClass, breakpointLineNumber, "unknown", -1);
                                                }
                                            }
                                            stackFrames.frames(breakpointClass, breakpointLineNumber, null, -1);

                                            StackFrame stackFrame = threadRef.frame(0);
                                            List<LocalVariable> visVars = stackFrame.visibleVariables();
                                            Map<LocalVariable, Value> values = stackFrame.getValues(visVars);
                                            int i = 0;
                                            Set<Map.Entry<LocalVariable, Value>> entrySet = values.entrySet();
                                            int count = entrySet.size();
                                            for (Map.Entry<LocalVariable, Value> entry : entrySet) {
                                                LocalVariable localVariable = entry.getKey();
                                                Value value = entry.getValue();
                                                double progress = (double) i / (double) count;
                                                try {
                                                    if (!breakpointState.state(hostName,
                                                        port,
                                                        progress,
                                                        start,
                                                        breakpointClass,
                                                        breakpointLineNumber,
                                                        localVariable.typeName(),
                                                        localVariable.name(),
                                                        () -> valueToString(threadRef, value),
                                                        valueToFields(threadRef, value),
                                                        valueToFieldValues(threadRef, value),
                                                        null)) {
                                                        break;
                                                    }
                                                } catch (Exception x) {
                                                    if (!breakpointState.state(hostName,
                                                        port,
                                                        progress,
                                                        start,
                                                        breakpointClass,
                                                        breakpointLineNumber,
                                                        localVariable.typeName(),
                                                        localVariable.name(),
                                                        null,
                                                        null,
                                                        null,
                                                        x)) {
                                                        break;
                                                    }
                                                }
                                                i++;
                                            }
                                            breakpointState.state(hostName,
                                                port,
                                                1.0d,
                                                start,
                                                breakpointClass,
                                                breakpointLineNumber,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null); // EOS
                                            log.add("Capture state for breakpoint:" + breakpointClass + ":" + breakpointLineNumber + " in " + (System
                                                .currentTimeMillis() - start) + " millis");
                                        }
                                    } catch (AbsentInformationException aie) {
                                        log.add("AbsentInformationException: did you compile your target application with -g option?");
                                    } catch (Exception exc) {
                                        log.add(exc.getMessage() + "\n" + Joiner.on("\n").join(exc.getStackTrace()));
                                    } finally {
                                        evtSet.resume();
                                    }
                                }
                            }
                        }
                    }
                    log.add("Exiting Debugger");
                } finally {
                    if (vm != null) {
                        log.add("Shutting down debugger.");
                        vm.resume();
                        vm.dispose();
                    }
                }
            }
        }
    }

    public static List<String> valueToFields(ThreadReference threadRef, Value var) {
        if (var instanceof ObjectReference) {
            List<Field> allFields = ((ObjectReference) var).referenceType().allFields();
            List<String> fields = new ArrayList<>();
            for (Field field : allFields) {
                fields.add(field.name());
            }
            return fields;
        } else {
            return null;
        }
    }

    public static List<Callable<String>> valueToFieldValues(ThreadReference threadRef, Value var) {
        if (var instanceof ObjectReference) {
            List<Field> allFields = ((ObjectReference) var).referenceType().allFields();
            List<Callable<String>> fieldValues = new ArrayList<>();
            for (Field field : allFields) {
                fieldValues.add((Callable<String>) () -> valueToString(threadRef, ((ObjectReference) var).getValue(field)));
            }
            return fieldValues;
        } else {
            return null;
        }
    }

    public static String valueToString(ThreadReference threadRef, Value var) throws Exception {
        if (var == null) {
            return "null";
        }
        if (var instanceof ArrayReference) {
            StringBuilder sb = new StringBuilder("[");
            ArrayReference arrayObj = (ArrayReference) var;
            if (arrayObj.length() == 0) {
                return "[]";
            }
            List<Value> values = arrayObj.getValues();
            for (Value value : values) {
                sb.append(valueToString(threadRef, value)).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("]");
            return sb.toString();
        } else if (var instanceof ObjectReference) {
            StringReference strValue = (StringReference) invoke(threadRef, var, "toString", new ArrayList());
            return strValue.value();
        } else {
            if (var instanceof BooleanValue) {
                return String.valueOf(((BooleanValue) var).value());
            }
            if (var instanceof CharValue) {
                return String.valueOf(((CharValue) var).value());
            }
            if (var instanceof ByteValue) {
                return String.valueOf(((ByteValue) var).value());
            }
            if (var instanceof DoubleValue) {
                return String.valueOf(((DoubleValue) var).value());
            }
            if (var instanceof FloatValue) {
                return String.valueOf(((FloatValue) var).value());
            }
            if (var instanceof IntegerValue) {
                return String.valueOf(((IntegerValue) var).value());
            }
            if (var instanceof LongValue) {
                return String.valueOf(((LongValue) var).value());
            }
            if (var instanceof ShortValue) {
                return String.valueOf(((ShortValue) var).value());
            }
            if (var instanceof VoidValue) {
                return String.valueOf(Void.class);
            }
            return var.toString();
        }
    }

    public static Value invoke(ThreadReference threadRef, Object invoker, String methodName, List args) throws Exception {
        Value value = null;
        Method matchedMethod = null;
        List<Method> methods = null;
        ClassType refType = null;
        ObjectReference obj = null;
        if (invoker instanceof ClassType) {
            refType = (ClassType) invoker;
            methods = refType.methodsByName(methodName);
        } else {
            obj = (ObjectReference) invoker;
            methods = obj.referenceType().methodsByName(methodName);
        }
        if (methods == null || methods.size() == 0) {
            throw new RuntimeException("eval expression error, method '" + methodName + "' can't be found");
        }
        if (methods.size() == 1) {
            matchedMethod = methods.get(0);
        } else {
            matchedMethod = findMatchedMethod(methods, args);
        }

        if (invoker instanceof ClassType) {
            ClassType clazz = (ClassType) refType;
            value = clazz.invokeMethod(threadRef, matchedMethod, args,
                ObjectReference.INVOKE_SINGLE_THREADED);
        } else {
            value = obj.invokeMethod(threadRef, matchedMethod, args,
                ObjectReference.INVOKE_SINGLE_THREADED);
        }

        return value;
    }

    private static Method findMatchedMethod(List<Method> methods, List arguments) {
        for (Method method : methods) {
            try {
                List argTypes = method.argumentTypes();
                if (argumentsMatch(argTypes, arguments)) {
                    return method;
                }
            } catch (ClassNotLoadedException e) {
            }
        }
        return null;
    }

    private static boolean argumentsMatch(List argTypes, List arguments) {
        if (argTypes.size() != arguments.size()) {
            return false;
        }
        Iterator typeIter = argTypes.iterator();
        Iterator valIter = arguments.iterator();
        while (typeIter.hasNext()) {
            Type argType = (Type) typeIter.next();
            Value value = (Value) valIter.next();
            if (value == null) {
                if (isPrimitiveType(argType.name())) {
                    return false;
                }
            }
            if (!value.type().equals(argType)) {
                if (isAssignableTo(value.type(), argType)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isPrimitiveType(String name) {
        for (String primitiveType : primitiveTypeNames) {
            if (primitiveType.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAssignableTo(Type fromType, Type toType) {

        if (fromType.equals(toType)) {
            return true;
        }
        if (fromType instanceof BooleanType && toType instanceof BooleanType) {
            return true;
        }
        if (toType instanceof BooleanType) {
            return false;
        }
        if (fromType instanceof PrimitiveType
            && toType instanceof PrimitiveType) {
            return true;
        }
        if (toType instanceof PrimitiveType) {
            return false;
        }

        if (fromType instanceof ArrayType) {
            return isArrayAssignableTo((ArrayType) fromType, toType);
        }
        List interfaces;
        if (fromType instanceof ClassType) {
            ClassType superclazz = ((ClassType) fromType).superclass();
            if ((superclazz != null) && isAssignableTo(superclazz, toType)) {
                return true;
            }
            interfaces = ((ClassType) fromType).interfaces();
        } else {
            interfaces = ((InterfaceType) fromType).superinterfaces();
        }
        Iterator iter = interfaces.iterator();
        while (iter.hasNext()) {
            InterfaceType interfaze = (InterfaceType) iter.next();
            if (isAssignableTo(interfaze, toType)) {
                return true;
            }
        }
        return false;
    }

    static boolean isArrayAssignableTo(ArrayType fromType, Type toType) {
        if (toType instanceof ArrayType) {
            try {
                Type toComponentType = ((ArrayType) toType).componentType();
                return isComponentAssignable(fromType.componentType(),
                    toComponentType);
            } catch (ClassNotLoadedException e) {
                return false;
            }
        }
        if (toType instanceof InterfaceType) {
            return toType.name().equals("java.lang.Cloneable");
        }
        return toType.name().equals("java.lang.Object");
    }

    private static boolean isComponentAssignable(Type fromType, Type toType) {
        if (fromType instanceof PrimitiveType) {
            return fromType.equals(toType);
        }
        if (toType instanceof PrimitiveType) {
            return false;
        }
        return isAssignableTo(fromType, toType);
    }

}
