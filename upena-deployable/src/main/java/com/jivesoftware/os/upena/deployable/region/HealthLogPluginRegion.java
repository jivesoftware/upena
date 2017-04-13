package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.HealthLogPluginRegion.HealthLogPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.RecordedChange;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
// soy.page.hostsPluginRegion
public class HealthLogPluginRegion implements PageRegion<HealthLogPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    public HealthLogPluginRegion(String template,
        SoyRenderer renderer,
        UpenaStore upenaStore) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
    }

    @Override
    public String getRootPath() {
        return "/ui/healthLog";
    }

    public static class HealthLogPluginRegionInput implements PluginInput {

        final String who;
        final String what;
        final String when;
        final String why;
        final String where;
        final String how;
        final String action;

        public HealthLogPluginRegionInput(String who, String what, String when, String where, String why, String how, String action) {
            this.who = who;
            this.what = what;
            this.when = when;
            this.why = why;
            this.where = where;
            this.how = how;
            this.action = action;
        }

        @Override
        public String name() {
            return "Health-Log";
        }

    }

    @Override
    public String render(String user, HealthLogPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();

        try {
            Map<String, String> filters = new HashMap<>();
            filters.put("who", input.who);
            filters.put("what", input.what);
            filters.put("when", input.when);
            filters.put("where", input.where);
            filters.put("why", input.why);
            filters.put("how", input.how);

            final List<Map<String, String>> rows = new ArrayList<>();
            AtomicLong i = new AtomicLong();
            upenaStore.healthLog(TimeUnit.DAYS.toMillis(2), // TODO expose?
                0,
                100, // TODO expose?
                input.who,
                input.what,
                input.why,
                input.where,
                input.how,
                (RecordedChange change) -> {
                    long c = i.incrementAndGet();
                    Map<String, String> row = new HashMap<>();
                    row.put("class", c % 2 == 0 ? "" : "timeline-inverted");
                    row.put("who", change.who);
                    row.put("what", change.what);
                    row.put("when", String.valueOf(humanReadableUptime(System.currentTimeMillis() - change.when)));
                    row.put("why", change.why);
                    row.put("where", change.where);
                    row.put("how", change.how);
                    rows.add(row);
                    return true;
                });

            data.put("log", rows);

        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Health Log";
    }

    public static String humanReadableUptime(long millis) {
        if (millis < 0) {
            return String.valueOf(millis);
        }

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        if (hours < 10) {
            sb.append('0');
        }
        sb.append(hours);
        sb.append(":");
        if (minutes < 10) {
            sb.append('0');
        }
        sb.append(minutes);
        sb.append(":");
        if (seconds < 10) {
            sb.append('0');
        }
        sb.append(seconds);

        return sb.toString();
    }
}
