package com.jivesoftware.os.upena.deployable.region;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
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
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import com.jivesoftware.os.upena.uba.service.RepositoryProvider;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

    private final RepositoryProvider repositoryProvider;
    private final String template;
    private final String simpleTemplate;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    public ReleasesPluginRegion(RepositoryProvider repositoryProvider,
        String template,
        String simpleTemplate,
        SoyRenderer renderer,
        UpenaStore upenaStore) {

        this.repositoryProvider = repositoryProvider;
        this.template = template;
        this.simpleTemplate = simpleTemplate;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
    }

    @Override
    public String getRootPath() {
        return "/ui/releases";
    }

    public Object renderChangelog(String releaseKey) throws Exception {

        ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(new ReleaseGroupKey(releaseKey));
        if (releaseGroup == null) {
            return "No release group for " + releaseKey;
        }

        boolean newerVersionAvailable = false;
        StringBuilder newerVersion = new StringBuilder();
        LinkedHashMap<String, String> latestRelease = new CheckForLatestRelease(repositoryProvider).isLatestRelease(releaseGroup.repository,
            releaseGroup.version);
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
            return new CheckChangelog(repositoryProvider).changelog(releaseGroup.repository, newerVersion.toString());
        } else {
            return new CheckChangelog(repositoryProvider).changelog(releaseGroup.repository, releaseGroup.version);
        }

    }

    public Object renderScm(String releaseKey) throws Exception {
        ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(new ReleaseGroupKey(releaseKey));
        if (releaseGroup == null) {
            return "No release group for " + releaseKey;
        }

        boolean newerVersionAvailable = false;
        StringBuilder newerVersion = new StringBuilder();
        LinkedHashMap<String, String> latestRelease = new CheckForLatestRelease(repositoryProvider).isLatestRelease(releaseGroup.repository,
            releaseGroup.version);
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
            return new CheckGitInfo(repositoryProvider).gitInfo(releaseGroup.repository, newerVersion.toString());
        } else {
            return new CheckGitInfo(repositoryProvider).gitInfo(releaseGroup.repository, releaseGroup.version);
        }

    }

    public static class ReleasesPluginRegionInput implements PluginInput {

        final String key;
        final String name;
        final String description;
        final String rollback;
        final String version;
        final String upgrade;
        final String repository;
        final String email;
        final boolean autoRelease;
        final String action;

        public ReleasesPluginRegionInput(String key,
            String name,
            String description,
            String rollback,
            String version,
            String upgrade,
            String repository,
            String email,
            boolean autoRelease,
            String action) {

            this.key = key;
            this.name = name;
            this.description = description;
            this.rollback = rollback;
            this.version = version;
            this.upgrade = upgrade;
            this.repository = repository;
            this.email = email;
            this.autoRelease = autoRelease;
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
            Map<ServiceKey, String> serviceColor = ServiceColorUtil.serviceKeysColor(upenaStore);

            Map<String, String> filters = new HashMap<>();
            filters.put("name", input.name);
            filters.put("email", input.email);
            filters.put("repository", input.repository);
            filters.put("version", input.version);
            filters.put("description", input.description);
            filters.put("autoRelease", String.valueOf(input.autoRelease));
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
                            null,
                            input.version,
                            input.repository,
                            input.description,
                            input.autoRelease
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

                            List<String> errors = new CheckReleasable(repositoryProvider).isReleasable(input.repository, input.version);
                            if (errors.isEmpty()) {

                                ReleaseGroup updated = new ReleaseGroup(input.name,
                                    input.email,
                                    release.version,
                                    input.version,
                                    input.repository,
                                    input.description,
                                    input.autoRelease);
                                upenaStore.releaseGroups.update(new ReleaseGroupKey(input.key), updated);
                                data.put("message", "Updated Release:" + input.name);
                                upenaStore.record(user, "updated", System.currentTimeMillis(), "", "release-ui", updated.toString());
                            } else {
                                data.put("message", Joiner.on("\n").join(errors));
                            }
                        }

                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to update Release:" + input.name + "\n" + trace);
                    }
                } else if (input.action.equals("upgrade")) {
                    filters.clear();
                    try {
                        ReleaseGroup release = upenaStore.releaseGroups.get(new ReleaseGroupKey(input.key));
                        if (release == null) {
                            data.put("message", "Couldn't update no existent cluster. Someone else likely just removed it since your last refresh.");
                        } else {

                            List<String> errors = new CheckReleasable(repositoryProvider).isReleasable(input.repository, input.version);
                            if (errors.isEmpty()) {

                                ReleaseGroup updated = new ReleaseGroup(input.name,
                                    input.email,
                                    input.version,
                                    input.upgrade,
                                    input.repository,
                                    input.description,
                                    input.autoRelease);
                                upenaStore.releaseGroups.update(new ReleaseGroupKey(input.key), updated);
                                data.put("message", "Upgrade Release:" + input.name);
                                upenaStore.record(user, "upgrade", System.currentTimeMillis(), "", "release-ui", updated.toString());
                            } else {
                                data.put("message", Joiner.on("\n").join(errors));
                            }
                        }

                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to add Release:" + input.name + "\n" + trace);
                    }
                } else if (input.action.equals("rollback")) {
                    filters.clear();
                    try {
                        ReleaseGroup release = upenaStore.releaseGroups.get(new ReleaseGroupKey(input.key));
                        if (release == null) {
                            data.put("message", "Couldn't update no existent cluster. Someone else likely just removed it since your last refresh.");
                        } else {

                            List<String> errors = new CheckReleasable(repositoryProvider).isReleasable(input.repository, input.version);
                            if (errors.isEmpty()) {

                                ReleaseGroup updated = new ReleaseGroup(input.name,
                                    input.email,
                                    null,
                                    input.rollback,
                                    input.repository,
                                    input.description,
                                    input.autoRelease);
                                upenaStore.releaseGroups.update(new ReleaseGroupKey(input.key), updated);
                                data.put("message", "Rollback Release:" + input.name);
                                upenaStore.record(user, "rollback", System.currentTimeMillis(), "", "release-ui", updated.toString());
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

            List<Map<String, Object>> rows = new ArrayList<>();
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
                HashMultiset<ServiceKey> serviceKeyCount = HashMultiset.create();
                for (TimestampedValue<Instance> i : instances.values()) {
                    if (!i.getTombstoned()) {
                        serviceKeyCount.add(i.getValue().serviceKey);
                    }
                }

                List<Map<String, String>> instanceCounts = new ArrayList<>();
                for (ServiceKey sk : new HashSet<>(serviceKeyCount)) {
                    instanceCounts.add(ImmutableMap.of(
                        "count", String.valueOf(serviceKeyCount.count(sk)),
                        "color", serviceColor.get(sk)
                    ));
                }

                Map<String, Object> row = new HashMap<>();
                row.put("instanceCounts", instanceCounts);
                row.put("key", key.getKey());
                row.put("name", value.name);
                row.put("email", value.email);
                row.put("repository", value.repository);
                row.put("autoRelease", value.autoRelease);

                if (value.rollbackVersion != null) {
                    row.put("rollback", value.rollbackVersion);
                }
                row.put("version", value.version);
                row.put("description", value.description);

                boolean newerVersionAvailable = false;
                StringBuilder newerVersion = new StringBuilder();
                LinkedHashMap<String, String> latestRelease = new CheckForLatestRelease(repositoryProvider).isLatestRelease(value.repository, value.version);
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
                    row.put("upgrade", newerVersion.toString());
                } else {
                    row.put("runningLatest", "true");
                }

                rows.add(row);
            }

            Collections.sort(rows, (Map<String, Object> o1, Map<String, Object> o2) -> {
                String aIsLatest = (String) o1.get("runningLatest");
                String bIsLatest = (String) o2.get("runningLatest");
                if (aIsLatest.equals(bIsLatest)) {
                    String aName = (String) o1.get("name");
                    String bName = (String) o2.get("name");
                    return aName.compareTo(bName);
                }
                return Boolean.valueOf(aIsLatest).compareTo(Boolean.valueOf(bIsLatest));
            });

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

    public String doExport(ReleasesPluginRegionInput input, String user) {

        try {

            Map<String, String> filters = new HashMap<>();
            filters.put("name", input.name);
            filters.put("email", input.email);
            filters.put("repository", input.repository);
            filters.put("version", input.version);
            filters.put("description", input.description);
            filters.put("autoRelease", String.valueOf(input.autoRelease));

            ReleaseGroupFilter filter = new ReleaseGroupFilter(
                input.name.isEmpty() ? null : input.name,
                input.description.isEmpty() ? null : input.description,
                input.version.isEmpty() ? null : input.version,
                input.repository.isEmpty() ? null : input.repository,
                input.email.isEmpty() ? null : input.email,
                0, 10000);

            List<ReleaseGroup> values = new ArrayList<>();
            Map<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> found = upenaStore.releaseGroups.find(filter);
            for (Map.Entry<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> entrySet : found.entrySet()) {
                TimestampedValue<ReleaseGroup> timestampedValue = entrySet.getValue();
                ReleaseGroup value = timestampedValue.getValue();
                values.add(value);
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(values);

        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
            return e.toString();
        }
    }

    public String doImport(InputStream in, String user) {
        Map<String, Object> data = Maps.newHashMap();
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<ReleaseGroup> values = mapper.readValue(in, new TypeReference<List<ReleaseGroup>>() {
            });

            for (ReleaseGroup releaseGroup : values) {
                upenaStore.releaseGroups.update(null, releaseGroup);

                data.put("message", "Import Release:" + releaseGroup.name);
                upenaStore.record(user, "imported", System.currentTimeMillis(), "", "release-ui", releaseGroup.toString());
            }

            return "Imported " + values.size();
        } catch (Exception x) {
            log.error("Unable to retrieve data", x);
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            return "Error while trying to import releaseGroups \n" + trace;
        }
    }

}
