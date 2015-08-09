package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.ReleasesPluginRegion.ReleasesPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
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
public class ReleasesPluginRegion implements PageRegion<ReleasesPluginRegionInput> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final String simpleTemplate;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    public ReleasesPluginRegion(String template,
        String simpleTemplate,
        SoyRenderer renderer,
        UpenaStore upenaStore) {
        this.template = template;
        this.simpleTemplate = simpleTemplate;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
    }

    public static class ReleasesPluginRegionInput implements PluginInput {

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

        @Override
        public String name() {
            return "Releases";
        }

    }

    @Override
    public String render(String user, ReleasesPluginRegionInput input) {
        Map<String, Object> data = renderData(input, user);
        return renderer.render(template, data);
    }

    public String renderSimple(String user, ReleasesPluginRegionInput input) {
        Map<String, Object> data = renderData(input, user);
        data.put("filters", null);
        return renderer.render(simpleTemplate, data);
    }

    private Map<String, Object> renderData(ReleasesPluginRegionInput input, String user) {
        Map<String, Object> data = Maps.newHashMap();
        try {

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
                        ReleaseGroup newRelease = new ReleaseGroup(input.name,
                            input.email,
                            input.version,
                            input.repository,
                            input.description
                        );
                        upenaStore.releaseGroups.update(null, newRelease);

                        data.put("message", "Created Release:" + input.name);
                        upenaStore.record(user, "added", System.currentTimeMillis(), "", "release-ui", newRelease.toString());

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

                            List<String> errors = new CheckReleasable().isReleasable(input.repository, input.version);
                            if (errors.isEmpty()) {

                                ReleaseGroup updated = new ReleaseGroup(input.name,
                                    input.email,
                                    input.version,
                                    input.repository,
                                    input.description);
                                upenaStore.releaseGroups.update(new ReleaseGroupKey(input.key), updated);
                                data.put("message", "Updated Release:" + input.name);
                                upenaStore.record(user, "updated", System.currentTimeMillis(), "", "release-ui", updated.toString());
                            } else {
                                data.put("message", Joiner.on("\n").join(errors));
                            }
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
                            ReleaseGroupKey releaseGroupKey = new ReleaseGroupKey(input.key);
                            ReleaseGroup removing = upenaStore.releaseGroups.get(releaseGroupKey);
                            if (removing != null) {
                                upenaStore.releaseGroups.remove(releaseGroupKey);
                                upenaStore.record(user, "removed", System.currentTimeMillis(), "", "release-ui", removing.toString());
                            }

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

                InstanceFilter instanceFilter = new InstanceFilter(
                    null,
                    null,
                    null,
                    key,
                    null,
                    0, 10000);

                Map<InstanceKey, TimestampedValue<Instance>> instances = upenaStore.instances.find(instanceFilter);

                Map<String, String> row = new HashMap<>();
                row.put("instanceCount", String.valueOf(instances.size()));
                row.put("key", key.getKey());
                row.put("name", value.name);
                row.put("email", value.email);
                row.put("repository", value.repository);
                row.put("version", value.version);
                row.put("description", value.description);

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
                
                rows.add(row);
            }
            data.put("releases", rows);

        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }
        return data;
    }

    @Override
    public String getTitle() {
        return "Upena Releases";
    }

}
