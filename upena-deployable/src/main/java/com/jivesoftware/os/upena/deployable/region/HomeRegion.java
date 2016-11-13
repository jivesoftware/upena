package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.UpenaHealth;
import com.jivesoftware.os.upena.deployable.region.HomeRegion.HomeInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.uba.service.UbaService;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystem.ProcessSort;
import oshi.util.FormatUtil;
import oshi.util.Util;

/**
 *
 */
public class HomeRegion implements PageRegion<HomeInput>, Runnable {


    private final String template;
    private final SoyRenderer renderer;
    private final HostKey hostKey;
    private final UpenaStore upenaStore;
    private final UbaService ubaService;
    private final RuntimeMXBean runtimeBean;

    private final ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("home-%d").build();
    private final ScheduledExecutorService homeExecutors = Executors.newScheduledThreadPool(1, namedThreadFactory);

    public HomeRegion(String template,
        SoyRenderer renderer,
        HostKey hostKey,
        UpenaStore upenaStore,
        UbaService ubaService) {

        this.template = template;
        this.renderer = renderer;
        this.hostKey = hostKey;
        this.upenaStore = upenaStore;
        this.ubaService = ubaService;
        runtimeBean = ManagementFactory.getRuntimeMXBean();

        homeExecutors.scheduleAtFixedRate(this, 10, 10, TimeUnit.SECONDS);
    }


    private final AtomicReference<List<String>> procs = new AtomicReference<>(Collections.singletonList("ERROR"));
    private final AtomicReference<List<String>> memory = new AtomicReference<>(Collections.singletonList("ERROR"));
    private final AtomicReference<List<String>> cpu = new AtomicReference<>(Collections.singletonList("ERROR"));
    private final AtomicReference<List<String>> sensor = new AtomicReference<>(Collections.singletonList("ERROR"));
    private final AtomicReference<List<String>> power = new AtomicReference<>(Collections.singletonList("ERROR"));
    private final AtomicReference<List<String>> nic = new AtomicReference<>(Collections.singletonList("ERROR"));
    private final AtomicReference<List<String>> disk = new AtomicReference<>(Collections.singletonList("ERROR"));
    private final AtomicReference<List<String>> fsys = new AtomicReference<>(Collections.singletonList("ERROR"));
    private final AtomicReference<List<String>> osys = new AtomicReference<>(Collections.singletonList("ERROR"));
    private final AtomicReference<List<String>> processes = new AtomicReference<>(Collections.singletonList("ERROR"));

    private final AtomicReference<StringBuilder> overviewHeaders = new AtomicReference<>(new StringBuilder());
    private final LinkedList<StringBuilder> overview = new LinkedList<>();


    @Override
    public void run() {

        StringBuilder header = new StringBuilder();
        header.append("<th>time</th>");
        StringBuilder values = new StringBuilder();
        values.append("<td>"+new Date().toString()+"</td>");
        try {
            SystemInfo si = null;
            try {
                si = new SystemInfo();
            } catch (Exception x) {
                LOG.warn("oshi SystemInfo failed.", x);
            }
            HardwareAbstractionLayer hal = null;
            try {
                hal = si.getHardware();


                procs.set(printProcessor(hal, header, values));
                memory.set(printMemory(hal, header, values));
                nic.set(printNetworkInterfaces(hal, header, values));
                cpu.set(printCpu(hal, header, values));
                sensor.set(printSensors(hal, header, values));
                power.set(printPowerSources(hal, header, values));
                disk.set(printDisks(hal, header, values));

            } catch (Exception x) {
                LOG.warn("oshi HardwareAbstractionLayer failed.", x);
            }

            try {
                OperatingSystem os = si.getOperatingSystem();
                fsys.set(printFileSystem(os, header, values));
                osys.set(Collections.singletonList(os.toString()));
                if (hal != null) {
                    processes.set(printProcesses(os, hal, header, values));
                } else {
                    processes.set(Collections.singletonList("ERROR"));
                }

            } catch (Exception x) {
                LOG.warn("oshi OperatingSystem failed.", x);
            }

        } catch (Exception x) {
            LOG.warn("Home failed...", x);
        }
        overviewHeaders.set(header);

        overview.addFirst(values);
        if (overview.size() > 100) {
            overview.removeLast();
        }
    }

