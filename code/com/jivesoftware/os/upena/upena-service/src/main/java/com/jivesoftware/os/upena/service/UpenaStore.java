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
import com.jivesoftware.os.amza.service.AmzaService;
import com.jivesoftware.os.amza.service.AmzaTable;
import com.jivesoftware.os.amza.shared.RowChanges;
import com.jivesoftware.os.amza.shared.RowIndexKey;
import com.jivesoftware.os.amza.shared.RowIndexValue;
import com.jivesoftware.os.amza.shared.RowScan;
import com.jivesoftware.os.amza.shared.RowsChanged;
import com.jivesoftware.os.amza.shared.TableName;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.routing.shared.InstanceChanged;
import com.jivesoftware.os.upena.routing.shared.TenantChanged;
import com.jivesoftware.os.upena.shared.BasicTimestampedValue;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.RecordedChange;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.Tenant;
import com.jivesoftware.os.upena.shared.TenantKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UpenaStore {

    private final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final AmzaService amzaService;
    private final InstanceChanges instanceChanges;
    private final InstanceChanges instanceRemoved;
    private final TenantChanges tenantChanges;

    public final TableName clusterStoreKey = new TableName("master", "clusters", null, null);
    public final TableName hostStoreKey = new TableName("master", "hosts", null, null);
    public final TableName serviceStoreKey = new TableName("master", "services", null, null);
    public final TableName releaseGroupStoreKey = new TableName("master", "releaseGroups", null, null);
    public final TableName instanceStoreKey = new TableName("master", "intances", null, null);
    public final TableName tenantStoreKey = new TableName("master", "tenants", null, null);
    public final TableName changeLogStoreKey = new TableName("master", "changeLog", null, null);

    public final UpenaTable<ClusterKey, Cluster> clusters;
    public final UpenaTable<HostKey, Host> hosts;
    public final UpenaTable<ServiceKey, Service> services;
    public final UpenaTable<ReleaseGroupKey, ReleaseGroup> releaseGroups;
    public final UpenaTable<InstanceKey, Instance> instances;
    public final UpenaTable<TenantKey, Tenant> tenants;
    public final AmzaTable changeLog;

    public UpenaStore(AmzaService amzaService,
        InstanceChanges instanceChanges,
        InstanceChanges instanceRemoved,
        TenantChanges tenantChanges) throws Exception {
        this.amzaService = amzaService;
        this.instanceChanges = instanceChanges;
        this.instanceRemoved = instanceRemoved;
        this.tenantChanges = tenantChanges;

        clusters = new UpenaTable<>(amzaService.getTable(clusterStoreKey), ClusterKey.class, Cluster.class, new ClusterKeyProvider(), null);
        hosts = new UpenaTable<>(amzaService.getTable(hostStoreKey), HostKey.class, Host.class, new HostKeyProvider(), null);
        services = new UpenaTable<>(amzaService.getTable(serviceStoreKey), ServiceKey.class, Service.class, new ServiceKeyProvider(), null);
        releaseGroups = new UpenaTable<>(amzaService.getTable(releaseGroupStoreKey),
            ReleaseGroupKey.class, ReleaseGroup.class, new ReleaseGroupKeyProvider(), null);
        instances = new UpenaTable<>(amzaService.getTable(instanceStoreKey),
            InstanceKey.class, Instance.class, new InstanceKeyProvider(), new InstanceValidator());
        tenants = new UpenaTable<>(amzaService.getTable(tenantStoreKey), TenantKey.class, Tenant.class, new TenantKeyProvider(), null);

        changeLog = amzaService.getTable(changeLogStoreKey);

    }

    public void record(String who, String what, long whenTimestampMillis, String why, String where, String how) throws Exception {
        long descendingTimestamp = Long.MAX_VALUE - whenTimestampMillis;
        changeLog.set(new RowIndexKey(longBytes(descendingTimestamp, new byte[8], 0)),
            mapper.writeValueAsBytes(new RecordedChange(who, what, whenTimestampMillis, where, why, how)));
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
        changeLog.scan(new RowScan<Exception>() {

            @Override
            public boolean row(long l, RowIndexKey key, RowIndexValue value) throws Exception {
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
            }
        });

    }

    public interface LogStream {

        boolean stream(RecordedChange change) throws Exception;
    }

    public void attachWatchers() throws Exception {
        amzaService.watch(clusterStoreKey, new Changes<>(ClusterKey.class, Cluster.class,
            new KeyValueChange<ClusterKey, Cluster>() {
                @Override
                public void change(ClusterKey key, TimestampedValue<Cluster> value) throws Exception {
                    InstanceFilter impactedFilter = new InstanceFilter(key, null, null, null, null, 0, Integer.MAX_VALUE);
                    ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(impactedFilter);
                    List<InstanceChanged> changes = new ArrayList<>();
                    for (Entry<InstanceKey, TimestampedValue<Instance>> instance : got.entrySet()) {
                        changes.add(new InstanceChanged(instance.getValue().getValue().hostKey.getKey(), instance.getKey().getKey()));
                    }
                    instanceChanges.changed(changes);
                }
            }, new KeyValueChange<ClusterKey, Cluster>() {
                @Override
                public void change(ClusterKey key, TimestampedValue<Cluster> value) throws Exception {
                    InstanceFilter impactedFilter = new InstanceFilter(key, null, null, null, null, 0, Integer.MAX_VALUE);
                    ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(impactedFilter);
                    for (Entry<InstanceKey, TimestampedValue<Instance>> e : got.entrySet()) {
                        LOG.info("Removing instance:" + e + " because cluster:" + value + " was removed.");
                        instances.remove(e.getKey());
                    }
                }
            }));
        amzaService.watch(hostStoreKey, new Changes<>(HostKey.class, Host.class, new KeyValueChange<HostKey, Host>() {
            @Override
            public void change(HostKey key, TimestampedValue<Host> value) throws Exception {
                InstanceFilter impactedFilter = new InstanceFilter(null, key, null, null, null, 0, Integer.MAX_VALUE);
                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(impactedFilter);
                List<InstanceChanged> changes = new ArrayList<>();
                for (Entry<InstanceKey, TimestampedValue<Instance>> instance : got.entrySet()) {
                    changes.add(new InstanceChanged(instance.getValue().getValue().hostKey.getKey(), instance.getKey().getKey()));
                }
                instanceChanges.changed(changes);
            }
        }, new KeyValueChange<HostKey, Host>() {
            @Override
            public void change(HostKey key, TimestampedValue<Host> value) throws Exception {
                InstanceFilter impactedFilter = new InstanceFilter(null, key, null, null, null, 0, Integer.MAX_VALUE);
                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(impactedFilter);
                for (Entry<InstanceKey, TimestampedValue<Instance>> e : got.entrySet()) {
                    LOG.info("Removing instance:" + e + " because host:" + value + " was removed.");
                    instances.remove(e.getKey());
                }
            }
        }));
        amzaService.watch(serviceStoreKey, new Changes<>(ServiceKey.class, Service.class, new KeyValueChange<ServiceKey, Service>() {
            @Override
            public void change(ServiceKey key, TimestampedValue<Service> value) throws Exception {
                InstanceFilter impactedFilter = new InstanceFilter(null, null, key, null, null, 0, Integer.MAX_VALUE);
                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(impactedFilter);
                List<InstanceChanged> changes = new ArrayList<>();
                for (Entry<InstanceKey, TimestampedValue<Instance>> instance : got.entrySet()) {
                    changes.add(new InstanceChanged(instance.getValue().getValue().hostKey.getKey(), instance.getKey().getKey()));
                }
                instanceChanges.changed(changes);
            }
        }, new KeyValueChange<ServiceKey, Service>() {
            @Override
            public void change(ServiceKey key, TimestampedValue<Service> value) throws Exception {
                InstanceFilter impactedFilter = new InstanceFilter(null, null, key, null, null, 0, Integer.MAX_VALUE);
                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(impactedFilter);
                for (Entry<InstanceKey, TimestampedValue<Instance>> e : got.entrySet()) {
                    LOG.info("Removing instance:" + e + " because service:" + value + " was removed.");
                    instances.remove(e.getKey());
                }
            }
        }));
        amzaService.watch(releaseGroupStoreKey, new Changes<>(ReleaseGroupKey.class, ReleaseGroup.class, new KeyValueChange<ReleaseGroupKey, ReleaseGroup>() {
            @Override
            public void change(ReleaseGroupKey key, TimestampedValue<ReleaseGroup> value) throws Exception {
                InstanceFilter impactedFilter = new InstanceFilter(null, null, null, key, null, 0, Integer.MAX_VALUE);
                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(impactedFilter);
                List<InstanceChanged> changes = new ArrayList<>();
                for (Entry<InstanceKey, TimestampedValue<Instance>> instance : got.entrySet()) {
                    changes.add(new InstanceChanged(instance.getValue().getValue().hostKey.getKey(), instance.getKey().getKey()));
                }
                instanceChanges.changed(changes);

            }
        }, new KeyValueChange<ReleaseGroupKey, ReleaseGroup>() {
            @Override
            public void change(ReleaseGroupKey key, TimestampedValue<ReleaseGroup> value) throws Exception {
                InstanceFilter impactedFilter = new InstanceFilter(null, null, null, key, null, 0, Integer.MAX_VALUE);
                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = instances.find(impactedFilter);
                for (Entry<InstanceKey, TimestampedValue<Instance>> e : got.entrySet()) {
                    LOG.info("Removing instance:" + e + " because release group:" + value + " was removed.");
                    instances.remove(e.getKey());
                }

            }
        }));
        amzaService.watch(instanceStoreKey, new Changes<>(InstanceKey.class, Instance.class, new KeyValueChange<InstanceKey, Instance>() {
            @Override
            public void change(InstanceKey key, TimestampedValue<Instance> value) throws Exception {
                List<InstanceChanged> changes = new ArrayList<>();
                changes.add(new InstanceChanged(value.getValue().hostKey.getKey(), key.getKey()));
                instanceChanges.changed(changes);
            }
        }, new KeyValueChange<InstanceKey, Instance>() {

            @Override
            public void change(InstanceKey key, TimestampedValue<Instance> value) throws Exception {
                List<InstanceChanged> changes = new ArrayList<>();
                changes.add(new InstanceChanged(value.getValue().hostKey.getKey(), key.getKey()));
                instanceRemoved.changed(changes);
            }
        }));

        amzaService.watch(tenantStoreKey, new Changes<>(TenantKey.class, Tenant.class, new KeyValueChange<TenantKey, Tenant>() {
            @Override
            public void change(TenantKey key, TimestampedValue<Tenant> value) throws Exception {
                List<TenantChanged> changes = new ArrayList<>();
                changes.add(new TenantChanged(value.getValue().tenantId));
                tenantChanges.changed(changes);
            }
        }, new KeyValueChange<TenantKey, Tenant>() {
            @Override
            public void change(TenantKey key, TimestampedValue<Tenant> value) throws Exception {
                List<TenantChanged> changes = new ArrayList<>();
                changes.add(new TenantChanged(value.getValue().tenantId));
                tenantChanges.changed(changes);
            }
        }));

    }

    private static final ObjectMapper mapper = new ObjectMapper();

    static class Changes<K, V> implements RowChanges {

        private final Class<K> keyClass;
        private final Class<V> valueClass;
        private final KeyValueChange<K, V> adds;
        private final KeyValueChange<K, V> removes;

        public Changes(Class<K> keyClass, Class<V> valueClass, KeyValueChange<K, V> adds, KeyValueChange<K, V> removes) {
            this.keyClass = keyClass;
            this.valueClass = valueClass;
            this.adds = adds;
            this.removes = removes;
        }

        @Override
        public void changes(RowsChanged changes) throws Exception {
            NavigableMap<RowIndexKey, RowIndexValue> appliedRows = changes.getApply();
            for (Entry<RowIndexKey, RowIndexValue> entry : appliedRows.entrySet()) {
                RowIndexKey rawKey = entry.getKey();
                RowIndexValue rawValue = entry.getValue();
                if (entry.getValue().getTombstoned() && removes != null) {
                    Collection<RowIndexValue> got = changes.getClobbered().get(rawKey);
                    if (got != null) {
                        for (RowIndexValue g : got) {
                            K k = mapper.readValue(rawKey.getKey(), keyClass);
                            V v = mapper.readValue(g.getValue(), valueClass);
                            removes.change(k, new BasicTimestampedValue<>(v, g.getTimestampId(), g.getTombstoned()));
                        }
                    }
                } else if (adds != null) {
                    K k = mapper.readValue(rawKey.getKey(), keyClass);
                    V v = mapper.readValue(rawValue.getValue(), valueClass);
                    adds.change(k, new BasicTimestampedValue<>(v, rawValue.getTimestampId(), rawValue.getTombstoned()));
                }
            }
        }
    }
}
