package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.amza.shared.RingHost;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import com.jivesoftware.os.upena.uba.service.UbaService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 */
// soy.page.releasesPluginRegion
public class ReleasesPluginRegion implements PageRegion<Optional<ReleasesPluginRegion.ReleasesPluginRegionInput>> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final UpenaService upenaService;
    private final UbaService ubaService;
    private final RingHost ringHost;

    public ReleasesPluginRegion(String template,
        SoyRenderer renderer,
        AmzaInstance amzaInstance,
        UpenaStore upenaStore,
        UpenaService upenaService,
        UbaService ubaService,
        RingHost ringHost) {
        this.template = template;
        this.renderer = renderer;
        this.amzaInstance = amzaInstance;
        this.upenaStore = upenaStore;
        this.upenaService = upenaService;
        this.ubaService = ubaService;
        this.ringHost = ringHost;
    }

    public static class ReleasesPluginRegionInput {

        final String key;
        final String name;
        final String description;
        final String version;
        final String repository;
        final String email;
        final String action;

        public ReleasesPluginRegionInput(String key, String name, String description, String version, String repository, String email, String action) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.version = version;
            this.repository = repository;
            this.email = email;
            this.action = action;
        }

    }

    @Override
    public String render(Optional<ReleasesPluginRegionInput> optionalInput) {
        Map<String, Object> data = Maps.newHashMap();

        try {
            if (optionalInput.isPresent()) {
                ReleasesPluginRegionInput input = optionalInput.get();

                Map<String, String> filters = new HashMap<>();
                filters.put("name", input.name);
                filters.put("email", input.email);
                filters.put("repository", input.repository);
                filters.put("version", input.version);
                filters.put("description", input.description);
                data.put("filters", filters);

                ReleaseGroupFilter filter = new ReleaseGroupFilter(null, null, null, null, null, 0, 10000);
                if (input.action != null) {
                    if (input.action.equals("filter")) {
                        filter = new ReleaseGroupFilter(
                            input.name.isEmpty() ? null : input.name,
                            input.description.isEmpty() ? null : input.description,
                            input.version.isEmpty() ? null : input.version,
                            input.repository.isEmpty() ? null : input.repository,
                            input.email.isEmpty() ? null : input.email,
                            0, 10000);
                        data.put("message", "Filtering: "
                            + "name.contains '" + input.name + "' "
                            + "description.contains '" + input.description + "' "
                            + "version.contains '" + input.version + "' "
                            + "repository.contains '" + input.repository + "'"
                            + "email.contains '" + input.email + "'"
                        );
                    } else if (input.action.equals("add")) {
                        filters.clear();
                        try {
                            upenaStore.releaseGroups.update(null, new ReleaseGroup(input.name,
                                input.email,
                                input.version,
                                input.repository,
                                input.description
                            ));

                            data.put("message", "Created Release:" + input.name);
                        } catch (Exception x) {
                            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                            data.put("message", "Error while trying to add Release:" + input.name + "\n" + trace);
                        }
                    } else if (input.action.equals("update")) {
                        filters.clear();
                        try {
                            ReleaseGroup release = upenaStore.releaseGroups.get(new ReleaseGroupKey(input.key));
                            if (release == null) {
                                data.put("message", "Couldn't update no existent cluster. Someone else likely just removed it since your last refresh.");
                            } else {
                                upenaStore.releaseGroups.update(new ReleaseGroupKey(input.key), new ReleaseGroup(input.name,
                                    input.email,
                                    input.version,
                                    input.repository,
                                    input.description));
                                data.put("message", "Updated Release:" + input.name);
                            }

                        } catch (Exception x) {
                            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                            data.put("message", "Error while trying to add Release:" + input.name + "\n" + trace);
                        }
                    } else if (input.action.equals("remove")) {
                        if (input.key.isEmpty()) {
                            data.put("message", "Failed to remove Release:" + input.name);
                        } else {
                            try {
                                upenaStore.releaseGroups.remove(new ReleaseGroupKey(input.key));
                            } catch (Exception x) {
                                String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                                data.put("message", "Error while trying to remove Release:" + input.name + "\n" + trace);
                            }
                        }
                    }
                }

                List<Map<String, String>> rows = new ArrayList<>();
                Map<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> found = upenaStore.releaseGroups.find(filter);
                for (Map.Entry<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> entrySet : found.entrySet()) {
                    ReleaseGroupKey key = entrySet.getKey();
                    TimestampedValue<ReleaseGroup> timestampedValue = entrySet.getValue();
                    ReleaseGroup value = timestampedValue.getValue();

                    Map<String, String> row = new HashMap<>();
                    row.put("key", key.getKey());
                    row.put("name", value.name);
                    row.put("email", value.email);
                    row.put("repository", value.repository);
                    row.put("version", value.version);
                    row.put("description", value.description);

                    if (input.action.equals("latest")) {
                        boolean newerVersionAvailable = false;
                        StringBuilder newerVersion = new StringBuilder();
                        LinkedHashMap<String, String> latestRelease = new CheckForLatestRelease().isLatestRelease(value.repository, value.version);
                        for (Entry<String, String> e : latestRelease.entrySet()) {
                            if (newerVersion.length() > 0) {
                                newerVersion.append(",");
                            }
                            newerVersion.append(e.getValue());
                            if (!e.getKey().equals(e.getValue())) {
                                newerVersionAvailable = true;
                            }
                        }
                        if (newerVersionAvailable) {
                            row.put("runningLatest", "false");
                            row.put("newerVersion", newerVersion.toString());
                        } else {
                            row.put("runningLatest", "true");
                        }
                    }

                    rows.add(row);
                }
                data.put("releases", rows);

            }
        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Upena Releases";
    }

    static class ServiceStatus {

    }
}