    @Override
    public String getRootPath() {
        return "/";
    }

    public static class HomeInput implements PluginInput {

        public HomeInput() {
        }

        @Override
        public String name() {
            return "Upena";
        }

    }

    @Override
    public String render(String user, HomeInput input) {

        Subject s;
        try {
            s = SecurityUtils.getSubject();
        } catch (Exception x) {
            s = null;
            LOG.error("Failure.", x);
        }

        PrincipalCollection principals = s.getPrincipals();
        LOG.info("Realms:" + principals.getRealmNames());
        LOG.info("Primary:" + principals.getPrimaryPrincipal() + " " + principals.getClass() + " " + principals.getPrimaryPrincipal().getClass());


        LOG.info("role:readwrite?" + s.hasRole("readwrite"));
        LOG.info("role:readonly?" + s.hasRole("readonly"));

        LOG.info("perm:read?" + s.isPermitted("read"));
        LOG.info("perm:write?" + s.isPermitted("write"));


        Subject subject = s;
        Map<String, Object> data = Maps.newHashMap();

        List<Map<String, String>> instances = new ArrayList<>();
        try {
            upenaStore.hosts.scan((HostKey key, Host value) -> {
                instances.add(ImmutableMap.of("host", value.name,
                    "name", value.name,
                    "port", String.valueOf(value.port),
                    "path", "/ui"));
                return true;
            });
        } catch (Exception x) {
            LOG.error("Failure.", x);
        }

        if (!instances.isEmpty() && subject != null && subject.isAuthenticated()) {
            data.put("instances", instances);
        }


        data.put("procs", procs.get());
        data.put("memory", memory.get());
        data.put("cpu", cpu.get());
        data.put("sensor", sensor.get());
        data.put("power", power.get());
        data.put("nic", nic.get());
        data.put("disk", disk.get());
        data.put("fs", fsys.get());
        data.put("os", osys.get());
        data.put("process", processes.get());


        try {
            return renderer.render(template, data);
        } catch (Exception x) {
            LOG.warn("soy render failed.", x);
            return "Woop :(";
        }
    }

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final NumberFormat numberFormat = NumberFormat.getInstance();

    public String renderOverview(String user) throws Exception {
        StringBuilder sb = new StringBuilder();


       /* sb.append("<p>");
        sb.append("<span class=\"badge\">").append("uptime " + getDurationBreakdown(runtimeBean.getUptime())).append("</span>");


        UbaReport ubaReport = ubaService.report();
        Multiset<String> stateCount = HashMultiset.create();
        double total = 0;
        for (NannyReport nannyReport : ubaReport.nannyReports) {
            total++;
            stateCount.add(nannyReport.state);
        }


        String[] states = stateCount.elementSet().toArray(new String[0]);
        Arrays.sort(states);
        for (String state : states) {
            sb.append(bar(state + " (" + numberFormat.format(stateCount.count(state)) + ")",
                (int) (((double) stateCount.count(state) / total) * 100),
                "lime", numberFormat.format(total)));
        }
        sb.append("</p>");
*/

        sb.append("<table class=\"table-hover table-condensed float-table-head table-responsive\" style=\"width:100%\">");
        sb.append("<thead> ");
        sb.append("<tr> ");
        sb.append(overviewHeaders.get().toString());
        sb.append("</tr> ");
        sb.append("</thead> ");
        sb.append("<tbody> ");
        for (StringBuilder stringBuilder : overview) {
            sb.append("</tr> ");
            sb.append(stringBuilder.toString());
            sb.append("</tr> ");
        }


        sb.append("</tbody> ");
        sb.append("</table>");


        return sb.toString();
    }

    private String td(String title, int progress, String color, String value) {
        double v = (100 - progress) / 100d;
        return "<td style=\"background-color: rgba(" + UpenaHealth.trafficlightColorRGB(v, 0.75f, (float)(1d-v)) + ")\">" + title + "</td>";
    }


    private String bar(String title, int progress, String color, String value) {

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("progress", progress);
        data.put("color", color);
        data.put("value", value);
        return renderer.render("soy.page.upenaStackedProgress", data);

    }

