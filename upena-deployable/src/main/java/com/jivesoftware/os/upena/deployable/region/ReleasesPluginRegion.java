package com.jivesoftware.os.upena.deployable.region;

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
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroup.Type;
import com.jivesoftware.os.upena.shared.ReleaseGroupFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.ReleaseGroupPropertyKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import com.jivesoftware.os.upena.uba.service.RepositoryProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;

/**
 *
 */
// soy.page.releasesPluginRegion
public class ReleasesPluginRegion implements PageRegion<ReleasesPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final ObjectMapper mapper;
    private final RepositoryProvider repositoryProvider;
    private final String template;
    private final String simpleTemplate;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    public ReleasesPluginRegion(ObjectMapper mapper,
        RepositoryProvider repositoryProvider,
        String template,
        String simpleTemplate,
        SoyRenderer renderer,
        UpenaStore upenaStore) {
        this.mapper = mapper;
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
        SecurityUtils.getSubject().checkPermission("write");
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
        SecurityUtils.getSubject().checkPermission("write");
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
        if (SecurityUtils.getSubject().isPermitted("write")) {
            data.put("readWrite", true);
        }
        return renderer.render(template, data);
    }

    public String renderSimple(String user, ReleasesPluginRegionInput input) {
        Map<String, Object> data = renderData(input, user);
        data.put("filters", null);
        return renderer.render(simpleTemplate, data);
    }

    private Map<String, Object> renderData(ReleasesPluginRegionInput input, String user) {
        Map<String, Object> data = Maps.newHashMap();
        if (SecurityUtils.getSubject().isPermitted("write")) {
            data.put("readWrite", true);
        }
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

            ReleaseGroupFilter filter = new ReleaseGroupFilter(null, null, null, null, null, 0, 100_000);
            if (input.action != null) {
                if (input.action.equals("filter")) {
                    SecurityUtils.getSubject().checkPermission("read");
                    filter = new ReleaseGroupFilter(
                        input.name.isEmpty() ? null : input.name,
                        input.description.isEmpty() ? null : input.description,
                        input.version.isEmpty() ? null : input.version,
                        input.repository.isEmpty() ? null : input.repository,
                        input.email.isEmpty() ? null : input.email,
                        0, 100_000);
                    data.put("message", "Filtering: "
                        + "name.contains '" + input.name + "' "
                        + "description.contains '" + input.description + "' "
                        + "version.contains '" + input.version + "' "
                        + "repository.contains '" + input.repository + "'"
                        + "email.contains '" + input.email + "'"
                    );
                } else if (input.action.equals("add")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleAdd(filters, input, data, user);
                } else if (input.action.equals("update-immediate")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleUpdate(filters, input, data, user, Type.immediate);
                } else if (input.action.equals("update-canary")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleUpdate(filters, input, data, user, Type.canary);
                } else if (input.action.equals("update-rolling")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleUpdate(filters, input, data, user, Type.rolling);
                } else if (input.action.equals("upgrade-immediate")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleUpgrade(filters, input, data, user, Type.immediate);
                } else if (input.action.equals("upgrade-canary")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleUpdate(filters, input, data, user, Type.canary);
                } else if (input.action.equals("upgrade-rolling")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleUpdate(filters, input, data, user, Type.rolling);
                } else if (input.action.equals("upgrade-all-immediate")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    filter = handleUpgradeAll(Type.immediate, filter, input, user, data);
                } else if (input.action.equals("upgrade-all-canary")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    filter = handleUpgradeAll(Type.canary, filter, input, user, data);
                } else if (input.action.equals("upgrade-all-rolling")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    filter = handleUpgradeAll(Type.rolling, filter, input, user, data);
                } else if (input.action.equals("rollback")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleRollback(filters, input, data, user);
                } else if (input.action.equals("remove")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    handleRemove(input, data, user);
                }
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            Map<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> found = upenaStore.releaseGroups.find(false, filter);
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
                    0, 100_000);

                Map<InstanceKey, TimestampedValue<Instance>> instances = upenaStore.instances.find(false, instanceFilter);
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
                row.put("type", value.type.toString());
                row.put("version", value.version);
                row.put("description", value.description);

                List<Map<String, String>> properties = new ArrayList<>();
                Set<String> declaredProperties = new HashSet<>();
                for (Entry<String, String> p : value.properties.entrySet()) {
                    ReleaseGroupPropertyKey releaseGroupPropertyKey = ReleaseGroupPropertyKey.forKey(p.getKey());
                    String defaultValue = releaseGroupPropertyKey != null ? releaseGroupPropertyKey.getDefaultValue() : "";
                    declaredProperties.add(p.getKey());
                    properties.add(ImmutableMap.of("name", p.getKey(), "value", p.getValue(), "default", defaultValue));
                }
                for (ReleaseGroupPropertyKey rgpk : ReleaseGroupPropertyKey.values()) {
                    if (!declaredProperties.contains(rgpk.key())) {
                        properties.add(ImmutableMap.of("name", rgpk.key(), "value", "", "default", rgpk.getDefaultValue()));
                    }
                }

                row.put("properties", properties);

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
        } catch (AuthorizationException x) {
            throw x;
        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }
        return data;
    }

    private ReleaseGroupFilter handleUpgradeAll(Type type, ReleaseGroupFilter filter,
        ReleasesPluginRegionInput input,
        String user,
        Map<String, Object> data) {

        try {
            filter = new ReleaseGroupFilter(
                input.name.isEmpty() ? null : input.name,
                input.email.isEmpty() ? null : input.email,
                input.version.isEmpty() ? null : input.version,
                input.repository.isEmpty() ? null : input.repository,
                input.description.isEmpty() ? null : input.description,
                0, 100_000);

            List<String> messages = new ArrayList<>();
            Map<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> found = upenaStore.releaseGroups.find(false, filter);
            for (Map.Entry<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> entrySet : found.entrySet()) {
                ReleaseGroupKey key = entrySet.getKey();
                ReleaseGroup value = entrySet.getValue().getValue();

                boolean newerVersionAvailable = false;
                StringBuilder newerVersion = new StringBuilder();
                LinkedHashMap<String, String> latestRelease =
                    new CheckForLatestRelease(repositoryProvider).isLatestRelease(value.repository, value.version);
                for (Entry<String, String> latestEntry : latestRelease.entrySet()) {
                    if (newerVersion.length() > 0) {
                        newerVersion.append(",");
                    }
                    newerVersion.append(latestEntry.getValue());
                    if (!latestEntry.getKey().equals(latestEntry.getValue())) {
                        newerVersionAvailable = true;
                    }
                }

                if (newerVersionAvailable) {
                    List<String> errors = new CheckReleasable(repositoryProvider).isReleasable(value.repository, newerVersion.toString());
                    if (errors.isEmpty()) {
                        ReleaseGroup updated = new ReleaseGroup(type,
                            value.name,
                            value.email,
                            value.version,
                            newerVersion.toString(),
                            value.repository,
                            value.description,
                            value.autoRelease,
                            value.properties);
                        upenaStore.releaseGroups.update(key, updated);
                        messages.add("Updated Release:" + value.name);
                        upenaStore.record(user, "updated", System.currentTimeMillis(), "", "release-ui", updated.toString());
                    } else {
                        messages.add(Joiner.on("\n").join(errors));
                    }
                }
            }

            data.put("message", Joiner.on("\n").join(messages));
        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error upgrading release:" + input.name + "\n" + trace);
        }
        return filter;
    }

    private void handleRemove(ReleasesPluginRegionInput input, Map<String, Object> data, String user) {
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
                data.put("message", "Error removing release:" + input.name + "\n" + trace);
            }
        }
    }

    private void handleRollback(Map<String, String> filters, ReleasesPluginRegionInput input, Map<String, Object> data, String user) {
        filters.clear();

        try {
            ReleaseGroup release = upenaStore.releaseGroups.get(new ReleaseGroupKey(input.key));
            if (release == null) {
                data.put("message", "Could not update. No existing cluster. Someone may have removed it since your last refresh.");
            } else {
                List<String> errors = new CheckReleasable(repositoryProvider).isReleasable(input.repository, input.version);
                if (errors.isEmpty()) {
                    ReleaseGroup updated = new ReleaseGroup(Type.immediate,
                        input.name,
                        input.email,
                        null,
                        input.rollback,
                        input.repository,
                        input.description,
                        input.autoRelease,
                        release.properties);
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
    }


    private void handleUpgrade(Map<String, String> filters, ReleasesPluginRegionInput input, Map<String, Object> data, String user, Type type) {
        filters.clear();

        try {
            ReleaseGroup release = upenaStore.releaseGroups.get(new ReleaseGroupKey(input.key));
            if (release == null) {
                data.put("message", "Could not upgrade. No existing cluster. Someone may have removed it since your last refresh.");
            } else {
                List<String> errors = new CheckReleasable(repositoryProvider).isReleasable(input.repository, input.version);
                if (errors.isEmpty()) {
                    ReleaseGroup updated = new ReleaseGroup(type,
                        input.name,
                        input.email,
                        input.version,
                        input.upgrade,
                        input.repository,
                        input.description,
                        input.autoRelease,
                        release.properties);

                    upenaStore.releaseGroups.update(new ReleaseGroupKey(input.key), updated);
                    data.put("message", "Upgrade Release:" + input.name);
                    upenaStore.record(user, "upgrade", System.currentTimeMillis(), "", "release-ui", updated.toString());
                } else {
                    data.put("message", Joiner.on("\n").join(errors));
                }
            }
        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error upgrading release:" + input.name + "\n" + trace);
        }
    }

    private void handleUpdate(Map<String, String> filters, ReleasesPluginRegionInput input, Map<String, Object> data, String user, Type type) {
        filters.clear();

        try {
            ReleaseGroup release = upenaStore.releaseGroups.get(new ReleaseGroupKey(input.key));
            if (release == null) {
                data.put("message", "Could not update. No existing cluster. Someone may have removed it since your last refresh.");
            } else {
                List<String> errors = new CheckReleasable(repositoryProvider).isReleasable(input.repository, input.version);
                if (errors.isEmpty()) {
                    ReleaseGroup updated = new ReleaseGroup(type,
                        input.name,
                        input.email,
                        release.version,
                        input.version,
                        input.repository,
                        input.description,
                        input.autoRelease,
                        release.properties);

                    upenaStore.releaseGroups.update(new ReleaseGroupKey(input.key), updated);
                    data.put("message", "Updated Release:" + input.name);
                    upenaStore.record(user, "updated", System.currentTimeMillis(), "", "release-ui", updated.toString());
                } else {
                    data.put("message", Joiner.on("\n").join(errors));
                }
            }
        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error updating release:" + input.name + "\n" + trace);
        }
    }

    private void handleAdd(Map<String, String> filters, ReleasesPluginRegionInput input, Map<String, Object> data, String user) {
        filters.clear();

        try {
            ReleaseGroup newRelease = new ReleaseGroup(Type.stable,
                input.name,
                input.email,
                null,
                input.version,
                input.repository,
                input.description,
                input.autoRelease,
                new ConcurrentHashMap<>()
            );
            upenaStore.releaseGroups.update(null, newRelease);

            data.put("message", "Created Release:" + input.name);
            upenaStore.record(user, "added", System.currentTimeMillis(), "", "release-ui", newRelease.toString());
        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error adding release:" + input.name + "\n" + trace);
        }
    }

    @Override
    public String getTitle() {
        return "Upena Releases";
    }

    public String doExport(ReleasesPluginRegionInput input, String user) {
        SecurityUtils.getSubject().checkPermission("write");
        try {
            ReleaseGroupFilter filter = new ReleaseGroupFilter(
                input.name.isEmpty() ? null : input.name,
                input.description.isEmpty() ? null : input.description,
                input.version.isEmpty() ? null : input.version,
                input.repository.isEmpty() ? null : input.repository,
                input.email.isEmpty() ? null : input.email,
                0, 100_000);

            ListOfReleaseGroup values = new ListOfReleaseGroup();
            Map<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> found = upenaStore.releaseGroups.find(false, filter);
            for (Map.Entry<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> entrySet : found.entrySet()) {
                TimestampedValue<ReleaseGroup> timestampedValue = entrySet.getValue();
                ReleaseGroup value = timestampedValue.getValue();
                values.add(value);
            }

            return mapper.writeValueAsString(values);

        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
            return e.toString();
        }
    }

    public String doImport(String in, String user) {
        SecurityUtils.getSubject().checkPermission("write");
        Map<String, Object> data = Maps.newHashMap();
        try {
            ListOfReleaseGroup values = mapper.readValue(in, ListOfReleaseGroup.class);

            for (ReleaseGroup value : values) {
                upenaStore.releaseGroups.update(null, value);

                data.put("message", "Import:" + value.name);
                upenaStore.record(user, "imported", System.currentTimeMillis(), "", "release-ui", value.toString());
            }

            return "Imported " + values.size();
        } catch (Exception x) {
            LOG.error("Unable to retrieve data", x);
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            return "Error while trying to import releaseGroups \n" + trace;
        }
    }

    static class ListOfReleaseGroup extends ArrayList<ReleaseGroup> {
    }

    private String instanceToHumanReadableString(Instance instance) throws Exception {
        Cluster cluster = upenaStore.clusters.get(instance.clusterKey);
        Host host = upenaStore.hosts.get(instance.hostKey);
        Service service = upenaStore.services.get(instance.serviceKey);
        ReleaseGroup release = upenaStore.releaseGroups.get(instance.releaseGroupKey);
        return ((cluster == null) ? "unknownCluster" : cluster.name) + "/"
            + ((host == null) ? "unknownHost" : host.name) + "/"
            + ((service == null) ? "unknownService" : service.name) + "/"
            + String.valueOf(instance.instanceId) + "/"
            + ((release == null) ? "unknownRelease" : release.name);
    }

    public void add(String remoteUser, PropertyUpdate update) throws Exception {
        SecurityUtils.getSubject().checkPermission("write");
        ReleaseGroupKey releaseGroupKey = new ReleaseGroupKey(update.releaseKey);
        ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(releaseGroupKey);
        if (releaseGroup != null) {
            releaseGroup.properties.put(update.name, update.value);
            upenaStore.releaseGroups.update(releaseGroupKey, releaseGroup);
        }
    }

    public void remove(String remoteUser, PropertyUpdate update) throws Exception {
        SecurityUtils.getSubject().checkPermission("write");
        ReleaseGroupKey releaseGroupKey = new ReleaseGroupKey(update.releaseKey);
        ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(releaseGroupKey);
        if (releaseGroup != null) {
            releaseGroup.properties.remove(update.name);
            upenaStore.releaseGroups.update(releaseGroupKey, releaseGroup);
        }
    }

    public static class PropertyUpdate {

        public String releaseKey;
        public String name;
        public String value;

        public PropertyUpdate() {
        }

        public PropertyUpdate(String releaseKey, String name, String value) {
            this.releaseKey = releaseKey;
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return "PropertyUpdate{" + "releaseKey=" + releaseKey + ", name=" + name + ", value=" + value + '}';
        }

    }

}
