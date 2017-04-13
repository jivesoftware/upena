/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.ChaosStateGenerator.PartitionStrategy;
import com.jivesoftware.os.upena.service.ChaosStateHelper;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.ChaosState;
import com.jivesoftware.os.upena.shared.ChaosStateFilter;
import com.jivesoftware.os.upena.shared.ChaosStateKey;
import com.jivesoftware.os.upena.shared.ChaosStrategyKey;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Monkey;
import com.jivesoftware.os.upena.shared.MonkeyFilter;
import com.jivesoftware.os.upena.shared.MonkeyKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentNavigableMap;
import org.apache.shiro.SecurityUtils;

// soy.page.monkeyPluginRegion
public class MonkeyPluginRegion implements PageRegion<MonkeyPluginRegion.MonkeyPluginRegionInput> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    public MonkeyPluginRegion(String template,
                              SoyRenderer renderer,
                              UpenaStore upenaStore) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
    }

    @Override
    public String getRootPath() {
        return "/ui/chaos";
    }

    @Override
    public String getTitle() {
        return "Upena Chaos";
    }

    public static class MonkeyPluginRegionInput implements PluginInput {

        final String key;
        final boolean enabled;
        final String clusterKey;
        final String cluster;
        final String hostKey;
        final String host;
        final String serviceKey;
        final String service;
        final String strategyKey;
        final String strategy;
        final String action;

        public MonkeyPluginRegionInput(String key,
                                       boolean enabled,
                                       String clusterKey,
                                       String cluster,
                                       String hostKey,
                                       String host,
                                       String serviceKey,
                                       String service,
                                       String strategyKey,
                                       String strategy,
                                       String action) {
            this.key = key;
            this.clusterKey = clusterKey;
            this.cluster = cluster;
            this.hostKey = hostKey;
            this.host = host;
            this.serviceKey = serviceKey;
            this.service = service;
            this.strategyKey = strategyKey;
            this.strategy = strategy;
            this.enabled = enabled;
            this.action = action;
        }

        @Override
        public String name() {
            return "Upena Chaos";
        }

    }

    @Override
    public String render(String user, MonkeyPluginRegionInput input) {

        SecurityUtils.getSubject().checkPermission("debug");
        Map<String, Object> data = Maps.newHashMap();

        try {
            MonkeyFilter filter = new MonkeyFilter(
                    input.clusterKey.isEmpty() ? null : new ClusterKey(input.clusterKey),
                    input.hostKey.isEmpty() ? null : new HostKey(input.hostKey),
                    input.serviceKey.isEmpty() ? null : new ServiceKey(input.serviceKey),
                    input.strategyKey.isEmpty() ? null : ChaosStrategyKey.valueOf(input.strategyKey),
                    0, 100_000);

            if (input.action.equals("filter")) {
                handleFilter(input, data);
            } else if (input.action.equals("add")) {
                handleAdd(user, input, data);
            } else if (input.action.equals("update")) {
                handleUpdate(user, input, data);
            } else if (input.action.equals("remove")) {
                handleRemove(user, input, data);
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            Map<MonkeyKey, TimestampedValue<Monkey>> foundMonkeys = upenaStore.monkeys.find(false, filter);
            for (Map.Entry<MonkeyKey, TimestampedValue<Monkey>> entrySet : foundMonkeys.entrySet()) {
                MonkeyKey key = entrySet.getKey();
                TimestampedValue<Monkey> timestampedValue = entrySet.getValue();
                rows.add(clusterToMap(key, timestampedValue));
            }

            Collections.sort(rows, (o1, o2) -> {
                String clusterName1 = (String) ((Map) o1.get("cluster")).get("name");
                String clusterName2 = (String) ((Map) o2.get("cluster")).get("name");
                int c = clusterName1.compareTo(clusterName2);
                if (c != 0) {
                    return c;
                }

                String hostName1 = "";
                Map mHost1 = (Map) o1.get("host");
                if (mHost1 != null) {
                    hostName1 = (String) mHost1.get("name");
                }
                String hostName2 = "";
                Map mHost2 = (Map) o2.get("host");
                if (mHost2 != null) {
                    hostName2 = (String) mHost2.get("name");
                }
                c = hostName1.compareTo(hostName2);
                if (c != 0) {
                    return c;
                }

                String serviceName1 = (String) ((Map) o1.get("service")).get("name");
                String serviceName2 = (String) ((Map) o2.get("service")).get("name");
                c = serviceName1.compareTo(serviceName2);
                if (c != 0) {
                    return c;
                }

                String strategyName1 = (String) ((Map) o1.get("strategy")).get("name");
                String strategyName2 = (String) ((Map) o2.get("strategy")).get("name");
                return strategyName1.compareTo(strategyName2);
            });

            data.put("monkeys", rows);
        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    private void handleFilter(MonkeyPluginRegionInput input, Map<String, Object> data) {
        data.put("message", "Filtering: "
                + "enabled.equals '" + input.enabled + "' "
                + "cluster.equals '" + input.cluster + "' "
                + "host.equals '" + input.host + "' "
                + "service.equals '" + input.service + "' "
                + "strategy.equals '" + input.strategy + "'"
        );
    }

    private void handleAdd(String user, MonkeyPluginRegionInput input, Map<String, Object> data) {
        try {
            boolean valid = true;

            Cluster cluster = upenaStore.clusters.get(new ClusterKey(input.clusterKey));
            if (cluster == null) {
                data.put("message", "Cluster key:" + input.clusterKey + " is invalid.");
                valid = false;
            }
            Service service = upenaStore.services.get(new ServiceKey(input.serviceKey));
            if (service == null) {
                data.put("message", "Service key:" + input.serviceKey + " is invalid.");
                valid = false;
            }
            if (input.strategyKey.isEmpty()) {
                data.put("message", "Strategy key is empty.");
                valid = false;
            }
            Host host = upenaStore.hosts.get(new HostKey(input.hostKey));
            if (host == null) {
                if (!input.strategyKey.isEmpty() &&
                        ChaosStrategyKey.valueOf(input.strategyKey) != ChaosStrategyKey.SPLIT_BRAIN &&
                        ChaosStrategyKey.valueOf(input.strategyKey) != ChaosStrategyKey.RANDOM_NETWORK_PARTITION &&
                        ChaosStrategyKey.valueOf(input.strategyKey) != ChaosStrategyKey.ADHOC_NETWORK_PARTITION) {
                    data.put("message", "Host key:" + input.hostKey + " is invalid.");
                    valid = false;
                }
            } else {
                if (!input.strategyKey.isEmpty() &&
                        (ChaosStrategyKey.valueOf(input.strategyKey) == ChaosStrategyKey.SPLIT_BRAIN ||
                                ChaosStrategyKey.valueOf(input.strategyKey) == ChaosStrategyKey.RANDOM_NETWORK_PARTITION ||
                                ChaosStrategyKey.valueOf(input.strategyKey) == ChaosStrategyKey.ADHOC_NETWORK_PARTITION)) {
                    data.put("message", "Host not applicable.");
                    valid = false;
                }
            }

            if (valid) {
                ChaosStrategyKey chaosStrategyKey = ChaosStrategyKey.valueOf(input.strategyKey);

                Monkey newMonkey = new Monkey(
                        input.enabled,
                        new ClusterKey(input.clusterKey),
                        input.hostKey.isEmpty() ? null : new HostKey(input.hostKey),
                        new ServiceKey(input.serviceKey),
                        ChaosStrategyKey.valueOf(input.strategyKey));

                if (chaosStrategyKey == ChaosStrategyKey.RANDOM_NETWORK_PARTITION ||
                        chaosStrategyKey == ChaosStrategyKey.ADHOC_NETWORK_PARTITION) {
                    Map<String, String> properties = new HashMap<>();
                    properties.put(ChaosStateHelper.PARTITION_TYPE, PartitionStrategy.HALVE.toString());
                    properties.put(ChaosStateHelper.PARTITION_INTERVAL, "600000");
                    newMonkey.properties = properties;
                }

                upenaStore.monkeys.update(null, newMonkey);
                upenaStore.recordChange(user, "added", System.currentTimeMillis(), "", "monkey-ui",
                        monkeyToHumanReadableString(newMonkey) + "\n" + newMonkey.toString());

                data.put("message", "Created Monkey.");
            }
        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error while trying to add Monkey.\n" + trace);
        }
    }

    private void handleUpdate(String user, MonkeyPluginRegionInput input, Map<String, Object> data) {
        try {
            Monkey monkey = upenaStore.monkeys.get(new MonkeyKey(input.key));

            if (monkey == null) {
                data.put("message", "Could not update. No existing monkey. Someone may have removed it since your last refresh.");
            } else {
                boolean valid = true;

                ClusterKey clusterKey = new ClusterKey(input.clusterKey);
                Cluster cluster = upenaStore.clusters.get(clusterKey);
                if (cluster == null) {
                    data.put("message", "Cluster key:" + input.clusterKey + " is invalid.");
                    valid = false;
                }
                Service service = upenaStore.services.get(new ServiceKey(input.serviceKey));
                if (service == null) {
                    data.put("message", "Service key:" + input.serviceKey + " is invalid.");
                    valid = false;
                }
                if (input.strategyKey.isEmpty()) {
                    data.put("message", "Strategy key is empty.");
                    valid = false;
                }
                Host host = upenaStore.hosts.get(new HostKey(input.hostKey));
                if (host == null) {
                    if (!input.strategyKey.isEmpty() &&
                            ChaosStrategyKey.valueOf(input.strategyKey) != ChaosStrategyKey.SPLIT_BRAIN &&
                            ChaosStrategyKey.valueOf(input.strategyKey) != ChaosStrategyKey.RANDOM_NETWORK_PARTITION &&
                            ChaosStrategyKey.valueOf(input.strategyKey) != ChaosStrategyKey.ADHOC_NETWORK_PARTITION) {
                        data.put("message", "Host key:" + input.hostKey + " is invalid.");
                        valid = false;
                    }
                } else {
                    if (!input.strategyKey.isEmpty() &&
                            (ChaosStrategyKey.valueOf(input.strategyKey) == ChaosStrategyKey.SPLIT_BRAIN ||
                                    ChaosStrategyKey.valueOf(input.strategyKey) == ChaosStrategyKey.RANDOM_NETWORK_PARTITION ||
                                    ChaosStrategyKey.valueOf(input.strategyKey) == ChaosStrategyKey.ADHOC_NETWORK_PARTITION)) {
                        data.put("message", "Host not applicable.");
                        valid = false;
                    }
                }

                if (valid) {
                    MonkeyKey monkeyKey = new MonkeyKey(input.key);
                    Monkey existingMonkey = upenaStore.monkeys.get(monkeyKey);

                    Monkey updatedMonkey = new Monkey(
                            input.enabled,
                            new ClusterKey(input.clusterKey),
                            new HostKey(input.hostKey),
                            new ServiceKey(input.serviceKey),
                            ChaosStrategyKey.valueOf(input.strategyKey));
                    updatedMonkey.properties = existingMonkey.properties;

                    upenaStore.monkeys.update(new MonkeyKey(input.key), updatedMonkey);

                    if (!input.enabled) {
                        ConcurrentNavigableMap<ChaosStateKey, TimestampedValue<ChaosState>> gotChaosStates =
                                upenaStore.chaosStates.find(false, new ChaosStateFilter(
                                        updatedMonkey.serviceKey,
                                        0, Integer.MAX_VALUE));
                        for (Entry<ChaosStateKey, TimestampedValue<ChaosState>> entry : gotChaosStates.entrySet()) {
                            upenaStore.chaosStates.remove(entry.getKey());
                        }
                    }

                    upenaStore.recordChange(user, "updated", System.currentTimeMillis(), "", "monkey-ui",
                            monkeyToHumanReadableString(monkey) + "\n"
                                    + updatedMonkey.toString());

                    data.put("message", "Updated Monkey:" + input.key);
                }
            }
        } catch (Exception x) {
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error while trying to update Monkey:" + input.key + "\n" + trace);
        }
    }

    private void handleRemove(String user, MonkeyPluginRegionInput input, Map<String, Object> data) {
        if (input.key.isEmpty()) {
            data.put("message", "Failed to remove Monkey:" + input.key);
        } else {
            try {
                MonkeyKey monkeyKey = new MonkeyKey(input.key);
                Monkey removing = upenaStore.monkeys.get(monkeyKey);
                if (removing != null) {
                    upenaStore.monkeys.remove(monkeyKey);

                    ConcurrentNavigableMap<ChaosStateKey, TimestampedValue<ChaosState>> gotChaosStates =
                            upenaStore.chaosStates.find(false, new ChaosStateFilter(
                                    removing.serviceKey,
                                    0, Integer.MAX_VALUE));
                    for (Entry<ChaosStateKey, TimestampedValue<ChaosState>> entry : gotChaosStates.entrySet()) {
                        upenaStore.chaosStates.remove(entry.getKey());
                    }

                    upenaStore.recordChange(user, "removed", System.currentTimeMillis(), "", "monkey-ui", removing.toString());
                }
            } catch (Exception x) {
                String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                data.put("message", "Error while trying to remove Monkey:" + input.key + "\n" + trace);
            }
        }
    }

    private Map<String, Object> clusterToMap(MonkeyKey key,
                                             TimestampedValue<Monkey> timestampedValue) throws Exception {
        Map<String, Object> map = new HashMap<>();

        Monkey value = timestampedValue.getValue();
        map.put("key", key.getKey());

        map.put("enabled", value.enabled);

        Cluster cluster = upenaStore.clusters.get(value.clusterKey);
        Host host = upenaStore.hosts.get(value.hostKey);
        Service service = upenaStore.services.get(value.serviceKey);

        map.put("cluster", ImmutableMap.of(
                "key", value.clusterKey.getKey(),
                "name", cluster != null ? cluster.name : "unknownCluster"));

        String hostName = "";
        if (host != null) {
            hostName = host.hostName + "/" + host.name;
            if (host.name.equals(host.hostName)) {
                hostName = host.hostName;
            }
        }
        map.put("host", ImmutableMap.of(
                "key", (value.hostKey != null) ? value.hostKey.getKey() : "",
                "name", hostName));

        map.put("service", ImmutableMap.of(
                "key", value.serviceKey.getKey(),
                "name", service != null ? service.name : "unknownService"));

        map.put("strategy", ImmutableMap.of(
                "key", value.strategyKey.name(),
                "name", value.strategyKey.description));

        if (value.properties != null) {
            List<Map<String, String>> properties = new ArrayList<>();
            for (Entry<String, String> entry : value.properties.entrySet()) {
                properties.add(ImmutableMap.of("name", entry.getKey(), "value", entry.getValue()));
            }
            map.put("properties", properties);
        }

        return map;
    }

    private String monkeyToHumanReadableString(Monkey monkey) throws Exception {
        Cluster cluster = upenaStore.clusters.get(monkey.clusterKey);
        Host host = upenaStore.hosts.get(monkey.hostKey);
        Service service = upenaStore.services.get(monkey.serviceKey);
        return ((cluster == null) ? "unknownCluster" : cluster.name) + "/"
                + ((host == null) ? "unknownHost" : host.name) + "/"
                + ((service == null) ? "unknownService" : service.name) + "/"
                + monkey.strategyKey.description;
    }

    public void add(MonkeyPropertyUpdate update) throws Exception {
        SecurityUtils.getSubject().checkPermission("debug");
        MonkeyKey monkeyKey = new MonkeyKey(update.monkeyKey);
        Monkey monkey = upenaStore.monkeys.get(monkeyKey);
        if (monkey != null) {
            monkey.properties.put(update.name, update.value);
            upenaStore.monkeys.update(monkeyKey, monkey);

            ConcurrentNavigableMap<ChaosStateKey, TimestampedValue<ChaosState>> gotChaosStates =
                    upenaStore.chaosStates.find(false, new ChaosStateFilter(
                            monkey.serviceKey,
                            0, Integer.MAX_VALUE));
            for (Entry<ChaosStateKey, TimestampedValue<ChaosState>> entry : gotChaosStates.entrySet()) {
                upenaStore.chaosStates.remove(entry.getKey());
            }
        }
    }

    public void remove(MonkeyPropertyUpdate update) throws Exception {
        SecurityUtils.getSubject().checkPermission("debug");
        MonkeyKey monkeyKey = new MonkeyKey(update.monkeyKey);
        Monkey monkey = upenaStore.monkeys.get(monkeyKey);
        if (monkey != null) {
            monkey.properties.remove(update.name);
            upenaStore.monkeys.update(monkeyKey, monkey);

            ConcurrentNavigableMap<ChaosStateKey, TimestampedValue<ChaosState>> gotChaosStates =
                    upenaStore.chaosStates.find(false, new ChaosStateFilter(
                            monkey.serviceKey,
                            0, Integer.MAX_VALUE));
            for (Entry<ChaosStateKey, TimestampedValue<ChaosState>> entry : gotChaosStates.entrySet()) {
                upenaStore.chaosStates.remove(entry.getKey());
            }
        }
    }

    public static class MonkeyPropertyUpdate {

        public String monkeyKey;
        public String name;
        public String value;

        public MonkeyPropertyUpdate() {
        }

        public MonkeyPropertyUpdate(String monkeyKey, String name, String value) {
            this.monkeyKey = monkeyKey;
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return "MonkeyPropertyUpdate{" + "monkeyKey=" + monkeyKey + ", name=" + name + ", value=" + value + '}';
        }

    }

}