    public static String getDurationBreakdown(long millis) {
        if (millis < 0) {
            return String.valueOf(millis);
        }

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        millis -= TimeUnit.SECONDS.toMillis(seconds);

        StringBuilder sb = new StringBuilder(64);
        boolean showRemaining = true;
        if (showRemaining || hours > 0) {
            if (hours < 10) {
                sb.append('0');
            }
            sb.append(hours);
            sb.append(":");
            showRemaining = true;
        }
        if (showRemaining || minutes > 0) {
            if (minutes < 10) {
                sb.append('0');
            }
            sb.append(minutes);
            sb.append(":");
            showRemaining = true;
        }
        if (showRemaining || seconds > 0) {
            if (seconds < 10) {
                sb.append('0');
            }
            sb.append(seconds);
            sb.append(".");
            showRemaining = true;
        }
        if (millis < 100) {
            sb.append('0');
        }
        if (millis < 10) {
            sb.append('0');
        }
        sb.append(millis);

        return (sb.toString());
    }


    @Override
    public String getTitle() {
        return "Upena";
    }

    private List<String> printProcessor(HardwareAbstractionLayer hal, StringBuilder header, StringBuilder values) {
        List<String> l = new ArrayList<>();
        try {
            CentralProcessor processor = hal.getProcessor();
            l.add(processor.toString());
            l.add(" " + processor.getPhysicalProcessorCount() + " physical CPU(s)");
            l.add(" " + processor.getLogicalProcessorCount() + " logical CPU(s)");

            l.add("Identifier: " + processor.getIdentifier());
            l.add("Serial Num: " + processor.getSystemSerialNumber());
        } catch (Exception x) {
            l.add("ERROR");
            LOG.warn("Failure", x);
        }
        return l;
    }

    private List<String> printMemory(HardwareAbstractionLayer hal, StringBuilder header, StringBuilder values) {
        List<String> l = new ArrayList<>();
        try {
            GlobalMemory memory = hal.getMemory();
            l.add("Memory: " + FormatUtil.formatBytes(memory.getAvailable()) + "/" + FormatUtil.formatBytes(memory.getTotal()));
            l.add("Swap used: " + FormatUtil.formatBytes(memory.getSwapUsed()) + "/" + FormatUtil.formatBytes(memory.getSwapTotal()));

            header.append("<th>memory</th>");
            values.append(td((int) (((double) memory.getAvailable() / memory.getTotal()) * 100)+"%",
                (int) (((double) memory.getAvailable() / memory.getTotal()) * 100),
                "cyan", FormatUtil.formatBytes(memory.getTotal())));

            header.append("<th>swap</th>");
            values.append(td(FormatUtil.formatBytes(memory.getSwapUsed()),
                (int) (((double) memory.getSwapUsed() / memory.getSwapTotal()) * 100),
                "cyan", FormatUtil.formatBytes(memory.getSwapTotal())));

        } catch (Exception x) {
            l.add("ERROR");
            LOG.warn("Failure", x);
        }
        return l;
    }

    private List<String> printCpu(HardwareAbstractionLayer hal, StringBuilder header, StringBuilder values) {
        List<String> l = new ArrayList<>();
        try {
            CentralProcessor processor = hal.getProcessor();
            l.add("Uptime: " + FormatUtil.formatElapsedSecs(processor.getSystemUptime()));

            long[] prevTicks = processor.getSystemCpuLoadTicks();
            l.add("CPU, IOWait, and IRQ ticks @ 0 sec:" + Arrays.toString(prevTicks));
            // Wait a second...
            Util.sleep(1000);
            long[] ticks = processor.getSystemCpuLoadTicks();
            l.add("CPU, IOWait, and IRQ ticks @ 1 sec:" + Arrays.toString(ticks));
            long user = ticks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()];
            long nice = ticks[TickType.NICE.getIndex()] - prevTicks[TickType.NICE.getIndex()];
            long sys = ticks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()];
            long idle = ticks[TickType.IDLE.getIndex()] - prevTicks[TickType.IDLE.getIndex()];
            long iowait = ticks[TickType.IOWAIT.getIndex()] - prevTicks[TickType.IOWAIT.getIndex()];
            long irq = ticks[TickType.IRQ.getIndex()] - prevTicks[TickType.IRQ.getIndex()];
            long softirq = ticks[TickType.SOFTIRQ.getIndex()] - prevTicks[TickType.SOFTIRQ.getIndex()];
            long totalCpu = user + nice + sys + idle + iowait + irq + softirq;

