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
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final Map<String, BreakpointDebuggerOutput> debuggers = new ConcurrentHashMap<>();
    private final ExecutorService debuggerExecutors = Executors.newCachedThreadPool();

    public BreakpointDumperPluginRegion(String template,
        SoyRenderer renderer,
        UpenaStore upenaStore,
        JDIAPI jvm) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
        this.jvm = jvm;
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
        final String className;
        final int lineNumber;

        final String breakpoint;

        final String action;

        public BreakpointDumperPluginRegionInput(String clusterKey, String cluster, String hostKey, String host, String serviceKey, String service,
            String instanceId, String releaseKey, String release, List<String> instanceKeys, String hostName, int port, String className, int lineNumber,
            String breakpoint, String action) {
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
            this.className = className;
            this.lineNumber = lineNumber;
            this.breakpoint = breakpoint;
            this.action = action;
        }

        @Override
        public String name() {
            return "BreakpointDumper";
        }

    }

    @Override
    public String render(String user, BreakpointDumperPluginRegionInput input) {
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

        try {
            if (input.action.equals("find")) {
                InstanceFilter filter = new InstanceFilter(
                    input.clusterKey.isEmpty() ? null : new ClusterKey(input.clusterKey),
                    input.hostKey.isEmpty() ? null : new HostKey(input.hostKey),
                    input.serviceKey.isEmpty() ? null : new ServiceKey(input.serviceKey),
                    input.releaseKey.isEmpty() ? null : new ReleaseGroupKey(input.releaseKey),
                    input.instanceId.isEmpty() ? null : Integer.parseInt(input.instanceId),
                    0, 10000);

                List<Map<String, Object>> instances = new ArrayList<>();
                Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
                for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
                    InstanceKey key = entrySet.getKey();
                    TimestampedValue<Instance> timestampedValue = entrySet.getValue();
                    Instance value = timestampedValue.getValue();
                    instances.add(toMap(key, value));
                }
                data.put("instances", instances);
            }

            if (input.action.equals("attach")) {
                List<String> messages = new ArrayList<>();
                if (input.hostName != null && input.port != -1) {
                    addBreakpoint(input.hostName, input.port, input, messages);
                }
                if (input.instanceKeys != null && !input.instanceKeys.isEmpty()) {
                    for (String instanceKey : input.instanceKeys) {
                        Instance instance = upenaStore.instances.get(new InstanceKey(instanceKey));
                        if (instance != null) {
                            Instance.Port debugPort = instance.ports.get("debug");
                            if (debugPort != null) {
                                Host host = upenaStore.hosts.get(instance.hostKey);
                                if (host != null) {
                                    addBreakpoint(host.hostName, debugPort.port, input, messages);
                                }
                            }
                        }
                    }
                }
                data.put("message", Joiner.on("\n").join(messages));
            }

            if (input.action.equals("dettach")) {
                String key = input.hostName + ":" + input.port;
                BreakpointDebuggerOutput breakpointDebugger = debuggers.get(key);
                if (breakpointDebugger != null) {

                    breakpointDebugger.breakpointDebugger.removeBreakpoint(input.className, input.lineNumber);
                    if (breakpointDebugger.breakpointDebugger.getAttachedBreakpoints().isEmpty()) {
                        debuggers.remove(key);
                        breakpointDebugger.breakpointDebugger.dettach();
                    }
                    data.put("message", "Detached breakpoint debugger for " + input.hostName + ":" + input.port);
                }
            }

        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }

        List<Map<String, Object>> dumps = new ArrayList<>();

        List<String> keys = new ArrayList<>(debuggers.keySet());
        int id = 0;
        for (String key : keys) {
            BreakpointDebuggerOutput breakpointDebugger = debuggers.get(key);
            if (breakpointDebugger != null) {
                Map<String, Object> dump = new HashMap<>();
                dump.put("id", id);
                id++;
                dump.put("hostName", breakpointDebugger.breakpointDebugger.getHostName());
                dump.put("port", breakpointDebugger.breakpointDebugger.getPort());
                dump.put("log", breakpointDebugger.breakpointDebugger.getLog());

                List<Map<String, Object>> breakpoints = new ArrayList<>();
                for (BreakpointDebugger.Breakpoint attachedBreakpoint : breakpointDebugger.breakpointDebugger.getAttachedBreakpoints()) {
                    Map<String, Object> breakpoint = new HashMap<>();
                    breakpoint.put("className", attachedBreakpoint.getClassName());
                    breakpoint.put("lineNumber", attachedBreakpoint.getLineNumber());
                    breakpoint.put("progress", breakpointDebugger.progress(attachedBreakpoint.getClassName(), attachedBreakpoint.getLineNumber()));
                    List<Map<String, String>> got = breakpointDebugger.getCaptured(attachedBreakpoint.getClassName(), attachedBreakpoint.getLineNumber());
                    breakpoint.put("dump", got);
                    if (got == null || got.isEmpty()) {
                        got = breakpointDebugger.getCapturing(attachedBreakpoint.getClassName(), attachedBreakpoint.getLineNumber());
                        breakpoint.put("dump", got);
                    }

                    got = breakpointDebugger.getCapturedFrames(attachedBreakpoint.getClassName(), attachedBreakpoint.getLineNumber());
                    breakpoint.put("frames", got);
                    if (got == null || got.isEmpty()) {
                        got = breakpointDebugger.getCapturingFrames(attachedBreakpoint.getClassName(), attachedBreakpoint.getLineNumber());
                        breakpoint.put("frames", got);
                    }
                    breakpoints.add(breakpoint);
                }

                dump.put("breakpoints", breakpoints);
                dumps.add(dump);
            }
        }

        data.put("dumps", dumps);

        return renderer.render(template, data);
    }

    private void addBreakpoint(String hostName, int port, BreakpointDumperPluginRegionInput input, List<String> messages) {
        if (hostName != null && !hostName.isEmpty() && input.port != -1) {
            if (input.className != null && input.lineNumber != -1) {
                String key = input.hostName + ":" + input.port;
                addBreakpoint(key, input.hostName, input.port, input.className, input.lineNumber, messages);
            }
            if (input.breakpoint != null && !input.breakpoint.isEmpty() && input.breakpoint.indexOf(":") != -1) {
                String key = input.hostName + ":" + input.port;
                String[] classNameLineNumber = input.breakpoint.trim().split(":");
                int lineNumber = Integer.parseInt(classNameLineNumber[1]);
                addBreakpoint(key, input.hostName, input.port, classNameLineNumber[0], lineNumber, messages);
            }
        }
    }

    private void addBreakpoint(String key, String hostName, int port, String className, int lineNumber, List<String> messages) {
        BreakpointDebuggerOutput breakpointDebugger = debuggers.computeIfAbsent(key, (t) -> {
            return new BreakpointDebuggerOutput(jvm.create(hostName, port));
        });

        breakpointDebugger.breakpointDebugger.addBreakpoint(className, lineNumber);
        if (!breakpointDebugger.isCapturing() && !breakpointDebugger.breakpointDebugger.attached()) {
            debuggerExecutors.submit(breakpointDebugger);
            messages.add("Added breakpoint " + className + ":" + lineNumber
                + " to " + hostName + ":" + port
                + " and submitted for attachment.");
        } else {
            messages.add("Added breakpoint  " + className + ":" + lineNumber + " to " + hostName + ":" + port);
        }
    }

    @Override
    public String getTitle() {
        return "Breakpoint Dump";
    }

    class BreakpointDebuggerOutput implements BreakpointState, StackFrames, Runnable {

        private final Map<String, List<Map<String, String>>> capturingFrames = new ConcurrentHashMap<>();
        private final Map<String, List<Map<String, String>>> capturedFrames = new ConcurrentHashMap<>();

        private final Map<String, List<Map<String, String>>> capturing = new ConcurrentHashMap<>();
        private final Map<String, List<Map<String, String>>> captured = new ConcurrentHashMap<>();
        private final Map<String, Double> progress = new ConcurrentHashMap<>();

        final BreakpointDebugger breakpointDebugger;
        private final AtomicBoolean running = new AtomicBoolean(false);

        public BreakpointDebuggerOutput(BreakpointDebugger breakpointDebugger) {
            this.breakpointDebugger = breakpointDebugger;
        }

        public boolean isCapturing() {
            return running.get();
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
                List<Map<String, String>> got = capturingFrames.get(key);
                if (got != null && !got.isEmpty()) {
                    capturedFrames.put(key, got);
                    capturingFrames.remove(key);
                }
            } else {
                List<Map<String, String>> got = capturingFrames.computeIfAbsent(key, (t) -> new ArrayList<>());
                Map<String, String> state = new HashMap<>();
                state.put("className", stackFrameClass);
                state.put("lineNumber", String.valueOf(stackFrameLineNumber));
                got.add(state);
            }
            return true;
        }

        @Override
        public boolean state(double progress, String breakpointClass, int lineNumber, String className, String fieldName, String value, String fieldNames,
            Exception x) {
            String key = breakpointClass + ":" + lineNumber;
            this.progress.put(key, progress);
            if (className == null && fieldName == null && value == null && x == null) {
                List<Map<String, String>> got = capturing.get(key);
                if (got != null && !got.isEmpty()) {
                    captured.put(key, got);
                    capturing.remove(key);
                }
            } else {
                List<Map<String, String>> got = capturing.computeIfAbsent(key, (t) -> new ArrayList<>());
                Map<String, String> state = new HashMap<>();
                state.put("className", className);
                state.put("fieldName", fieldName);
                state.put("value", value);
                state.put("fieldNames", fieldNames);
                state.put("exception", (x != null) ? x.getMessage() : "");
                got.add(state);
            }
            return true;
        }

        public List<Map<String, String>> getCapturedFrames(String breakpointClass, int lineNumber) {
            String key = breakpointClass + ":" + lineNumber;
            return capturedFrames.get(key);
        }

        public List<Map<String, String>> getCapturingFrames(String breakpointClass, int lineNumber) {
            String key = breakpointClass + ":" + lineNumber;
            return capturingFrames.get(key);
        }

        public List<Map<String, String>> getCaptured(String breakpointClass, int lineNumber) {
            String key = breakpointClass + ":" + lineNumber;
            return captured.get(key);
        }

        public List<Map<String, String>> getCapturing(String breakpointClass, int lineNumber) {
            String key = breakpointClass + ":" + lineNumber;
            return capturing.get(key);
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
    }

    private Map<String, Object> toMap(InstanceKey key, Instance value) throws Exception {

        Map<String, Object> map = new HashMap<>();
        map.put("key", key.getKey());

        Cluster cluster = upenaStore.clusters.get(value.clusterKey);
        Host host = upenaStore.hosts.get(value.hostKey);
        Service service = upenaStore.services.get(value.serviceKey);
        ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(value.releaseGroupKey);

        String name = cluster != null ? cluster.name : "unknownCluster";
        name += " - ";
        name += host != null ? host.name : "unknownHost";
        name += " - ";
        name += service != null ? service.name : "unknownService";
        name += " - ";
        name += String.valueOf(value.instanceId);
        name += " - ";
        name += releaseGroup != null ? releaseGroup.name : "unknownRelease";

        map.put("key", value.releaseGroupKey.getKey());
        map.put("name", name);
        return map;
    }
}
