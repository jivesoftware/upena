package com.jivesoftware.os.upena.deployable.region;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.ServicesPluginRegion.ServicesPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceFilter;
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
public class ServicesPluginRegion implements PageRegion<ServicesPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final ObjectMapper mapper;
    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    public ServicesPluginRegion(ObjectMapper mapper,
        String template,
        SoyRenderer renderer,
        UpenaStore upenaStore) {
        this.mapper = mapper;
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
    }

    @Override
    public String getRootPath() {
        return "/ui/services";
    }

    public static class ServicesPluginRegionInput implements PluginInput {

        final String key;
        final String name;
        final String description;
        final String action;

        public ServicesPluginRegionInput(String key, String name, String description, String action) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.action = action;
        }

        @Override
        public String name() {
            return "Services";
        }

    }

    @Override
    public String render(String user, ServicesPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();
        if (SecurityUtils.getSubject().hasRole("readwrite")) {
            data.put("readWrite", true);
        }
        try {
            Map<ServiceKey, String> serviceColor = ServiceColorUtil.serviceKeysColor(upenaStore);

            Map<String, String> filters = new HashMap<>();
            filters.put("name", input.name);
            filters.put("description", input.description);
            data.put("filters", filters);

            ServiceFilter filter = new ServiceFilter(null, null, 0, 100_000);
            if (input.action != null) {
                if (input.action.equals("filter")) {
                    filter = handleFilter(input, data);
                } else if (input.action.equals("add")) {
                    SecurityUtils.getSubject().checkRoles("write");
                    handleAdd(user, filters, input, data);
                } else if (input.action.equals("update")) {
                    SecurityUtils.getSubject().checkRoles("write");
                    handleUpdate(user, filters, input, data);
                } else if (input.action.equals("remove")) {
                    SecurityUtils.getSubject().checkRoles("write");
                    handleRemove(user, input, data);
                }
            }

            List<Map<String, String>> rows = new ArrayList<>();
            Map<ServiceKey, TimestampedValue<Service>> found = upenaStore.services.find(false, filter);
            for (Map.Entry<ServiceKey, TimestampedValue<Service>> entrySet : found.entrySet()) {
                ServiceKey key = entrySet.getKey();
                TimestampedValue<Service> timestampedValue = entrySet.getValue();
                Service value = timestampedValue.getValue();

                InstanceFilter instanceFilter = new InstanceFilter(
                    null,
                    null,
                    key,
                    null,
                    null,
                    0, 100_000);

                Map<InstanceKey, TimestampedValue<Instance>> instances = upenaStore.instances.find(false, instanceFilter);

                Map<String, String> row = new HashMap<>();
                row.put("instanceCount", String.valueOf(instances.size()));
                row.put("color", serviceColor.get(key));
                row.put("key", key.getKey());
                row.put("name", value.name);
                row.put("description", value.description);
                rows.add(row);
            }

            Collections.sort(rows, (Map<String, String> o1, Map<String, String> o2) -> {
                String serviceName1 = o1.get("name");
                String serviceName2 = o2.get("name");

                int c = serviceName1.compareTo(serviceName2);
                if (c != 0) {
                    return c;
                }
                return c;
            });

            data.put("services", rows);
        } catch (AuthorizationException a) {
            throw a;
        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    private ServiceFilter handleFilter(ServicesPluginRegionInput input, Map<String, Object> data) {
        ServiceFilter filter;
        filter = new ServiceFilter(
            input.name.isEmpty() ? null : input.name,
            input.description.isEmpty() ? null : input.description,
            0, 100_000);
        data.put("message", "Filtering: name.contains '" + input.name + "' description.contains '" + input.description + "'");
        return filter;
    }

    private void handleAdd(String user, Map<String, String> filters, ServicesPluginRegionInput input, Map<String, Object> data) {
        filters.clear();
        try {
            Service newService = new Service(input.name, input.description);
            upenaStore.services.update(null, newService);

            data.put("message", "Created Service:" + input.name);
            upenaStore.record(user, "added", System.currentTimeMillis(), "", "service-ui", newService.toString());
        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error while trying to add Service:" + input.name + "\n" + trace);
        }
    }

    private void handleUpdate(String user, Map<String, String> filters, ServicesPluginRegionInput input, Map<String, Object> data) {
        filters.clear();
        try {
            Service service = upenaStore.services.get(new ServiceKey(input.key));
            if (service == null) {
                data.put("message", "Update failed. No existing service. Someone may have removed it since your last refresh.");
            } else {
                Service update = new Service(input.name, input.description);
                upenaStore.services.update(new ServiceKey(input.key), update);
                upenaStore.record(user, "updated", System.currentTimeMillis(), "", "service-ui", update.toString());
                data.put("message", "Service Cluster:" + input.name);
            }
        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error while trying to add Service:" + input.name + "\n" + trace);
        }
    }

    private void handleRemove(String user, ServicesPluginRegionInput input, Map<String, Object> data) {
        if (input.key.isEmpty()) {
            data.put("message", "Failed to remove Service:" + input.name);
        } else {
            try {
                ServiceKey serviceKey = new ServiceKey(input.key);
                Service removing = upenaStore.services.get(serviceKey);
                if (removing != null) {
                    upenaStore.services.remove(serviceKey);
                    upenaStore.record(user, "updated", System.currentTimeMillis(), "", "service-ui", removing.toString());
                }

            } catch (Exception x) {
                String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                data.put("message", "Error while trying to remove Service:" + input.name + "\n" + trace);
            }
        }
    }

    @Override
    public String getTitle() {
        return "Upena Services";
    }

    public String doExport(ServicesPluginRegionInput input, String user) {
        try {
            ServiceFilter filter;
            filter = new ServiceFilter(
                input.name.isEmpty() ? null : input.name,
                input.description.isEmpty() ? null : input.description,
                0, 100_000);

            ListOfService values = new ListOfService();
            Map<ServiceKey, TimestampedValue<Service>> found = upenaStore.services.find(false, filter);
            for (Map.Entry<ServiceKey, TimestampedValue<Service>> entrySet : found.entrySet()) {
                TimestampedValue<Service> timestampedValue = entrySet.getValue();
                Service value = timestampedValue.getValue();
                values.add(value);
            }

            return mapper.writeValueAsString(values);
        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
            return e.toString();
        }
    }

    public String doImport(String in, String user) {
        Map<String, Object> data = Maps.newHashMap();
        try {
            ListOfService values = mapper.readValue(in, ListOfService.class);

            for (Service value : values) {
                upenaStore.services.update(null, value);

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

    static class ListOfService extends ArrayList<Service> {
    }
}