            l.add(String.format(
                "User: %.1f%% Nice: %.1f%% System: %.1f%% Idle: %.1f%% IOwait: %.1f%% IRQ: %.1f%% SoftIRQ: %.1f%%%n",
                100d * user / totalCpu,
                100d * nice / totalCpu,
                100d * sys / totalCpu,
                100d * idle / totalCpu,
                100d * iowait / totalCpu,
                100d * irq / totalCpu,
                100d * softirq / totalCpu));

            l.add(String.format("CPU load: %.1f%% (counting ticks)%n", processor.getSystemCpuLoadBetweenTicks() * 100));
            l.add(String.format("CPU load: %.1f%% (OS MXBean)%n", processor.getSystemCpuLoad() * 100));

            double[] loadAverage = processor.getSystemLoadAverage(3);
            l.add("CPU load averages:" + (loadAverage[0] < 0 ? " N/A" : String.format(" %.2f", loadAverage[0]))
                + (loadAverage[1] < 0 ? " N/A" : String.format(" %.2f", loadAverage[1]))
                + (loadAverage[2] < 0 ? " N/A" : String.format(" %.2f", loadAverage[2])));


            // per core CPU
            StringBuilder procCpu = new StringBuilder("CPU load per processor:");
            double[] load = processor.getProcessorCpuLoadBetweenTicks();
            for (double avg : load) {
                procCpu.append(String.format(" %.1f%%", avg * 100));
            }
            l.add(procCpu.toString());

            long maxLoad = load.length * 10; // ??

            header.append("<th>1min</th>");
            values.append(td((loadAverage[0] < 0 ? " N/A" : String.format(" %.2f", loadAverage[0])),
                (int) (((double) loadAverage[0] / maxLoad) * 100),
                "navy", String.valueOf(maxLoad)));

            header.append("<th>5min</th>");
            values.append(td((loadAverage[1] < 0 ? " N/A" : String.format(" %.2f", loadAverage[1])),
                (int) (((double) loadAverage[1] / maxLoad) * 100),
                "navy", String.valueOf(maxLoad)));

            header.append("<th>15min</th>");
            values.append(td((loadAverage[2] < 0 ? " N/A" : String.format(" %.2f", loadAverage[2])),
                (int) (((double) loadAverage[2] / maxLoad) * 100),
                "navy", String.valueOf(maxLoad)));


