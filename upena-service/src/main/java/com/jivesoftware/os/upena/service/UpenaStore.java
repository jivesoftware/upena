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
import com.jivesoftware.os.amza.api.partition.Consistency;
import com.jivesoftware.os.amza.api.partition.Durability;
import com.jivesoftware.os.amza.api.partition.PartitionName;
import com.jivesoftware.os.amza.api.partition.PartitionProperties;
import com.jivesoftware.os.amza.api.stream.RowType;
import com.jivesoftware.os.amza.service.AmzaService;
import com.jivesoftware.os.amza.service.EmbeddedClientProvider;
import com.jivesoftware.os.amza.service.EmbeddedClientProvider.CheckOnline;
import com.jivesoftware.os.amza.service.EmbeddedClientProvider.EmbeddedClient;
import com.jivesoftware.os.amza.service.Partition.ScanRange;
import com.jivesoftware.os.jive.utils.ordered.id.OrderIdProvider;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.shared.BasicTimestampedValue;
import com.jivesoftware.os.upena.shared.ChaosState;
import com.jivesoftware.os.upena.shared.ChaosStateKey;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.Key;
import com.jivesoftware.os.upena.shared.KeyValueFilter;
import com.jivesoftware.os.upena.shared.LB;
import com.jivesoftware.os.upena.shared.LBKey;
import com.jivesoftware.os.upena.shared.Monkey;
import com.jivesoftware.os.upena.shared.MonkeyKey;
import com.jivesoftware.os.upena.shared.Permission;
import com.jivesoftware.os.upena.shared.PermissionKey;
import com.jivesoftware.os.upena.shared.Project;
import com.jivesoftware.os.upena.shared.ProjectKey;
import com.jivesoftware.os.upena.shared.RecordedChange;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.Stored;
import com.jivesoftware.os.upena.shared.Tenant;
import com.jivesoftware.os.upena.shared.TenantKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import com.jivesoftware.os.upena.shared.User;
import com.jivesoftware.os.upena.shared.UserKey;
import java.util.Collections;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class UpenaStore {

    private final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final ObjectMapper mapper;

    public UpenaMap<UserKey, User> users;
    public UpenaMap<PermissionKey, Permission> permissions;
    public UpenaMap<ProjectKey, Project> projects;
    public UpenaMap<ClusterKey, Cluster> clusters;
    public UpenaMap<LBKey, LB> loadBalancers;
    public UpenaMap<HostKey, Host> hosts;
    public UpenaMap<ServiceKey, Service> services;
    public UpenaMap<ReleaseGroupKey, ReleaseGroup> releaseGroups;
    public UpenaMap<InstanceKey, Instance> instances;
    public UpenaMap<TenantKey, Tenant> tenants;
    public UpenaMap<MonkeyKey, Monkey> monkeys;
    public UpenaMap<ChaosStateKey, ChaosState> chaosStates;

    private final AmzaService amzaService;
    private final EmbeddedClientProvider embeddedClientProvider;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public UpenaStore(
        ObjectMapper mapper,
        InstanceChanges instanceChanges,
        InstanceChanges instanceRemoved,
        TenantChanges tenantChanges,

        AmzaService amzaService,
        EmbeddedClientProvider embeddedClientProvider) throws Exception {

        this.amzaService = amzaService;
        this.embeddedClientProvider = embeddedClientProvider;

        this.mapper = mapper;
    }

    public void init(OrderIdProvider idProvider,
        int minServicePort,
        int maxServicePort,
        boolean cleanup) throws Exception {

        PartitionProperties partitionProperties = new PartitionProperties(Durability.fsync_async,
            TimeUnit.DAYS.toMillis(30), TimeUnit.DAYS.toMillis(10), TimeUnit.DAYS.toMillis(30), TimeUnit.DAYS.toMillis(10), 0, 0, 0, 0,
            false, Consistency.quorum, true, true, false, RowType.snappy_primary, "lab", -1, null, -1, -1);


        users = new AmzaUpenaMap<>(mapper, amzaService, embeddedClientProvider, partitionProperties,
            getPartitionName("user"), UserKey.class, User.class, new UserKeyProvider(), null);

        permissions = new AmzaUpenaMap<>(mapper, amzaService, embeddedClientProvider, partitionProperties,
            getPartitionName("permissions"), PermissionKey.class, Permission.class, new PermissionKeyProvider(), null);

        projects = new AmzaUpenaMap<>(mapper, amzaService, embeddedClientProvider, partitionProperties,
            getPartitionName("projects"), ProjectKey.class, Project.class, new ProjectKeyProvider(idProvider), null);


        clusters = new AmzaUpenaMap<>(mapper, amzaService, embeddedClientProvider, partitionProperties,
            getPartitionName("clusters"), ClusterKey.class, Cluster.class, new ClusterKeyProvider(idProvider), null);

        loadBalancers = new AmzaUpenaMap<>(mapper, amzaService, embeddedClientProvider, partitionProperties,
            getPartitionName("load-balancers"), LBKey.class, LB.class, new LBKeyProvider(idProvider), null);

        hosts = new AmzaUpenaMap<>(mapper, amzaService, embeddedClientProvider, partitionProperties,
            getPartitionName("hosts"), HostKey.class, Host.class, new HostKeyProvider(), null);

        services = new AmzaUpenaMap<>(mapper, amzaService, embeddedClientProvider, partitionProperties,
            getPartitionName("services"), ServiceKey.class, Service.class, new ServiceKeyProvider(idProvider), null);

        releaseGroups = new AmzaUpenaMap<>(mapper, amzaService, embeddedClientProvider, partitionProperties,
            getPartitionName("releases"), ReleaseGroupKey.class, ReleaseGroup.class, new ReleaseGroupKeyProvider(idProvider), null);

        instances = new AmzaUpenaMap<>(mapper, amzaService, embeddedClientProvider, partitionProperties,
            getPartitionName("instances"), InstanceKey.class, Instance.class, new InstanceKeyProvider(idProvider),
            new InstanceValidator(minServicePort, maxServicePort));

        tenants = new AmzaUpenaMap<>(mapper, amzaService, embeddedClientProvider, partitionProperties,
            getPartitionName("tenants"), TenantKey.class, Tenant.class, new TenantKeyProvider(), null);

        monkeys = new AmzaUpenaMap<>(mapper, amzaService, embeddedClientProvider, partitionProperties,
            getPartitionName("monkeys"), MonkeyKey.class, Monkey.class, new MonkeyKeyProvider(idProvider), null);

        chaosStates = new AmzaUpenaMap<>(mapper, amzaService, embeddedClientProvider, partitionProperties,
            getPartitionName("chaos"), ChaosStateKey.class, ChaosState.class, new ChaosStateKeyProvider(idProvider), null);

        initialized.set(true);
    }

    public boolean isReady() {
        return initialized.get();
    }

    private EmbeddedClient changeLogClient() throws Exception {
        PartitionProperties partitionProperties = new PartitionProperties(Durability.fsync_async,
            TimeUnit.DAYS.toMillis(30), TimeUnit.DAYS.toMillis(10), TimeUnit.DAYS.toMillis(30), TimeUnit.DAYS.toMillis(10),
            TimeUnit.DAYS.toMillis(30), TimeUnit.DAYS.toMillis(10), TimeUnit.DAYS.toMillis(30), TimeUnit.DAYS.toMillis(10),
            false, Consistency.quorum, true, true, false, RowType.snappy_primary, "lab", -1, null, -1, -1);

        PartitionName partitionName = getPartitionName("change-changeLog");
        while (true) {
            try {
                amzaService.getRingWriter().ensureMaximalRing(partitionName.getRingName(), 30_000L); //TODO config
                amzaService.createPartitionIfAbsent(partitionName, partitionProperties);
                amzaService.awaitOnline(partitionName, 30_000L); //TODO config
                return embeddedClientProvider.getClient(partitionName, CheckOnline.once);
            } catch (Exception x) {
                LOG.warn("Failed to get client for " + partitionName.getName() + ". Retrying...", x);
            }
        }
    }


    private EmbeddedClient healthLogClient() throws Exception {
        PartitionProperties partitionProperties = new PartitionProperties(Durability.fsync_async,
            TimeUnit.DAYS.toMillis(30), TimeUnit.DAYS.toMillis(10), TimeUnit.DAYS.toMillis(30), TimeUnit.DAYS.toMillis(10),
            TimeUnit.DAYS.toMillis(30), TimeUnit.DAYS.toMillis(10), TimeUnit.DAYS.toMillis(30), TimeUnit.DAYS.toMillis(10),
            false, Consistency.quorum, true, true, false, RowType.snappy_primary, "lab", -1, null, -1, -1);

        PartitionName partitionName = getPartitionName("health-changeLog");
        while (true) {
            try {
                amzaService.getRingWriter().ensureMaximalRing(partitionName.getRingName(), 30_000L); //TODO config
                amzaService.createPartitionIfAbsent(partitionName, partitionProperties);
                amzaService.awaitOnline(partitionName, 30_000L); //TODO config
                return embeddedClientProvider.getClient(partitionName, CheckOnline.once);
            } catch (Exception x) {
                LOG.warn("Failed to get client for " + partitionName.getName() + ". Retrying...", x);
            }
        }
    }

    private PartitionName getPartitionName(String name) {
        return new PartitionName(false, "upena".getBytes(), ("upena-" + name).getBytes());
    }

    private <K extends Key, V extends Stored> UpenaMap<K, V> copy(String name, UpenaMap<K, V> a, UpenaMap<K, V> b, boolean cleanup) throws Exception {
        if (a != null) {
            long[] count = { 0 };
            a.scan((key, value) -> {
                V got = b.get(key);
                if (got == null) {
                    count[0]++;
                    b.update(key, value);
                }
                return true;
            });
            LOG.info("UPGRADE: carried {} configs forward for {}.", count[0], name);
            if (cleanup) {
                a.scan((key, value) -> {
                    a.remove(key);
                    return true;
                });
            }
        }
        return b;
    }

    static public class AmzaUpenaMap<K extends Key, V extends Stored> implements UpenaMap<K, V> {

        public static final MetricLogger LOG = MetricLoggerFactory.getLogger();

        private final ObjectMapper mapper;
        private final PartitionProperties partitionProperties;
        private final PartitionName partitionName;
        private final AmzaService amzaService;
        private final EmbeddedClientProvider embeddedClientProvider;
        private final Class<K> keyClass;
        private final Class<V> valueClass;
        private final UpenaKeyProvider<K, V> keyProvider;
        private final UpenaValueValidator<K, V> valueValidator;


        public AmzaUpenaMap(ObjectMapper mapper,
            AmzaService amzaService,
            EmbeddedClientProvider embeddedClientProvider,
            PartitionProperties partitionProperties,
            PartitionName partitionName,
            Class<K> keyClass,
            Class<V> valueClass,
            UpenaKeyProvider<K, V> keyProvider,
            UpenaValueValidator<K, V> valueValidator) {

            this.mapper = mapper;
            this.partitionProperties = partitionProperties;
            this.partitionName = partitionName;
            this.amzaService = amzaService;
            this.embeddedClientProvider = embeddedClientProvider;
            this.keyClass = keyClass;
            this.valueClass = valueClass;
            this.keyProvider = keyProvider;
            this.valueValidator = valueValidator;
        }

        @Override
        public void putIfAbsent(K key, V value) throws Exception {
            byte[] rawValue = client().getValue(Consistency.none, null, mapper.writeValueAsBytes(key));
            if (rawValue == null) {
                update(key, value);
            }
        }

        @Override
        public V get(K key) throws Exception {
            byte[] rawValue = client().getValue(Consistency.none, null, mapper.writeValueAsBytes(key));
            return rawValue != null ? mapper.readValue(rawValue, valueClass) : null;
        }

        @Override
        public void scan(Stream<K, V> stream) throws Exception {
            client().scan(Collections.singletonList(ScanRange.ROW_SCAN), (byte[] prefix, byte[] key, byte[] value, long timestamp, long version) -> {
                K k = mapper.readValue(key, keyClass);
                V v = mapper.readValue(value, valueClass);
                return stream.stream(k, v);
            }, true);
        }

        @Override
        public ConcurrentNavigableMap<K, TimestampedValue<V>> find(boolean removeBadKeysEnabled, KeyValueFilter<K, V> filter) throws Exception {
            ConcurrentNavigableMap<K, TimestampedValue<V>> results = filter == null ? null : filter.createCollector();
            if (results != null) {
                client().scan(Collections.singletonList(ScanRange.ROW_SCAN), (byte[] prefix, byte[] key, byte[] value, long timestamp, long version) -> {
                    K k = mapper.readValue(key, keyClass);
                    V v = mapper.readValue(value, valueClass);
                    if (filter.filter(k, v)) {
                        results.put(k, new BasicTimestampedValue<>(v, timestamp, false));
                    }
                    return true;
                }, true);
            }
            return results;
        }

        @Override
        public K update(K key, V value) throws Exception {
            if (key == null) {
                key = keyProvider.getNodeKey(this, value);
            }
            if (valueValidator != null) {
                value = valueValidator.validate(this, key, value);
            }

            final K k = key;
            final V v = value;
            client().commit(Consistency.quorum,
                null,
                commitKeyValueStream -> commitKeyValueStream.commit(
                    mapper.writeValueAsBytes(k),
                    mapper.writeValueAsBytes(v),
                    -1,
                    false
                ),
                30_000,
                TimeUnit.MILLISECONDS);
            return key;
        }

        @Override
        public boolean remove(K key) throws Exception {
            client().commit(Consistency.quorum,
                null,
                commitKeyValueStream -> commitKeyValueStream.commit(
                    mapper.writeValueAsBytes(key),
                    null,
                    -1,
                    true
                ),
                30_000,
                TimeUnit.MILLISECONDS);
            return true;
        }

        private EmbeddedClient client() throws Exception {
            while (true) {
                try {

                    amzaService.getRingWriter().ensureMaximalRing(partitionName.getRingName(), 30_000L); //TODO config
                    amzaService.createPartitionIfAbsent(partitionName, partitionProperties);
                    amzaService.awaitOnline(partitionName, 30_000L); //TODO config
                    return embeddedClientProvider.getClient(partitionName, CheckOnline.once);
                } catch (Exception x) {
                    LOG.warn("Failed to get client for " + partitionName.getName() + ". Retrying...", x);
                }
            }
        }
    }


    public void recordChange(String who, String what, long whenTimestampMillis, String why, String where, String how) throws Exception {
        if (who == null) {
            who = "null";
        }
        String w = who;
        long descendingTimestamp = Long.MAX_VALUE - whenTimestampMillis;

        changeLogClient().commit(Consistency.quorum, null, commitKeyValueStream -> commitKeyValueStream.commit(
            longBytes(descendingTimestamp, new byte[8], 0),
            mapper.writeValueAsBytes(new RecordedChange(w, what, whenTimestampMillis, where, why, how)),
            -1,
            false
            ),
            30_000,
            TimeUnit.MILLISECONDS);
    }

    public void recordHealth(String who, String what, long whenTimestampMillis, String why, String where, String how) throws Exception {
        if (who == null) {
            who = "null";
        }
        String w = who;
        long descendingTimestamp = Long.MAX_VALUE - whenTimestampMillis;

        healthLogClient().commit(Consistency.quorum, null, commitKeyValueStream -> commitKeyValueStream.commit(
            longBytes(descendingTimestamp, new byte[8], 0),
            mapper.writeValueAsBytes(new RecordedChange(w, what, whenTimestampMillis, where, why, how)),
            -1,
            false
            ),
            30_000,
            TimeUnit.MILLISECONDS);
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

    public void changeLog(long whenAgoElapseLargestMillis,
        long whenAgoElapseSmallestMillis,
        int minCount, //
         String who,
         String what,
         String why,
         String where,
         String how,
         LogStream logStream) throws Exception {

        long time = System.currentTimeMillis();
        final long maxTimestampInclusize = time - whenAgoElapseSmallestMillis;
        final long minTimestampExclusize = time - whenAgoElapseLargestMillis;

        final AtomicInteger count = new AtomicInteger(minCount);
        changeLogClient().scan(Collections.singletonList(ScanRange.ROW_SCAN),
            (byte[] prefix, byte[] key, byte[] value, long timestamp, long version) -> {
                RecordedChange change = mapper.readValue(value, RecordedChange.class);
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
            }, true);
    }

    public void healthLog(long whenAgoElapseLargestMillis,
        long whenAgoElapseSmallestMillis,
        int minCount, //
        String who,
        String what,
        String why,
        String where,
        String how,
        LogStream logStream) throws Exception {

        long time = System.currentTimeMillis();
        final long maxTimestampInclusize = time - whenAgoElapseSmallestMillis;
        final long minTimestampExclusize = time - whenAgoElapseLargestMillis;

        final AtomicInteger count = new AtomicInteger(minCount);
        healthLogClient().scan(Collections.singletonList(ScanRange.ROW_SCAN),
            (byte[] prefix, byte[] key, byte[] value, long timestamp, long version) -> {
                RecordedChange change = mapper.readValue(value, RecordedChange.class);
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
            }, true);
    }



    public interface LogStream {

        boolean stream(RecordedChange change) throws Exception;
    }



}
