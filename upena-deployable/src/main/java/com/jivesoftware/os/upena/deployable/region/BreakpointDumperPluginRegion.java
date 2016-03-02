package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.Maps;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.JDIAPI;
import com.jivesoftware.os.upena.deployable.JDIAPI.BreakpointDebugger;
import com.jivesoftware.os.upena.deployable.region.BreakpointDumperPluginRegion.BreakpointDumperPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
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
    private final AmzaInstance amzaInstance;
    private final JDIAPI jvm;
    private final Map<String, BreakpointDebuggerOutput> debuggers = new ConcurrentHashMap<>();
    private final ExecutorService debuggerExecutors = Executors.newCachedThreadPool();

    public BreakpointDumperPluginRegion(String template,
        SoyRenderer renderer,
        AmzaInstance amzaInstance,
        JDIAPI jvm) {
        this.template = template;
        this.renderer = renderer;
        this.amzaInstance = amzaInstance;
        this.jvm = jvm;
    }

    @Override
    public String getRootPath() {
        return "/ui/breakpoint";
    }

    public static class BreakpointDumperPluginRegionInput implements PluginInput {

        final String host;
        final int port;
        final String className;
        final int lineNumber;
        final String action;

        public BreakpointDumperPluginRegionInput(String host, int port, String className, int lineNumber, String action) {
            this.host = host;
            this.port = port;
            this.className = className;
            this.lineNumber = lineNumber;
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
        data.put("host", input.host);
        data.put("port", input.port);
        data.put("className", input.className);
        data.put("lineNumber", input.lineNumber);

        try {
            String key = input.host + ":" + input.port;
            if (input.action.equals("attach")) {
                BreakpointDebuggerOutput breakpointDebugger = debuggers.computeIfAbsent(key, (t) -> {
                    return new BreakpointDebuggerOutput(jvm.create(input.host, input.port));
                });

                breakpointDebugger.breakpointDebugger.addBreakpoint(input.className, input.lineNumber);
                if (!breakpointDebugger.isCapturing() && !breakpointDebugger.breakpointDebugger.attached()) {
                    debuggerExecutors.submit(breakpointDebugger);
                    data.put("message", "Added breakpoint " + input.className + ":" + input.lineNumber
                        + " to " + input.host + ":" + input.port
                        + " and submitted for attachment.");
                } else {
                    data.put("message", "Added breakpoint  " + input.className + ":" + input.lineNumber + " to " + input.host + ":" + input.port);
                }
            }
            if (input.action.equals("dettach")) {
                BreakpointDebuggerOutput breakpointDebugger = debuggers.get(key);
                if (breakpointDebugger != null) {

                    breakpointDebugger.breakpointDebugger.removeBreakpoint(input.className, input.lineNumber);
                    if (breakpointDebugger.breakpointDebugger.getAttachedBreakpoints().isEmpty()) {
                        debuggers.remove(key);
                        breakpointDebugger.breakpointDebugger.dettach();
                    }
                    data.put("message", "Detached breakpoint debugger for " + input.host + ":" + input.port);
                }
            }

        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }

        List<Map<String, Object>> dumps = new ArrayList<>();

        List<String> keys = new ArrayList<>(debuggers.keySet());
        for (String key : keys) {
            BreakpointDebuggerOutput breakpointDebugger = debuggers.get(key);
            if (breakpointDebugger != null) {
                Map<String, Object> dump = new HashMap<>();
                dump.put("host", breakpointDebugger.breakpointDebugger.getHostName());
                dump.put("port", breakpointDebugger.breakpointDebugger.getPort());

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
                    breakpoints.add(breakpoint);
                }

                dump.put("breakpoints", breakpoints);
                dumps.add(dump);
            }
        }

        data.put("dumps", dumps);

        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Breakpoint Dump";
    }

    class BreakpointDebuggerOutput implements BreakpointDebugger.BreakpointState, Runnable {

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
                breakpointDebugger.run(this);
            } catch (Exception x) {
                x.printStackTrace();
            } finally {
                running.compareAndSet(true, false);
            }
        }

        @Override
        public boolean state(double progress, String breakpointClass, int lineNumber, String className, String fieldName, String value, String fieldNames,
            Exception x) {
            System.out.println(breakpointClass + " " + lineNumber + " " + className + " " + fieldName + " " + value + " " + x);
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
            int p = (int) (progress.getOrDefault(key, 0.0d) * 100);
            if (p == 100) {
                return null;
            } else {
                return String.valueOf(p);
            }
        }
    }
}