            header.append("<th>User</th>");
            values.append(td(String.valueOf((int) (100d * user / totalCpu))+"%", (int) (100d * user / totalCpu), "red", String.valueOf(totalCpu)));
            header.append("<th>Nice</th>");
            values.append(td(String.valueOf((int) (100d * nice / totalCpu))+"%", (int) (100d * nice / totalCpu), "red", String.valueOf(totalCpu)));
            header.append("<th>System</th>");
            values.append(td(String.valueOf((int) (100d * sys / totalCpu))+"%", (int) (100d * sys / totalCpu), "red", String.valueOf(totalCpu)));
            header.append("<th>Idle</th>");
            values.append(td(String.valueOf((int) (100d * idle / totalCpu))+"%", (int) (100d * idle / totalCpu), "red", String.valueOf(totalCpu)));
            header.append("<th>Iowait</th>");
            values.append(td(String.valueOf((int) (100d * iowait / totalCpu))+"%", (int) (100d * iowait / totalCpu), "red", String.valueOf(totalCpu)));
            header.append("<th>IRQ</th>");
            values.append(td(String.valueOf((int) (100d * irq / totalCpu))+"%", (int) (100d * irq / totalCpu), "red", String.valueOf(totalCpu)));
            header.append("<th>softIRQ</th>");
            values.append(td(String.valueOf((int) (100d * softirq / totalCpu))+"%", (int) (100d * softirq / totalCpu), "red", String.valueOf(totalCpu)));


        } catch (Exception x) {
            l.add("ERROR");
            LOG.warn("Failure", x);
        }
        return l;
    }

    private List<String> printProcesses(OperatingSystem os, HardwareAbstractionLayer hal, StringBuilder header, StringBuilder values) {
        List<String> l = new ArrayList<>();
        try {
            GlobalMemory memory = hal.getMemory();
            l.add("Processes: " + os.getProcessCount() + ", Threads: " + os.getThreadCount());

            header.append("<th>process</th>");
            values.append(td(String.valueOf(os.getProcessCount()), 0, "red", ""));

            header.append("<th>threads</th>");
            values.append(td(String.valueOf(os.getThreadCount()), 0, "red", ""));


            // Sort by highest CPU
            List<OSProcess> procs = Arrays.asList(os.getProcesses(5, ProcessSort.CPU));

            l.add("   PID  %CPU %MEM       VSZ       RSS Name");
            for (int i = 0; i < procs.size() && i < 5; i++) {
                OSProcess p = procs.get(i);
                l.add(String.format(" %5d, %5.1f, %4.1f, %9s, %9s, %s%n", p.getProcessID(),
                    100d * (p.getKernelTime() + p.getUserTime()) / p.getUpTime(),
                    100d * p.getResidentSetSize() / memory.getTotal(), FormatUtil.formatBytes(p.getVirtualSize()),
                    FormatUtil.formatBytes(p.getResidentSetSize()), p.getName()));
            }
        } catch (Exception x) {
            l.add("ERROR");
            LOG.warn("Failure", x);
        }
        return l;
    }

    private static List<String> printSensors(HardwareAbstractionLayer hal, StringBuilder header, StringBuilder values) {
        List<String> l = new ArrayList<>();
        try {
            Sensors sensors = hal.getSensors();
            l.add("Sensors:");
            l.add(String.format(" CPU Temperature: %.1f°C%n", sensors.getCpuTemperature()));
            l.add(" Fan Speeds: " + Arrays.toString(sensors.getFanSpeeds()));
            l.add(String.format(" CPU Voltage: %.1fV%n", sensors.getCpuVoltage()));
        } catch (Exception x) {
            l.add("ERROR");
            LOG.warn("Failure", x);
        }
        return l;
    }

    private static List<String> printPowerSources(HardwareAbstractionLayer hal, StringBuilder header, StringBuilder values) {
        List<String> l = new ArrayList<>();
        try {
            PowerSource[] powerSources = hal.getPowerSources();
            StringBuilder powerSb = new StringBuilder("Power: ");
            if (powerSources.length == 0) {
                powerSb.append("Unknown");
            } else {
                double timeRemaining = powerSources[0].getTimeRemaining();
                if (timeRemaining < -1d) {
                    powerSb.append("Charging");
                } else if (timeRemaining < 0d) {
                    powerSb.append("Calculating time remaining");
                } else {
                    powerSb.append(String.format("%d:%02d remaining", (int) (timeRemaining / 3600),
                        (int) (timeRemaining / 60) % 60));
                }
            }
            for (PowerSource pSource : powerSources) {
                powerSb.append(String.format("%n %s @ %.1f%%", pSource.getName(), pSource.getRemainingCapacity() * 100d));
            }
            l.add(powerSb.toString());
        } catch (Exception x) {
            l.add("ERROR");
            LOG.warn("Failure", x);
        }
        return l;
    }

    AtomicLong[] lastReadCount;
    AtomicLong[] lastReads;
    AtomicLong[] lastWriteCount;
    AtomicLong[] lastWrites;

    private List<String> printDisks(HardwareAbstractionLayer hal, StringBuilder header, StringBuilder values) {
        List<String> l = new ArrayList<>();
        try {
            HWDiskStore[] diskStores = hal.getDiskStores();

            if (lastReadCount == null || lastReadCount.length != diskStores.length) {
                lastReadCount = new AtomicLong[diskStores.length];
                lastReads = new AtomicLong[diskStores.length];
                lastWriteCount = new AtomicLong[diskStores.length];
                lastWrites = new AtomicLong[diskStores.length];

                for (int i = 0; i < diskStores.length; i++) {
                    lastReadCount[i] = new AtomicLong(-1);
                    lastReads[i] = new AtomicLong(-1);
                    lastWriteCount[i] = new AtomicLong(-1);
                    lastWrites[i] = new AtomicLong(-1);
                }
            }

            l.add("Disks:");
            for (HWDiskStore disk : diskStores) {
                boolean readwrite = disk.getReads() > 0 || disk.getWrites() > 0;
                l.add(String.format(" %s: (model: %s - S/N: %s) size: %s, reads: %s (%s), writes: %s (%s), xfer: %s ms%n",
                    disk.getName(), disk.getModel(), disk.getSerial(),
                    disk.getSize() > 0 ? FormatUtil.formatBytesDecimal(disk.getSize()) : "?",
                    readwrite ? disk.getReads() : "?", readwrite ? FormatUtil.formatBytes(disk.getReadBytes()) : "?",
                    readwrite ? disk.getWrites() : "?", readwrite ? FormatUtil.formatBytes(disk.getWriteBytes()) : "?",
                    readwrite ? disk.getTransferTime() : "?"));
                HWPartition[] partitions = disk.getPartitions();
                if (partitions == null) {
                    // TODO Remove when all OS's implemented
                    continue;
                }
                for (HWPartition part : partitions) {
                    l.add(String.format(" |-- %s: %s (%s) Maj:Min=%d:%d, size: %s%s%n", part.getIdentification(),
                        part.getName(), part.getType(), part.getMajor(), part.getMinor(),
                        FormatUtil.formatBytesDecimal(part.getSize()),
                        part.getMountPoint().isEmpty() ? "" : " @ " + part.getMountPoint()));
                }
            }

            int i = 1;
            for (HWDiskStore disk : diskStores) {
                boolean readwrite = disk.getReads() > 0 || disk.getWrites() > 0;
                l.add(String.format(" %s: (model: %s - S/N: %s) size: %s, reads: %s (%s), writes: %s (%s), xfer: %s ms%n",
                    disk.getName(), disk.getModel(), disk.getSerial(),
                    disk.getSize() > 0 ? FormatUtil.formatBytesDecimal(disk.getSize()) : "?",
                    readwrite ? disk.getReads() : "?", readwrite ? FormatUtil.formatBytes(disk.getReadBytes()) : "?",
                    readwrite ? disk.getWrites() : "?", readwrite ? FormatUtil.formatBytes(disk.getWriteBytes()) : "?",
                    readwrite ? disk.getTransferTime() : "?"));

                header.append("<th>disk-" + i + "-#R</th>");
                if (lastReadCount[i - 1].get() != -1) {
                    values.append(td(readwrite ? String.valueOf(disk.getReads() - lastReadCount[i - 1].get()) : "?", 0, "red", ""));
                } else {
                    values.append("<td></td>");
                }
                header.append("<th>disk-" + i + "-R</th>");
                if (lastReads[i - 1].get() != -1) {

                    values.append(td(readwrite ? FormatUtil.formatBytes(disk.getReadBytes() - lastReads[i - 1].get()) : "?", 0, "red", ""));
                } else {
                    values.append("<td></td>");
                }

                header.append("<th>disk-" + i + "-#W</th>");
                if (lastWriteCount[i - 1].get() != -1) {
                    values.append(td(readwrite ? String.valueOf(disk.getWrites() - lastWriteCount[i - 1].get()) : "?", 0, "red", ""));
                } else {
                    values.append("<td></td>");
                }

                header.append("<th>disk-" + i + "-W</th>");
                if (lastWrites[i - 1].get() != -1) {
                    values.append(td(readwrite ? FormatUtil.formatBytes(disk.getWriteBytes() - lastWrites[i - 1].get()) : "?", 0, "red", ""));
                } else {
                    values.append("<td></td>");
                }

                lastReadCount[i - 1].set(readwrite ? disk.getReads() : -1);
                lastReads[i - 1].set(readwrite ? disk.getReadBytes() : -1);
                lastWriteCount[i - 1].set(readwrite ? disk.getWrites() : -1);
                lastWrites[i - 1].set(readwrite ? disk.getWriteBytes() : -1);


                i++;
            }


        } catch (Exception x) {
            l.add("ERROR");
            LOG.warn("Failure", x);
        }
        return l;
    }

    private List<String> printFileSystem(OperatingSystem os, StringBuilder header, StringBuilder values) {
        List<String> l = new ArrayList<>();
        try {
            FileSystem fileSystem = os.getFileSystem();
            l.add("File System:");

            l.add(String.format(" File Descriptors: %d/%d%n", fileSystem.getOpenFileDescriptors(),
                fileSystem.getMaxFileDescriptors()));

            header.append("<th>file descriptors</th>");
            values.append(td(String.valueOf(fileSystem.getOpenFileDescriptors()),
                (int) (100 * (fileSystem.getOpenFileDescriptors() / (float) fileSystem.getMaxFileDescriptors())), "red", ""));

            OSFileStore[] fileStores = fileSystem.getFileStores();
            for (OSFileStore fs : fileStores) {
                long usable = fs.getUsableSpace();
                long total = fs.getTotalSpace();
                l.add(String.format(" %s (%s) [%s] %s of %s free (%.1f%%) is %s and is mounted at %s%n", fs.getName(),
                    fs.getDescription().isEmpty() ? "file system" : fs.getDescription(), fs.getType(),
                    FormatUtil.formatBytes(usable), FormatUtil.formatBytes(fs.getTotalSpace()), 100d * usable / total,
                    fs.getVolume(), fs.getMount()));

                header.append("<th>" + fs.getName() + "</th>");
                values.append(td((int) (100d * (total - usable) / total)+"%", (int) (100d * (total - usable) / total), "yellow",
                    FormatUtil.formatBytes(fs.getTotalSpace())));
            }
        } catch (Exception x) {
            l.add("ERROR");
            LOG.warn("Failure", x);
        }
        return l;
    }

    AtomicLong lastTimesstamp = new AtomicLong(-1);
    AtomicLong lastBytesSent = new AtomicLong(-1);
    AtomicLong lastBytesRecv = new AtomicLong(-1);


    private List<String> printNetworkInterfaces(HardwareAbstractionLayer hal, StringBuilder header, StringBuilder values) {
        List<String> l = new ArrayList<>();
        try {
            NetworkIF[] networkIFs = hal.getNetworkIFs();
            l.add("Network interfaces:");
            for (NetworkIF net : networkIFs) {
                l.add(String.format(" Name: %s (%s)%n", net.getName(), net.getDisplayName()));
                l.add(String.format("   MAC Address: %s %n", net.getMacaddr()));
                l.add(String.format("   MTU: %s, Speed: %s %n", net.getMTU(), FormatUtil.formatValue(net.getSpeed(), "bps")));
                l.add(String.format("   IPv4: %s %n", Arrays.toString(net.getIPv4addr())));
                l.add(String.format("   IPv6: %s %n", Arrays.toString(net.getIPv6addr())));
                boolean hasData = net.getBytesRecv() > 0 || net.getBytesSent() > 0 || net.getPacketsRecv() > 0
                    || net.getPacketsSent() > 0;

                if (hasData) {

                    l.add(String.format("   Traffic: received %s/%s; transmitted %s/%s %n",
                        hasData ? net.getPacketsRecv() + " packets" : "?",
                        hasData ? FormatUtil.formatBytes(net.getBytesRecv()) : "?",
                        hasData ? net.getPacketsSent() + " packets" : "?",
                        hasData ? FormatUtil.formatBytes(net.getBytesSent()) : "?"));

                    header.append("<th>sent</th>");
                    header.append("<th>recv</th>");

                    if (lastBytesRecv.get() != -1) {
                        long now = System.currentTimeMillis();
                        double sentBps = ((net.getBytesSent() - lastBytesSent.get()) / (double) (now - lastTimesstamp.get())) * 8000;
                        double recvBps = ((net.getBytesRecv() - lastBytesRecv.get()) / (double) (now - lastTimesstamp.get())) * 8000;

                        values.append(
                            td(FormatUtil.formatValue((long) sentBps, "bps"), (int) (100d * (sentBps) / net.getSpeed()), "red",
                                FormatUtil.formatValue(net.getSpeed(), "bps")));

                        values.append(
                            td(FormatUtil.formatValue((long) recvBps, "bps"), (int) (100d * (recvBps) / net.getSpeed()), "red",
                                FormatUtil.formatValue(net.getSpeed(), "bps")));
                    } else {
                        values.append("<td></td>");
                        values.append("<td></td>");
                    }

                    lastTimesstamp.set(System.currentTimeMillis());
                    lastBytesSent.set(net.getBytesSent());
                    lastBytesRecv.set(net.getBytesRecv());
                }
            }
        } catch (Exception x) {
            l.add("ERROR");
            LOG.warn("Failure", x);
        }
        return l;
    }

}
