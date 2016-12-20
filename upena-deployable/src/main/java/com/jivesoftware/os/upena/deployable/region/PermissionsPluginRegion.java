package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.PermissionsPluginRegion.PermissionsPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Permission;
import com.jivesoftware.os.upena.shared.PermissionFilter;
import com.jivesoftware.os.upena.shared.PermissionKey;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;

/**
 *
 */
// soy.page.servicesPluginRegion
public class PermissionsPluginRegion implements PageRegion<PermissionsPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    public PermissionsPluginRegion(String template,
        SoyRenderer renderer,
        UpenaStore upenaStore) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
    }

    @Override
    public String getRootPath() {
        return "/ui/permissions";
    }

    public static class PermissionsPluginRegionInput implements PluginInput {

        final String key;
        final String permission;
        final String description;
        final String action;

        public PermissionsPluginRegionInput(String key, String permission, String description, String action) {
            this.key = key;
            this.permission = permission;
            this.description = description;
            this.action = action;
        }

        @Override
        public String name() {
            return "Permissions";
        }

    }

    @Override
    public String render(String user, PermissionsPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();
        if (SecurityUtils.getSubject().isPermitted("write")) {
            data.put("readWrite", true);
        }
        try {
            Map<ServiceKey, String> serviceColor = ServiceColorUtil.serviceKeysColor(upenaStore);

            Map<String, String> filters = new HashMap<>();
            filters.put("permission", input.permission);
            filters.put("description", input.description);
            data.put("filters", filters);

            PermissionFilter filter = new PermissionFilter(null, null, 0, 100_000);
            if (input.action != null) {
                if (input.action.equals("filter")) {
                    SecurityUtils.getSubject().checkPermissions("read");
                    filter = handleFilter(input, data);
                } else if (input.action.equals("add")) {
                    SecurityUtils.getSubject().checkPermissions("write");
                    handleAdd(user, filters, input, data);
                } else if (input.action.equals("update")) {
                    SecurityUtils.getSubject().checkPermissions("write");
                    handleUpdate(user, filters, input, data);
                } else if (input.action.equals("remove")) {
                    SecurityUtils.getSubject().checkPermissions("write");
                    handleRemove(user, input, data);
                }
            }

            List<Map<String, String>> rows = new ArrayList<>();
            Map<PermissionKey, TimestampedValue<Permission>> found = upenaStore.permissions.find(false, filter);
            for (Map.Entry<PermissionKey, TimestampedValue<Permission>> entrySet : found.entrySet()) {
                PermissionKey key = entrySet.getKey();
                TimestampedValue<Permission> timestampedValue = entrySet.getValue();
                Permission value = timestampedValue.getValue();


                Map<String, String> row = new HashMap<>();
                row.put("key", key.getKey());
                row.put("permission", value.permission);
                row.put("description", value.description);
                rows.add(row);
            }

            Collections.sort(rows, (Map<String, String> o1, Map<String, String> o2) -> {
                String serviceName1 = o1.get("permission");
                String serviceName2 = o2.get("permission");

                int c = serviceName1.compareTo(serviceName2);
                if (c != 0) {
                    return c;
                }
                return c;
            });

            data.put("permissions", rows);
        } catch (AuthorizationException a) {
            throw a;
        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    private PermissionFilter handleFilter(PermissionsPluginRegionInput input, Map<String, Object> data) {
        PermissionFilter filter;
        filter = new PermissionFilter(
            input.permission.isEmpty() ? null : input.permission,
            input.description.isEmpty() ? null : input.description,
            0, 100_000);
        data.put("message", "Filtering: permission.contains '" + input.permission + "' description.contains '" + input.description + "'");
        return filter;
    }

    private void handleAdd(String user, Map<String, String> filters, PermissionsPluginRegionInput input, Map<String, Object> data) {
        filters.clear();
        try {
            if (SecurityUtils.getSubject().isPermitted(input.permission)) {
                Permission create = new Permission(input.permission, input.description);
                upenaStore.permissions.update(null, create);

                data.put("message", "Created permission:" + input.permission);
                upenaStore.record(user, "added", System.currentTimeMillis(), "", "permissions-ui", create.toString());
            } else {
                data.put("message", "Sorry but you don't have adequate permissions to create permission:"+input.permission);
            }
        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error while trying to add permission:" + input.permission + "\n" + trace);
        }
    }

    private void handleUpdate(String user, Map<String, String> filters, PermissionsPluginRegionInput input, Map<String, Object> data) {
        filters.clear();
        try {
            Permission permission = upenaStore.permissions.get(new PermissionKey(input.key));
            if (permission == null) {
                data.put("message", "Update failed. No existing permission. Someone may have removed it since your last refresh.");
            } else {
                if (SecurityUtils.getSubject().isPermitted(input.permission)) {
                    Permission update = new Permission(input.permission, input.description);
                    upenaStore.permissions.update(new PermissionKey(input.key), update);
                    upenaStore.record(user, "updated", System.currentTimeMillis(), "", "permissions-ui", update.toString());
                    data.put("message", "Updated permission:" + input.permission);
                }
            }
        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error while trying to add Service:" + input.permission + "\n" + trace);
        }
    }

    private void handleRemove(String user, PermissionsPluginRegionInput input, Map<String, Object> data) {
        if (input.key.isEmpty()) {
            data.put("message", "Failed to remove permission:" + input.permission);
        } else {
            try {
                if (SecurityUtils.getSubject().isPermitted(input.permission)) {
                    PermissionKey key = new PermissionKey(input.key);
                    Permission removing = upenaStore.permissions.get(key);
                    if (removing != null) {
                        upenaStore.permissions.remove(key);
                        upenaStore.record(user, "removed", System.currentTimeMillis(), "", "permissions-ui", removing.toString());
                    }
                }

            } catch (Exception x) {
                String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                data.put("message", "Error while trying to remove permission:" + input.permission + "\n" + trace);
            }
        }
    }

    @Override
    public String getTitle() {
        return "Upena Permissions";
    }


}
