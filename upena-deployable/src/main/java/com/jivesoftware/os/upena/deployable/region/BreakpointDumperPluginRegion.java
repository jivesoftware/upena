package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.JDIAPI;
import com.jivesoftware.os.upena.deployable.JDIAPI.BreakpointDebugger;
import com.jivesoftware.os.upena.deployable.JDIAPI.BreakpointDebugger.BreakpointState;
import com.jivesoftware.os.upena.deployable.JDIAPI.BreakpointDebugger.StackFrames;
import com.jivesoftware.os.upena.deployable.region.BreakpointDumperPluginRegion.BreakpointDumperPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jetty.util.ConcurrentHashSet;

/**
 *
 */
// soy.page.upenaRingPluginRegion
public class BreakpointDumperPluginRegion implements PageRegion<BreakpointDumperPluginRegionInput> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;
    private final JDIAPI jvm;
    private final Map<Long, BreakpointDebuggerSession> debuggerSessions = new ConcurrentSkipListMap<>();
    private final AtomicLong debuggerSessionId = new AtomicLong(0);

    public BreakpointDumperPluginRegion(String template,
        SoyRenderer renderer,
        UpenaStore upenaStore,
        JDIAPI jvm) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
        this.jvm = jvm;
        long id = debuggerSessionId.incrementAndGet();
        debuggerSessions.put(id, new BreakpointDebuggerSession(id, 10));
    }

    @Override
    public String getRootPath() {
        return "/ui/breakpoint";
    }

    public static class BreakpointDumperPluginRegionInput implements PluginInput {

        final String clusterKey;
        final String cluster;
        final String hostKey;
        final String host;
        final String serviceKey;
        final String service;
        final String instanceId;
        final String releaseKey;
        final String release;
        final List<String> instanceKeys;
        final String hostName;
        final int port;

        final long sessionId;
        final long connectionId;
        final String breakPointFieldName;
        final String filter;
        final String className;
        final int lineNumber;
        final int maxVersions;

        final String breakpoint;

        final String action;

        public BreakpointDumperPluginRegionInput(String clusterKey, String cluster, String hostKey, String host, String serviceKey, String service,
            String instanceId, String releaseKey, String release, List<String> instanceKeys, String hostName,
            int port,
            long sessionId,
            long connectionId,
            String breakPointFieldName,
            String filter,
            String className,
            int lineNumber,
            int maxVersions,
            String breakpoint,
            String action) {
            this.clusterKey = clusterKey;
            this.cluster = cluster;
            this.hostKey = hostKey;
            this.host = host;
            this.serviceKey = serviceKey;
            this.service = service;
            this.instanceId = instanceId;
            this.releaseKey = releaseKey;
            this.release = release;
            this.instanceKeys = instanceKeys;
            this.hostName = hostName;
            this.port = port;
            this.sessionId = sessionId;
            this.connectionId = connectionId;
            this.breakPointFieldName = breakPointFieldName;
            this.filter = filter;
            this.className = className;
            this.lineNumber = lineNumber;
            this.maxVersions = maxVersions;
            this.breakpoint = breakpoint;
            this.action = action;
        }

        @Override
        public String name() {
            return "BreakpointDumper";
        }

    }

    @Override
    public String render(String user, BreakpointDumperPluginRegionInput input) throws Exception {
        Map<String, Object> data = Maps.newHashMap();
        data.put("hostName", input.hostName);
        data.put("port", input.port);
        data.put("className", input.className);
        data.put("lineNumber", input.lineNumber);

        Map<String, Object> filters = new HashMap<>();
        filters.put("clusterKey", input.clusterKey);
        filters.put("cluster", input.cluster);
        filters.put("hostKey", input.hostKey);
        filters.put("host", input.host);
        filters.put("serviceKey", input.serviceKey);
        filters.put("service", input.service);
        filters.put("instanceId", input.instanceId);
        filters.put("releaseKey", input.releaseKey);
        filters.put("release", input.release);
        data.put("filters", filters);

        if (input.action.equals("addSession")) {
            long sessionId = debuggerSessionId.incrementAndGet();
            debuggerSessions.put(sessionId, new BreakpointDebuggerSession(sessionId, 10));
        } else if (input.action.equals("refresh")) {
        } else if (input.action.length() > 0) {
            BreakpointDebuggerSession session = debuggerSessions.get(input.sessionId);
            if (session == null) {
                data.put("message", "Failed to " + input.action + " connection because there is no session for sessionId:" + input.sessionId);
            } else {

                if (input.action.equals("addConnections")) {

                    InstanceFilter filter = new InstanceFilter(
                        input.clusterKey.isEmpty() ? null : new ClusterKey(input.clusterKey),
                        input.hostKey.isEmpty() ? null : new HostKey(input.hostKey),
                        input.serviceKey.isEmpty() ? null : new ServiceKey(input.serviceKey),
                        input.releaseKey.isEmpty() ? null : new ReleaseGroupKey(input.releaseKey),
                        input.instanceId.isEmpty() ? null : Integer.parseInt(input.instanceId),
                        0, 10000);

                    Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
                    for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
                        TimestampedValue<Instance> timestampedValue = entrySet.getValue();
                        Instance instance = timestampedValue.getValue();
                        Instance.Port debugPort = instance.ports.get("debug");
                        if (debugPort != null) {
                            Host host = upenaStore.hosts.get(instance.hostKey);
                            if (host != null) {
                                Service service = upenaStore.services.get(instance.serviceKey);
                                if (service != null) {
                                    session.add(debuggerSessionId.incrementAndGet(), service.name, host.hostName, debugPort.port);
                                }
                            }
                        }
                    }

                    if (input.hostName != null && !input.hostName.isEmpty() && input.port > 0) {
                        session.add(debuggerSessionId.incrementAndGet(), "manual", input.hostName, input.port);
                    }
                }

                if (input.action.equals("attachAll")) {
                    data.put("message", session.attachAll());
                }

                if (input.action.equals("dettachAll")) {
                    data.put("message", session.dettachAll());
                }

                if (input.action.equals("removeAllConnections")) {
                    data.put("message", session.removeAllConnection());
                }

                if (input.action.equals("attach")) {
                    data.put("message", session.attach(input.connectionId));
                }

                if (input.action.equals("dettach")) {
                    data.put("message", session.dettach(input.connectionId));
                }

                if (input.action.equals("removeConnection")) {
                    data.put("message", session.removeConnection(input.connectionId));
                }

                if (input.action.equals("addBreakpoint")) {
                    if (input.breakpoint != null && input.breakpoint.length() > 0) {
                        String[] classNameLineNumber = input.breakpoint.trim().split(":");
                        int lineNumber = Integer.parseInt(classNameLineNumber[1]);
                        session.addBreakpoint(classNameLineNumber[0], lineNumber);
                    }
                    if (input.className != null && input.className.length() > 0) {
                        session.addBreakpoint(input.className, input.lineNumber);
                    }
                }

                if (input.action.equals("removeBreakpoint")) {
                    if (input.breakpoint != null && input.breakpoint.length() > 0) {
                        String[] classNameLineNumber = input.breakpoint.trim().split(":");
                        int lineNumber = Integer.parseInt(classNameLineNumber[1]);
                        session.removeBreakpoint(classNameLineNumber[0], lineNumber);
                    }
                    if (input.className != null && input.className.length() > 0) {
                        session.removeBreakpoint(input.className, input.lineNumber);
                    }
                }

                if (input.action.equals("setBreakPointFieldFilter")) {
                    session.setBreakPointFieldFilter(input.className, input.lineNumber, input.breakPointFieldName, input.filter);
                }

                if (input.action.equals("removeBreakPointFieldFilter")) {
                    session.removeBreakPointFieldFilter(input.className, input.lineNumber, input.breakPointFieldName);
                }

                if (input.action.equals("enableBreakPointField")) {
                    session.enableBreakpointField(input.className, input.lineNumber, input.breakPointFieldName);
                }

                if (input.action.equals("disableBreakPointField")) {
                    session.disableBreakpointField(input.className, input.lineNumber, input.breakPointFieldName);
                }
            }
        }

        List<Map<String, Object>> sessions = new ArrayList<>();
        List<Long> keys = new ArrayList<>(debuggerSessions.keySet());
        for (Long key : keys) {
            BreakpointDebuggerSession debuggerSession = debuggerSessions.get(key);
            if (debuggerSession != null) {
                List<Map<String, Object>> connections = new ArrayList<>();
                for (Map.Entry<Long, BreakpointDebuggerState> entry : debuggerSession.getAll()) {
                    Map<String, Object> connection = new HashMap<>();
                    BreakpointDebuggerState state = entry.getValue();
                    connection.put("id", String.valueOf(state.id));

                    connection.put("name", state.name);
                    connection.put("log", state.breakpointDebugger.getLog());
                    if (state.isCapturing()) {
                        connection.put("attached", String.valueOf(true));
                    }

                    List<Map<String, Object>> breakpoints = new ArrayList<>();
                    for (BreakpointDebugger.Breakpoint bp : state.breakpointDebugger.getBreakpoints()) {
                        Map<String, Object> breakpoint = new HashMap<>();
                        if (state.breakpointDebugger.isAttached(bp)) {
                            breakpoint.put("attached", "true");
                        }
                        breakpoint.put("className", bp.getClassName());
                        breakpoint.put("lineNumber", bp.getLineNumber());
                        breakpoint.put("progress", state.progress(bp.getClassName(), bp.getLineNumber()));
                        List<Map<String, Object>> got = state.getCaptured(bp.getClassName(), bp.getLineNumber());
                        breakpoint.put("dump", got);
                        if (got == null || got.isEmpty()) {
                            got = state.getCapturing(bp.getClassName(), bp.getLineNumber());
                            breakpoint.put("dump", got);
                        }

                        got = state.getCapturedFrames(bp.getClassName(), bp.getLineNumber());
                        breakpoint.put("frames", got);
                        if (got == null || got.isEmpty()) {
                            got = state.getCapturingFrames(bp.getClassName(), bp.getLineNumber());
                            breakpoint.put("frames", got);
                        }
                        breakpoints.add(breakpoint);
                    }

                    connection.put("breakpoints", breakpoints);
                    connections.add(connection);
                }
                Map<String, Object> session = new HashMap<>();
                session.put("id", String.valueOf(key));
                session.put("connections", connections);
                sessions.add(session);
            }
        }

        data.put("sessions", sessions);
        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Breakpoint Dump";

    }

    class BreakpointDebuggerSession {

        final long id;
        final int maxVersions;
        final Map<Long, BreakpointDebuggerState> breakpointDebuggers = new ConcurrentHashMap<>();
        final Executor debuggerThread = Executors.newCachedThreadPool();

        public BreakpointDebuggerSession(long id, int maxVersions) {
            this.id = id;
            this.maxVersions = maxVersions;
        }

        public Set<Map.Entry<Long, BreakpointDebuggerState>> getAll() {
            return breakpointDebuggers.entrySet();
        }

        public boolean isCapturing() {
            for (BreakpointDebuggerState value : breakpointDebuggers.values()) {
                if (value.isCapturing()) {
                    return true;
                }
            }
            return false;
        }

        public void add(long id, String name, String hostName, int port) {
            BreakpointDebugger debugger = jvm.create(hostName, port);
            breakpointDebuggers.putIfAbsent(id, new BreakpointDebuggerState(id, name + ":" + hostName + ":" + port, debugger));
        }

        private void addBreakpoint(String className, int lineNumber) {
            for (BreakpointDebuggerState value : breakpointDebuggers.values()) {
                value.breakpointDebugger.addBreakpoint(className, lineNumber);
            }
        }

        private void removeBreakpoint(String className, int lineNumber) {
            for (BreakpointDebuggerState value : breakpointDebuggers.values()) {
                value.breakpointDebugger.removeBreakpoint(className, lineNumber);
            }
        }

        private String attach(long connectionId) {
            StringBuilder sb = new StringBuilder();
            BreakpointDebuggerState got = breakpointDebuggers.get(connectionId);
            if (got != null) {
                sb.append("submited ").append(got.name).append("... ");
                debuggerThread.execute(got);
            } else {
                sb.append("could not dettach because there was no debugger for connectionId=").append(connectionId).append("... ");
            }
            return sb.toString();
        }

        private String dettach(long connectionId) {
            StringBuilder sb = new StringBuilder();
            BreakpointDebuggerState got = breakpointDebuggers.get(connectionId);
            if (got != null) {
                sb.append("dettach ").append(got.name).append("... ");
                got.breakpointDebugger.dettach();
            } else {
                sb.append("could not dettach because there was no debugger for connectionId=").append(connectionId).append("... ");
            }
            return sb.toString();
        }

        private String removeConnection(long connectionId) {
            StringBuilder sb = new StringBuilder();
            BreakpointDebuggerState got = breakpointDebuggers.get(connectionId);
            if (got != null) {
                sb.append("dettach ").append(got.name).append("... ");
                got.breakpointDebugger.dettach();
                sb.append("removed connectionId ").append(connectionId).append("... ");
                breakpointDebuggers.remove(connectionId);
            } else {
                sb.append("could not find a connection at id ").append(connectionId).append("... ");
            }
            return sb.toString();
        }

        private String attachAll() {
            StringBuilder sb = new StringBuilder();
            for (BreakpointDebuggerState value : breakpointDebuggers.values()) {
                sb.append("submited ").append(value.name).append("... ");
                debuggerThread.execute(value);
            }
            return sb.toString();
        }

        private String dettachAll() {
            StringBuilder sb = new StringBuilder();
            for (BreakpointDebuggerState value : breakpointDebuggers.values()) {
                sb.append("dettach ").append(value.name).append("... ");
                value.breakpointDebugger.dettach();
            }
            return sb.toString();
        }

        private String removeAllConnection() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Long, BreakpointDebuggerState> entry : breakpointDebuggers.entrySet()) {
                sb.append("dettach ").append(entry.getValue().breakpointDebugger.getHostName()).append("... ");
                entry.getValue().breakpointDebugger.dettach();
                sb.append("removed breakpointDebugger ").append(entry.getKey()).append("... ");
                breakpointDebuggers.remove(entry.getKey());
            }
            return sb.toString();
        }

        private void enableBreakpointField(String className, int lineNumber, String breakPointFieldName) {
            for (BreakpointDebuggerState value : breakpointDebuggers.values()) {
                value.enableBreakpointField(className, lineNumber, breakPointFieldName);
            }
        }

        private void disableBreakpointField(String className, int lineNumber, String breakPointFieldName) {
            for (BreakpointDebuggerState value : breakpointDebuggers.values()) {
                value.disableBreakpointField(className, lineNumber, breakPointFieldName);
            }
        }

        private void setBreakPointFieldFilter(String className, int lineNumber, String breakPointFieldName, String filter) {
            for (BreakpointDebuggerState value : breakpointDebuggers.values()) {
                value.setBreakPointFieldFilter(className, lineNumber, breakPointFieldName, filter);
            }
        }

        private void removeBreakPointFieldFilter(String className, int lineNumber, String breakPointFieldName) {
             for (BreakpointDebuggerState value : breakpointDebuggers.values()) {
                value.removeBreakPointFieldFilter(className, lineNumber, breakPointFieldName);
            }
        }

    }

    static class BreakpointDebuggerState implements BreakpointState, StackFrames, Runnable {

        private final long id;
        private final String name;
        private final BreakpointDebugger breakpointDebugger;
        private final Map<String, List<Map<String, Object>>> capturingFrames = new ConcurrentHashMap<>();
        private final Map<String, List<Map<String, Object>>> capturedFrames = new ConcurrentHashMap<>();

        private AtomicBoolean capturingFailedFilters = new AtomicBoolean(false);
        // breakpoint -> fieldName -> fieldState
        private final Map<String, Capturing> capturing = new ConcurrentHashMap<>();
        // breakpoint -> fieldName -> version -> fieldState
        private final Map<String, Map<String, Map<String, Object>>> captured = new ConcurrentHashMap<>();
        private final Map<String, Double> progress = new ConcurrentHashMap<>();

        private final Set<String> disabledFields = new ConcurrentHashSet<>();
        private final Map<String, String> fieldFilters = new ConcurrentHashMap<>();
        private final AtomicBoolean running = new AtomicBoolean(false);

        public BreakpointDebuggerState(long id, String name, BreakpointDebugger breakpointDebugger) {
            this.id = id;
            this.name = name;
            this.breakpointDebugger = breakpointDebugger;
        }

        private void setBreakPointFieldFilter(String className, int lineNumber, String breakPointFieldName, String filter) {
            if (filter == null || filter.length() == 0) {
                fieldFilters.remove(className + ":" + lineNumber + ":" + breakPointFieldName);
            } else {
                fieldFilters.put(className + ":" + lineNumber + ":" + breakPointFieldName, filter);
            }
        }

        private void removeBreakPointFieldFilter(String className, int lineNumber, String breakPointFieldName) {
            fieldFilters.remove(className + ":" + lineNumber + ":" + breakPointFieldName);
        }

        @Override
        public void run() {
            if (breakpointDebugger.attached()) {
                return;
            }
            try {
                running.compareAndSet(false, true);
                breakpointDebugger.run(this, this);
            } catch (Exception x) {
                breakpointDebugger.log(x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace()));
            } finally {
                running.compareAndSet(true, false);
            }
        }

        @Override
        public boolean frames(String breakpointClass, int lineNumber, String stackFrameClass, int stackFrameLineNumber) {
            String key = breakpointClass + ":" + lineNumber;
            if (stackFrameClass == null && stackFrameLineNumber == -1) {
                List<Map<String, Object>> got = capturingFrames.get(key);
                if (got != null && !got.isEmpty()) {
                    capturedFrames.put(key, got);
                    capturingFrames.remove(key);
                }
            } else {
                List<Map<String, Object>> got = capturingFrames.computeIfAbsent(key, (t) -> new ArrayList<>());
                Map<String, Object> state = new HashMap<>();
                state.put("className", stackFrameClass);
                state.put("lineNumber", String.valueOf(stackFrameLineNumber));
                got.add(state);
            }
            return true;
        }

        @Override
        public boolean state(String hostName,
            int port,
            double progress,
            long timestamp,
            String breakpointClass,
            int lineNumber,
            String className,
            String fieldName,
            Callable<String> value,
            List<String> fieldNames,
            List<Callable<String>> fieldValues,
            Exception x) throws Exception {

            String key = breakpointClass + ":" + lineNumber;
            this.progress.put(key, progress);
            if (className == null && fieldName == null && value == null && x == null) {
                Capturing got = capturing.get(key);
                if (got != null && !got.isEmpty()) {
                    if (got.filterFailed) {
                        capturing.remove(key);
                    } else {
                        captured.put(key, got);
                        capturing.remove(key);
                    }
                }
            } else {
                Capturing got = capturing.computeIfAbsent(key, (k) -> new Capturing());
               
                Map<String, Object> state = new HashMap<>();
                state.put("className", className);
                state.put("fieldName", fieldName);
                String clf = className + ":" + lineNumber + ":" + fieldName;
                if (disabledFields.contains(clf)) {
                    state.put("disabled", String.valueOf(true));
                    state.put("value", "DISABLED");
                } else if (value != null) {
                    String v = value.call();
                    String filter = fieldFilters.get(clf);
                    if (filter != null) {
                        state.put("filter", v);
                        if (!v.contains(filter)) {
                            got.filterFailed = true;
                        }
                    }

                    if (v.length() > 100) {
                        state.put("value", v.substring(0, Math.min(100, v.length())));
                        state.put("fullValue", v);
                    } else {
                        state.put("value", v);
                    }
                }
                if (fieldNames != null && !fieldNames.isEmpty()) {
                    List<Map<String, Object>> fns = new ArrayList<>();
                    for (String name : fieldNames) {
                        HashMap m = new HashMap<>();
                        m.put("name", name);
                        //m.put("disabled", bla); // TODO
                        fns.add(m);
                    }
                    state.put("fieldNames", fns);
                }
                state.put("exception", (x != null) ? x.getMessage() : "");
                state.put("hostName", hostName);
                state.put("port", String.valueOf(port));

                got.put(fieldName, state);
            }
            return true;
        }

        private void enableBreakpointField(String className, int lineNumber, String breakPointFieldName) {
            disabledFields.remove(className + ":" + lineNumber + ":" + breakPointFieldName);
        }

        private void disableBreakpointField(String className, int lineNumber, String breakPointFieldName) {
            disabledFields.add(className + ":" + lineNumber + ":" + breakPointFieldName);
        }

        public List<Map<String, Object>> getCapturedFrames(String breakpointClass, int lineNumber) {
            String key = breakpointClass + ":" + lineNumber;
            return capturedFrames.get(key);
        }

        public List<Map<String, Object>> getCapturingFrames(String breakpointClass, int lineNumber) {
            String key = breakpointClass + ":" + lineNumber;
            return capturingFrames.get(key);
        }

        public List<Map<String, Object>> getCaptured(String breakpointClass, int lineNumber) {
            String key = breakpointClass + ":" + lineNumber;
            Map<String, Map<String, Object>> got = captured.get(key);
            if (got == null) {
                return null;
            }
            List<Map<String, Object>> vs = new ArrayList<>(got.size());
            for (Map<String, Object> value : got.values()) {
                vs.add(value);
            }
            return vs;
        }

        public List<Map<String, Object>> getCapturing(String breakpointClass, int lineNumber) {
            String key = breakpointClass + ":" + lineNumber;
            Map<String, Map<String, Object>> got = capturing.get(key);
            if (got == null) {
                return null;
            }
            List<Map<String, Object>> vs = new ArrayList<>(got.size());
            for (Map<String, Object> value : got.values()) {
                vs.add(value);
            }
            return vs;
        }

        private String progress(String breakpointClass, int lineNumber) {
            String key = breakpointClass + ":" + lineNumber;
            int p = (int) (progress.getOrDefault(key, 0.0d) * 100d);
            if (p == 100) {
                return null;
            } else {
                return String.valueOf(p);
            }
        }

        public boolean isCapturing() {
            return running.get();
        }


    }

    static class Capturing extends ConcurrentSkipListMap<String, Map<String, Object>> {

        public boolean filterFailed = false;

    }
//
//    private Map<String, Object> toMap(InstanceKey key, Instance value) throws Exception {
//
//        Map<String, Object> map = new HashMap<>();
//        map.put("key", key.getKey());
//
//        Cluster cluster = upenaStore.clusters.get(value.clusterKey);
//        Host host = upenaStore.hosts.get(value.hostKey);
//        Service service = upenaStore.services.get(value.serviceKey);
//        ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(value.releaseGroupKey);
//
//        String name = cluster != null ? cluster.name : "unknownCluster";
//        name += " - ";
//        name += host != null ? host.name : "unknownHost";
//        name += " - ";
//        name += service != null ? service.name : "unknownService";
//        name += " - ";
//        name += String.valueOf(value.instanceId);
//        name += " - ";
//        name += releaseGroup != null ? releaseGroup.name : "unknownRelease";
//
//        map.put("key", value.releaseGroupKey.getKey());
//        map.put("name", name);
//        return map;
//    }
}
