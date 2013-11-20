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

import com.jivesoftware.os.amza.service.AmzaService;
import com.jivesoftware.os.amza.shared.TableDelta;
import com.jivesoftware.os.amza.shared.TableName;
import com.jivesoftware.os.amza.shared.TableStateChanges;
import com.jivesoftware.os.amza.shared.TimestampedValue;
import com.jivesoftware.os.jive.utils.logger.MetricLogger;
import com.jivesoftware.os.jive.utils.logger.MetricLoggerFactory;
import com.jivesoftware.os.upena.routing.shared.InstanceChanged;
import com.jivesoftware.os.upena.routing.shared.TenantChanged;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.Tenant;
import com.jivesoftware.os.upena.shared.TenantFilter;
import com.jivesoftware.os.upena.shared.TenantKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentNavigableMap;

public class UpenaStore {

    private final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final AmzaService amzaService;
    private final InstanceChanges instanceChanges;
    private final TenantChanges tenantChanges;

    public final TableName<ClusterKey, Cluster> clusterStoreKey = new TableName<>("master", "clusters", ClusterKey.class, null, null, Cluster.class);
    public final TableName<HostKey, Host> hostStoreKey = new TableName<>("master", "hosts", HostKey.class, null, null, Host.class);
    public final TableName<ServiceKey, Service> serviceStoreKey = new TableName<>("master", "services", ServiceKey.class, null, null, Service.class);
    public final TableName<ReleaseGroupKey, ReleaseGroup> releaseGroupStoreKey = new TableName<>("master",
            "releaseGroups", ReleaseGroupKey.class, null, null, ReleaseGroup.class);
    public final TableName<InstanceKey, Instance> instanceStoreKey = new TableName<>("master",
            "intances", InstanceKey.class, null, null, Instance.class);
    public final TableName<TenantKey, Tenant> tenantStoreKey = new TableName<>("master", "tenants", TenantKey.class, null, null, Tenant.class);

    public final UpenaTable<ClusterKey, Cluster> clusters;
    public final UpenaTable<HostKey, Host> hosts;
    public final UpenaTable<ServiceKey, Service> services;
    public final UpenaTable<ReleaseGroupKey, ReleaseGroup> releaseGroups;
    public final UpenaTable<InstanceKey, Instance> instances;
    public final UpenaTable<TenantKey, Tenant> tenants;

    public UpenaStore(AmzaService amzaService,
            InstanceChanges instanceChanges,
            TenantChanges tenantChanges) throws Exception {
        this.amzaService = amzaService;
        this.instanceChanges = instanceChanges;
        this.tenantChanges = tenantChanges;

        clusters = new UpenaTable<>(amzaService.getTable(clusterStoreKey), new ClusterKeyProvider(), null);
        hosts = new UpenaTable<>(amzaService.getTable(hostStoreKey), new HostKeyProvider(), null);
        services = new UpenaTable<>(amzaService.getTable(serviceStoreKey), new ServiceKeyProvider(), null);
        releaseGroups = new UpenaTable<>(amzaService.getTable(releaseGroupStoreKey), new ReleaseGroupKeyProvider(), null);
        instances = new UpenaTable<>(amzaService.getTable(instanceStoreKey), new InstanceKeyProvider(), new InstanceValidator());
        tenants = new UpenaTable<>(amzaService.getTable(tenantStoreKey), new TenantKeyProvider(), null);
    }

    public void attachWatchers() throws Exception {
        amzaService.watch(clusterStoreKey, new Changes<>(
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
        amzaService.watch(hostStoreKey, new Changes<>(new KeyValueChange<HostKey, Host>() {
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
        amzaService.watch(serviceStoreKey, new Changes<>(new KeyValueChange<ServiceKey, Service>() {
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
        amzaService.watch(releaseGroupStoreKey, new Changes<>(new KeyValueChange<ReleaseGroupKey, ReleaseGroup>() {
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

                TenantFilter impactedTenantsFilter = new TenantFilter(null, null, key, 0, Integer.MAX_VALUE);
                ConcurrentNavigableMap<TenantKey, TimestampedValue<Tenant>> gotTenants = tenants.find(impactedTenantsFilter);
                for (Entry<TenantKey, TimestampedValue<Tenant>> tenant : gotTenants.entrySet()) {
                    //updateTenant(new TenantKey(tenant.getKey()), null); TODO copy orphaned tenant
                }

            }
        }));
        amzaService.watch(instanceStoreKey, new Changes<>(new KeyValueChange<InstanceKey, Instance>() {
            @Override
            public void change(InstanceKey key, TimestampedValue<Instance> value) throws Exception {
                List<InstanceChanged> changes = new ArrayList<>();
                changes.add(new InstanceChanged(value.getValue().hostKey.getKey(), key.getKey()));
                instanceChanges.changed(changes);
            }
        }, null));

        amzaService.watch(tenantStoreKey, new Changes<>(new KeyValueChange<TenantKey, Tenant>() {
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

    static class Changes<K, V> implements TableStateChanges<K, V> {

        private final KeyValueChange<K, V> adds;
        private final KeyValueChange<K, V> removes;

        public Changes(KeyValueChange<K, V> adds, KeyValueChange<K, V> removes) {
            this.adds = adds;
            this.removes = removes;
        }

        @Override
        public void changes(TableName<K, V> mapName, TableDelta<K, V> changes) throws Exception {
            NavigableMap<K, TimestampedValue<V>> appliedRows = changes.getApply();
            for (Entry<K, TimestampedValue<V>> entry : appliedRows.entrySet()) {
                K key = entry.getKey();
                TimestampedValue<V> value = entry.getValue();
                if (value.getTombstoned() && removes != null) {
                    Collection<TimestampedValue<V>> got = changes.getClobbered().get(key);
                    if (got != null) {
                        for (TimestampedValue<V> g : got) {
                            removes.change(key, g);
                        }
                    }
                } else if (adds != null) {
                    adds.change(key, value);
                }
            }
        }
    }
}