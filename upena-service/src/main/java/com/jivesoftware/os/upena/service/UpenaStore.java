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
package com.jivesoftware.os.upena.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.jive.utils.ordered.id.OrderIdProvider;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.InstanceChanged;
import com.jivesoftware.os.routing.bird.shared.TenantChanged;
import com.jivesoftware.os.upena.amza.service.AmzaService;
import com.jivesoftware.os.upena.amza.service.AmzaTable;
import com.jivesoftware.os.upena.amza.shared.RowIndexKey;
import com.jivesoftware.os.upena.amza.shared.TableName;
import com.jivesoftware.os.upena.shared.ChaosState;
import com.jivesoftware.os.upena.shared.ChaosStateKey;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.LB;
import com.jivesoftware.os.upena.shared.LBKey;
import com.jivesoftware.os.upena.shared.Monkey;
import com.jivesoftware.os.upena.shared.MonkeyKey;
import com.jivesoftware.os.upena.shared.Project;
import com.jivesoftware.os.upena.shared.ProjectKey;
import com.jivesoftware.os.upena.shared.RecordedChange;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.Tenant;
import com.jivesoftware.os.upena.shared.TenantKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UpenaStore {

    private final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final OrderIdProvider idProvider;
    private final ObjectMapper mapper;
    private final AmzaService amzaService;

    private final InstanceChanges instanceChanges;
    private final InstanceChanges instanceRemoved;
    private final TenantChanges tenantChanges;

    public final TableName projectStoreKey = new TableName("master", "projects", null, null);
    public final TableName clusterStoreKey = new TableName("master", "clusters", null, null);
    public final TableName loadbalancers = new TableName("master", "loadbalancers", null, null);
    public final TableName hostStoreKey = new TableName("master", "hosts", null, null);
    public final TableName serviceStoreKey = new TableName("master", "services", null, null);
    public final TableName releaseGroupStoreKey = new TableName("master", "releaseGroups", null, null);
    public final TableName instanceStoreKey = new TableName("master", "intances", null, null);
    public final TableName tenantStoreKey = new TableName("master", "tenants", null, null);
    public final TableName monkeyStoreKey = new TableName("master", "monkeys", null, null);
    public final TableName chaosStateStoreKey = new TableName("master", "chaosState", null, null);
    public final TableName changeLogStoreKey = new TableName("master", "changeLog", null, null);

    public final UpenaTable<ProjectKey, Project> projects;
    public final UpenaTable<ClusterKey, Cluster> clusters;
    public final UpenaTable<LBKey, LB> loadBalancers;
    public final UpenaTable<HostKey, Host> hosts;
    public final UpenaTable<ServiceKey, Service> services;
    public final UpenaTable<ReleaseGroupKey, ReleaseGroup> releaseGroups;
    public final UpenaTable<InstanceKey, Instance> instances;
    public final UpenaTable<TenantKey, Tenant> tenants;
    public final UpenaTable<MonkeyKey, Monkey> monkeys;
    public final UpenaTable<ChaosStateKey, ChaosState> chaosStates;

    public final AmzaTable changeLog;

    public UpenaStore(OrderIdProvider idProvider,
        ObjectMapper mapper,
        AmzaService amzaService,
        InstanceChanges instanceChanges,
        InstanceChanges instanceRemoved,
        TenantChanges tenantChanges,
        int minServicePort,
        int maxServicePort) throws Exception {

        this.idProvider = idProvider;
        this.mapper = mapper;
        this.amzaService = amzaService;
        this.instanceChanges = instanceChanges;
        this.instanceRemoved = instanceRemoved;
        this.tenantChanges = tenantChanges;

        projects = new UpenaTable<>(mapper, amzaService.getTable(projectStoreKey), ProjectKey.class, Project.class, new ProjectKeyProvider(idProvider), null);
        clusters = new UpenaTable<>(mapper, amzaService.getTable(clusterStoreKey), ClusterKey.class, Cluster.class, new ClusterKeyProvider(idProvider), null);
        loadBalancers = new UpenaTable<>(mapper, amzaService.getTable(loadbalancers), LBKey.class, LB.class, new LBKeyProvider(idProvider), null);
        hosts = new UpenaTable<>(mapper, amzaService.getTable(hostStoreKey), HostKey.class, Host.class, new HostKeyProvider(), null);
        services = new UpenaTable<>(mapper, amzaService.getTable(serviceStoreKey), ServiceKey.class, Service.class, new ServiceKeyProvider(idProvider), null);
        releaseGroups = new UpenaTable<>(mapper, amzaService.getTable(releaseGroupStoreKey),
            ReleaseGroupKey.class, ReleaseGroup.class, new ReleaseGroupKeyProvider(idProvider), null);
        instances = new UpenaTable<>(mapper, amzaService.getTable(instanceStoreKey),
            InstanceKey.class, Instance.class, new InstanceKeyProvider(idProvider), new InstanceValidator(minServicePort, maxServicePort));
        tenants = new UpenaTable<>(mapper, amzaService.getTable(tenantStoreKey), TenantKey.class, Tenant.class, new TenantKeyProvider(), null);
        monkeys = new UpenaTable<>(mapper, amzaService.getTable(monkeyStoreKey), MonkeyKey.class, Monkey.class, new MonkeyKeyProvider(idProvider), null);
        chaosStates = new UpenaTable<>(mapper, amzaService.getTable(chaosStateStoreKey), ChaosStateKey.class, ChaosState.class, new ChaosStateKeyProvider(
            idProvider), null);

        changeLog = amzaService.getTable(changeLogStoreKey);
    }

    public void record(String who, String what, long whenTimestampMillis, String why, String where, String how) throws Exception {
        if (who == null) {
            who = "null";
        }
        long descendingTimestamp = Long.MAX_VALUE - whenTimestampMillis;
        changeLog.set(new RowIndexKey(longBytes(descendingTimestamp, new byte[8], 0)),
            mapper.writeValueAsBytes(new RecordedChange(who, what, whenTimestampMillis, where, why, how)));
    }

    public void clearChangeLog() {
        changeLog.scan((key, value) -> {
            if (key != null) {
                changeLog.remove(key);
            }
            return true;
        });
    }

    public static byte[] longBytes(long v, byte[] _bytes, int _offset) {
        _bytes[_offset + 0] = (byte) (v >>> 56);
        _bytes[_offset + 1] = (byte) (v >>> 48);
        _bytes[_offset + 2] = (byte) (v >>> 40);
        _bytes[_offset + 3] = (byte) (v >>> 32);
        _bytes[_offset + 4] = (byte) (v >>> 24);
        _bytes[_offset + 5] = (byte) (v >>> 16);
        _bytes[_offset + 6] = (byte) (v >>> 8);
        _bytes[_offset + 7] = (byte) v;
        return _bytes;
    }

    public void log(long whenAgoElapseLargestMillis,
        long whenAgoElapseSmallestMillis,
        int minCount, //
        final String who,
        final String what,
        final String why,
        final String where,
        final String how,
        final LogStream logStream) throws Exception {
        long time = System.currentTimeMillis();
        final long maxTimestampInclusize = time - whenAgoElapseSmallestMillis;
        final long minTimestampExclusize = time - whenAgoElapseLargestMillis;

        final AtomicInteger count = new AtomicInteger(minCount);
        changeLog.scan((l, key, value) -> {
            RecordedChange change = mapper.readValue(value.getValue(), RecordedChange.class);
            if (change.when <= maxTimestampInclusize && (change.when > minTimestampExclusize || count.get() > 0)) {
                if (who != null && who.length() > 0 && !change.who.contains(who)) {
                    return true;
                }
                if (what != null && what.length() > 0 && !change.what.contains(what)) {
                    return true;
                }
                if (why != null && why.length() > 0 && !change.why.contains(why)) {
                    return true;
                }
                if (where != null && where.length() > 0 && !change.where.contains(where)) {
                    return true;
                }
                if (how != null && how.length() > 0 && !change.how.contains(how)) {
                    return true;
                }
                count.decrementAndGet();
                return logStream.stream(change);
            }
            return change.when > maxTimestampInclusize || count.get() > 0;
        });

    }

    public interface LogStream {

        boolean stream(RecordedChange change) throws Exception;
    }

    public void attachWatchers() throws Exception {
        amzaService.watch(clusterStoreKey, new UpenaStoreChanges<>(mapper, ClusterKey.class, Cluster.class,
            (key, value) -> {
                InstanceFilter impactedFilter = new InstanceFilter(key, null, null, null, null, 0, Integer.MAX_VALUE);
                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(false, impactedFilter);
                List<InstanceChanged> changes = new ArrayList<>();
                for (Entry<InstanceKey, TimestampedValue<Instance>> instance : got.entrySet()) {
                    changes.add(new InstanceChanged(instance.getValue().getValue().hostKey.getKey(), instance.getKey().getKey()));
                }
                instanceChanges.changed(changes);
            },
            (key, value) -> {
                InstanceFilter impactedFilter = new InstanceFilter(key, null, null, null, null, 0, Integer.MAX_VALUE);
                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(false, impactedFilter);
                for (Entry<InstanceKey, TimestampedValue<Instance>> e : got.entrySet()) {
                    LOG.info("Removing instance:" + e + " because cluster:" + value + " was removed.");
                    instances.remove(e.getKey());
                }
            }));
        amzaService.watch(hostStoreKey, new UpenaStoreChanges<>(mapper, HostKey.class, Host.class,
            (key, value) -> {
                InstanceFilter impactedFilter = new InstanceFilter(null, key, null, null, null, 0, Integer.MAX_VALUE);
                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(false, impactedFilter);
                List<InstanceChanged> changes = new ArrayList<>();
                for (Entry<InstanceKey, TimestampedValue<Instance>> instance : got.entrySet()) {
                    changes.add(new InstanceChanged(instance.getValue().getValue().hostKey.getKey(), instance.getKey().getKey()));
                }
                instanceChanges.changed(changes);
            },
            (key, value) -> {
                InstanceFilter impactedFilter = new InstanceFilter(null, key, null, null, null, 0, Integer.MAX_VALUE);
                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(false, impactedFilter);
                for (Entry<InstanceKey, TimestampedValue<Instance>> e : got.entrySet()) {
                    LOG.info("Removing instance:" + e + " because host:" + value + " was removed.");
                    instances.remove(e.getKey());
                }
            }));
        amzaService.watch(serviceStoreKey, new UpenaStoreChanges<>(mapper, ServiceKey.class, Service.class,
            (key, value) -> {
                InstanceFilter impactedFilter = new InstanceFilter(null, null, key, null, null, 0, Integer.MAX_VALUE);
                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(false, impactedFilter);
                List<InstanceChanged> changes = new ArrayList<>();
                for (Entry<InstanceKey, TimestampedValue<Instance>> instance : got.entrySet()) {
                    changes.add(new InstanceChanged(instance.getValue().getValue().hostKey.getKey(), instance.getKey().getKey()));
                }
                instanceChanges.changed(changes);
            },
            (key, value) -> {
                InstanceFilter impactedFilter = new InstanceFilter(null, null, key, null, null, 0, Integer.MAX_VALUE);
                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(false, impactedFilter);
                for (Entry<InstanceKey, TimestampedValue<Instance>> e : got.entrySet()) {
                    LOG.info("Removing instance:" + e + " because service:" + value + " was removed.");
                    instances.remove(e.getKey());
                }
            }));
        amzaService.watch(releaseGroupStoreKey, new UpenaStoreChanges<>(mapper, ReleaseGroupKey.class, ReleaseGroup.class,
            (key, value) -> {
                InstanceFilter impactedFilter = new InstanceFilter(null, null, null, key, null, 0, Integer.MAX_VALUE);
                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(false, impactedFilter);
                List<InstanceChanged> changes = new ArrayList<>();
                for (Entry<InstanceKey, TimestampedValue<Instance>> instance : got.entrySet()) {
                    changes.add(new InstanceChanged(instance.getValue().getValue().hostKey.getKey(), instance.getKey().getKey()));
                }
                instanceChanges.changed(changes);
            },
            (key, value) -> {
                InstanceFilter impactedFilter = new InstanceFilter(null, null, null, key, null, 0, Integer.MAX_VALUE);
                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(false, impactedFilter);
                for (Entry<InstanceKey, TimestampedValue<Instance>> e : got.entrySet()) {
                    LOG.info("Removing instance:" + e + " because release group:" + value + " was removed.");
                    instances.remove(e.getKey());
                }
            }));
        amzaService.watch(instanceStoreKey, new UpenaStoreChanges<>(mapper, InstanceKey.class, Instance.class,
            (key, value) -> {
                if (value.getValue() != null) {
                    List<InstanceChanged> changes = new ArrayList<>();
                    changes.add(new InstanceChanged(value.getValue().hostKey.getKey(), key.getKey()));
                    instanceChanges.changed(changes);
                }
            },
            (key, value) -> {
                if (value.getValue() != null) {
                    List<InstanceChanged> changes = new ArrayList<>();
                    changes.add(new InstanceChanged(value.getValue().hostKey.getKey(), key.getKey()));
                    instanceRemoved.changed(changes);
                }
            }));

        amzaService.watch(tenantStoreKey, new UpenaStoreChanges<>(mapper, TenantKey.class, Tenant.class,
            (key, value) -> {
                if (value.getValue() != null) {
                    List<TenantChanged> changes = new ArrayList<>();
                    changes.add(new TenantChanged(value.getValue().tenantId));
                    tenantChanges.changed(changes);
                }
            },
            (key, value) -> {
                if (value.getValue() != null) {
                    List<TenantChanged> changes = new ArrayList<>();
                    changes.add(new TenantChanged(value.getValue().tenantId));
                    tenantChanges.changed(changes);
                }
            }));
    }

}
