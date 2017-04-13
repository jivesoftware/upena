package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.UsersPluginRegion.UsersPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.PermissionKeyProvider;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Permission;
import com.jivesoftware.os.upena.shared.PermissionKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import com.jivesoftware.os.upena.shared.User;
import com.jivesoftware.os.upena.shared.UserFilter;
import com.jivesoftware.os.upena.shared.UserKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;

/**
 *
 */
// soy.page.usersPluginRegion
public class UsersPluginRegion implements PageRegion<UsersPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    public UsersPluginRegion(String template,
        SoyRenderer renderer,
        UpenaStore upenaStore) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
    }

    @Override
    public String getRootPath() {
        return "/ui/users";
    }

    public static class UsersPluginRegionInput implements PluginInput {

        final String key;
        final String name;
        final String email;
        final String action;

        public UsersPluginRegionInput(String key, String name, String email, String action) {
            this.key = key;
            this.name = name;
            this.email = email;
            this.action = action;
        }

        @Override
        public String name() {
            return "Users";
        }

    }

    @Override
    public String render(String user, UsersPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();
        if (SecurityUtils.getSubject().isPermitted("write")) {
            data.put("readWrite", true);
        }
        try {

            Map<String, String> filters = new HashMap<>();
            filters.put("name", input.name);
            filters.put("email", input.email);
            data.put("filters", filters);

            UserFilter filter = new UserFilter(null, null, 0, 100_000);
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

            List<Map<String, Object>> rows = new ArrayList<>();
            Map<UserKey, TimestampedValue<User>> found = upenaStore.users.find(false, filter);
            for (Map.Entry<UserKey, TimestampedValue<User>> entrySet : found.entrySet()) {
                UserKey key = entrySet.getKey();
                TimestampedValue<User> timestampedValue = entrySet.getValue();
                User value = timestampedValue.getValue();

                List<Map<String,String>> permissions = Lists.newArrayList();
                for (Entry<PermissionKey,Long> e : value.permissions.entrySet()) {
                    Permission permission = upenaStore.permissions.get(e.getKey());
                    if (permission != null) {
                        String expiration = "never";
                        if ( e.getValue() != null && e.getValue() != Long.MAX_VALUE) {
                            if (e.getValue() <= System.currentTimeMillis()) {
                                expiration = "expired";
                            } else {
                                expiration = DurationFormatUtils.formatDurationHMS(e.getValue() - System.currentTimeMillis());
                            }
                        }

                        Map<String,String> map = ImmutableMap.of("permission",permission.permission,
                            "description",permission.description,
                            "expiration", expiration);
                        permissions.add(map);
                    }
                }


                Map<String, Object> row = new HashMap<>();
                row.put("key", key.getKey());
                row.put("name", value.name);
                row.put("email", value.email);
                row.put("permissions", permissions);
                rows.add(row);
            }

            Collections.sort(rows, (Map<String, Object> o1, Map<String, Object> o2) -> {
                String name1 = (String)o1.get("name");
                String name2 = (String)o2.get("name");
                int c = name1.compareTo(name2);
                if (c != 0) {
                    return c;
                }
                return c;
            });

            data.put("users", rows);
        } catch (AuthorizationException a) {
            throw a;
        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    private UserFilter handleFilter(UsersPluginRegionInput input, Map<String, Object> data) {
        UserFilter filter;
        filter = new UserFilter(
            input.name.isEmpty() ? null : input.name,
            input.email.isEmpty() ? null : input.email,
            0, 100_000);
        data.put("message", "Filtering: name.contains '" + input.name + "' email.contains '" + input.email + "'");
        return filter;
    }

    private void handleAdd(String user, Map<String, String> filters, UsersPluginRegionInput input, Map<String, Object> data) {
        filters.clear();
        try {
            User newUser = new User(input.name, input.email, new ConcurrentHashMap<>());
            upenaStore.users.update(null, newUser);

            data.put("message", "Created User:" + input.name);
            upenaStore.recordChange(user, "added", System.currentTimeMillis(), "", "user-ui", newUser.toString());
        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error while trying to add User:" + input.name + "\n" + trace);
        }
    }

    private void handleUpdate(String user, Map<String, String> filters, UsersPluginRegionInput input, Map<String, Object> data) {
        filters.clear();
        try {
            User had = upenaStore.users.get(new UserKey(input.key));
            if (had == null) {
                data.put("message", "Update failed. No existing user. Someone may have removed it since your last refresh.");
            } else {
                User update = new User(input.name, input.email, had.permissions);
                upenaStore.users.update(new UserKey(input.key), update);
                upenaStore.recordChange(user, "updated", System.currentTimeMillis(), "", "user-ui", update.toString());
                data.put("message", "Updated user:" + input.name);
            }
        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error while trying to update User:" + input.name + "\n" + trace);
        }
    }

    private void handleRemove(String user, UsersPluginRegionInput input, Map<String, Object> data) {
        if (input.key.isEmpty()) {
            data.put("message", "Failed to remove User:" + input.name);
        } else {
            try {
                UserKey key = new UserKey(input.key);
                User had = upenaStore.users.get(key);
                if (had != null) {
                    upenaStore.users.remove(key);
                    upenaStore.recordChange(user, "removed", System.currentTimeMillis(), "", "user-ui", had.toString());
                }
            } catch (Exception x) {
                String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                data.put("message", "Error while trying to remove User:" + input.name + "\n" + trace);
            }
        }
    }


    public void add(String remoteUser, PermissionUpdate update) throws Exception {
        UserKey key = new UserKey(update.userKey);
        User user = upenaStore.users.get(key);
        if (user != null) {
            PermissionKey permissionKey = PermissionKeyProvider.forgePermissionKey(update.permission);
            Permission permission = upenaStore.permissions.get(permissionKey);
            if (permission == null) {
                if (SecurityUtils.getSubject().isPermitted(update.permission)) {

                }
                // TODO forge permission if current user has permission
            } else {
                // TODO ensure current user has permission to grant permission
            }
            if (permission != null) {
                user.permissions.put(permissionKey, expiration(update.expiration));
                upenaStore.users.update(key, user);
            }
        }
    }

    private long expiration(String expiration) {
        if (expiration == null || expiration.equals("never")) {
            return Long.MAX_VALUE;
        }
        return Long.parseLong(expiration);
    }

    public void remove(String remoteUser, PermissionUpdate update) throws Exception {
        UserKey key = new UserKey(update.userKey);
        User user = upenaStore.users.get(key);
        if (user != null) {
            PermissionKey permissionKey = PermissionKeyProvider.forgePermissionKey(update.permission);
            Long removed = user.permissions.remove(permissionKey);
            if (removed != null) {
                upenaStore.users.update(key, user);
            }
        }
    }

    public static class PermissionUpdate {

        public String userKey;
        public String permission;
        public String expiration;

        public PermissionUpdate() {
        }

        public PermissionUpdate(String userKey, String permission, String expiration) {
            this.userKey = userKey;
            this.permission = permission;
            this.expiration = expiration;
        }

        @Override
        public String toString() {
            return "PermissionUpdate{" + "userKey=" + userKey + ", permission=" + permission + ", expiration=" + expiration + '}';
        }

    }

    @Override
    public String getTitle() {
        return "Upena Users";
    }


}
