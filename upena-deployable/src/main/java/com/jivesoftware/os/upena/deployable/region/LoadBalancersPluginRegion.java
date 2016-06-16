package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.LoadBalancersPluginRegion.LoadBalancersPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.LoadBalancer;
import com.jivesoftware.os.upena.shared.LoadBalancerFilter;
import com.jivesoftware.os.upena.shared.LoadBalancerKey;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
// soy.page.loadBalancersPluginRegion
public class LoadBalancersPluginRegion implements PageRegion<LoadBalancersPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    public LoadBalancersPluginRegion(String template,
        SoyRenderer renderer,
        UpenaStore upenaStore
    ) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
    }

    @Override
    public String getRootPath() {
        return "/ui/loadbalancers";
    }

    public static class LoadBalancersPluginRegionInput implements PluginInput {

        final String key;
        final String name;
        final String description;
        final String action;

        public LoadBalancersPluginRegionInput(String key, String name, String description, String action) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.action = action;
        }

        @Override
        public String name() {
            return "Load Balancers";
        }

    }

    @Override
    public String render(String user, LoadBalancersPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();

        try {

            Map<ServiceKey, String> serviceColor = ServiceColorUtil.serviceKeysColor(upenaStore);

            Map<String, String> filters = new HashMap<>();
            filters.put("name", input.name);
            filters.put("description", input.description);
            data.put("filters", filters);

            LoadBalancerFilter filter = new LoadBalancerFilter(null, null, 0, 10000);
            if (input.action != null) {
                if (input.action.equals("filter")) {
                    filter = new LoadBalancerFilter(
                        input.name.isEmpty() ? null : input.name,
                        input.description.isEmpty() ? null : input.description,
                        0, 10000);
                    data.put("message", "Filtering: name.contains '" + input.name + "' description.contains '" + input.description + "'");
                } else if (input.action.equals("add")) {
                    filters.clear();
                    try {
                        LoadBalancer newLoadBalancer = new LoadBalancer(input.name, input.description,
                            new HashMap<>());
                        upenaStore.loadBalancers.update(null, newLoadBalancer);
                        upenaStore.record(user, "added", System.currentTimeMillis(), "", "loadBalancers-ui", newLoadBalancer.toString());

                        data.put("message", "Created LoadBalancer:" + input.name);
                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to add LoadBalancer:" + input.name + "\n" + trace);
                    }
                } else if (input.action.equals("update")) {
                    filters.clear();
                    try {
                        LoadBalancer loadBalancer = upenaStore.loadBalancers.get(new LoadBalancerKey(input.key));
                        if (loadBalancer == null) {
                            data.put("message", "Couldn't update no existent loadBalancer. Someone else likely just removed it since your last refresh.");
                        } else {
                            LoadBalancer updatedLoadBalancer = new LoadBalancer(input.name, input.description,
                                loadBalancer.listeners);
                            upenaStore.loadBalancers.update(new LoadBalancerKey(input.key), updatedLoadBalancer);
                            data.put("message", "Updated LoadBalancer:" + input.name);
                            upenaStore.record(user, "updated", System.currentTimeMillis(), "", "loadBalancers-ui", loadBalancer.toString());
                        }
                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to add LoadBalancer:" + input.name + "\n" + trace);
                    }
                } else if (input.action.equals("remove")) {
                    if (input.key.isEmpty()) {
                        data.put("message", "Failed to remove LoadBalancer:" + input.name);
                    } else {
                        try {
                            LoadBalancerKey loadBalancerKey = new LoadBalancerKey(input.key);
                            LoadBalancer removing = upenaStore.loadBalancers.get(loadBalancerKey);
                            if (removing != null) {
                                upenaStore.loadBalancers.remove(loadBalancerKey);
                                upenaStore.record(user, "removed", System.currentTimeMillis(), "", "loadBalancers-ui", removing.toString());
                            }
                        } catch (Exception x) {
                            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                            data.put("message", "Error while trying to remove LoadBalancer:" + input.name + "\n" + trace);
                        }
                    }
                }
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            Map<LoadBalancerKey, TimestampedValue<LoadBalancer>> found = upenaStore.loadBalancers.find(filter);
            for (Map.Entry<LoadBalancerKey, TimestampedValue<LoadBalancer>> entrySet : found.entrySet()) {
                LoadBalancerKey key = entrySet.getKey();
                TimestampedValue<LoadBalancer> timestampedValue = entrySet.getValue();
                LoadBalancer value = timestampedValue.getValue();

                LoadBalancerFilter loadBalancerFilter = new LoadBalancerFilter(
                    null,
                    null,
                    0, 10000);

                HashMultiset<ServiceKey> serviceKeyCount = HashMultiset.create();
                Map<LoadBalancerKey, TimestampedValue<LoadBalancer>> instances = upenaStore.loadBalancers.find(loadBalancerFilter);
                
                

                Map<String, Object> row = new HashMap<>();
                row.put("key", key.getKey());
                row.put("name", value.name);
                row.put("description", value.description);
                rows.add(row);
            }

            Collections.sort(rows, (Map<String, Object> o1, Map<String, Object> o2) -> {
                String loadBalancerName1 = (String) o1.get("name");
                String loadBalancerName2 = (String) o2.get("name");

                int c = loadBalancerName1.compareTo(loadBalancerName2);
                if (c != 0) {
                    return c;
                }
                return c;
            });

            data.put("loadBalancers", rows);

        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "LoadBalancers";
    }


    public void add(String user, ListenerUpdate listenerUpdate) throws Exception {
//        LoadBalancerKey loadBalancerKey = new LoadBalancerKey(releaseGroupUpdate.loadBalancerId);
//        LoadBalancer loadBalancer = upenaStore.loadBalancers.get(loadBalancerKey);
//        if (loadBalancer != null) {
//            loadBalancer.defaultReleaseGroups.put(new ServiceKey(releaseGroupUpdate.serviceId), new ReleaseGroupKey(releaseGroupUpdate.releaseGroupId));
//            upenaStore.loadBalancers.update(loadBalancerKey, loadBalancer);
//            upenaStore.record(user, "updated", System.currentTimeMillis(), "", "loadBalancers-ui", loadBalancer.toString());
//        }
    }

    public void remove(String user, ListenerUpdate listenerUpdate) throws Exception {
//        LoadBalancerKey loadBalancerKey = new LoadBalancerKey(releaseGroupUpdate.loadBalancerId);
//        LoadBalancer loadBalancer = upenaStore.loadBalancers.get(loadBalancerKey);
//        if (loadBalancer != null) {
//            if (loadBalancer.defaultReleaseGroups.remove(new ServiceKey(releaseGroupUpdate.serviceId)) != null) {
//                upenaStore.loadBalancers.update(loadBalancerKey, loadBalancer);
//                upenaStore.record(user, "updated", System.currentTimeMillis(), "", "loadBalancers-ui", loadBalancer.toString());
//            }
//        }
    }

    public static class ListenerUpdate {

        public String loadBalancerId;
        public String serviceId;
        public String releaseGroupId;

        public ListenerUpdate() {
        }

        public ListenerUpdate(String loadBalancerId, String serviceId, String releaseGroupId) {
            this.loadBalancerId = loadBalancerId;
            this.serviceId = serviceId;
            this.releaseGroupId = releaseGroupId;
        }

    }

}
