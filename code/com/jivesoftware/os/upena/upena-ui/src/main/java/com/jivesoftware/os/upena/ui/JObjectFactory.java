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
package com.jivesoftware.os.upena.ui;

import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterFilter;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostFilter;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.Key;
import com.jivesoftware.os.upena.shared.KeyValueFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceFilter;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.Stored;
import com.jivesoftware.os.upena.shared.Tenant;
import com.jivesoftware.os.upena.shared.TenantFilter;
import com.jivesoftware.os.upena.shared.TenantKey;
import java.util.HashMap;
import java.util.Map;

public class JObjectFactory {

    private final Map<Class, Creator<? extends Key, ?, ? extends KeyValueFilter<?, ?>>> factory = new HashMap<>();

    public JObjectFactory(final RequestHelperProvider requestHelperProvider) {

        factory.put(Instance.class, new Creator<InstanceKey, Instance, InstanceFilter>() {
            @Override
            public JObject<InstanceKey, Instance, InstanceFilter> create(boolean hasPopup, IPicked<InstanceKey, Instance> picked) {

                JExecutor<InstanceKey, Instance, InstanceFilter> vExecutor = new JExecutor<>(requestHelperProvider, "instance");
                JFieldsForInstance fields = new JFieldsForInstance(JObjectFactory.this, "", null);
                return new JObject<>(fields, vExecutor, hasPopup, picked);
            }
        });

        factory.put(Cluster.class, new Creator<ClusterKey, Cluster, ClusterFilter>() {
            @Override
            public JObject<ClusterKey, Cluster, ClusterFilter> create(boolean hasPopup, IPicked<ClusterKey, Cluster> picked) {

                JExecutor<ClusterKey, Cluster, ClusterFilter> vExecutor = new JExecutor<>(requestHelperProvider, "cluster");
                JFieldsForCluster fields = new JFieldsForCluster(JObjectFactory.this, "", null);
                return new JObject<>(fields, vExecutor, hasPopup, picked);
            }
        });

        factory.put(Host.class, new Creator<HostKey, Host, HostFilter>() {
            @Override
            public JObject<HostKey, Host, HostFilter> create(boolean hasPopup, IPicked<HostKey, Host> picked) {

                JExecutor<HostKey, Host, HostFilter> vExecutor = new JExecutor<>(requestHelperProvider, "host");
                JFieldsForHost fields = new JFieldsForHost(JObjectFactory.this, "", null);
                return new JObject<>(fields, vExecutor, hasPopup, picked);
            }
        });

        factory.put(Service.class, new Creator<ServiceKey, Service, ServiceFilter>() {
            @Override
            public JObject<ServiceKey, Service, ServiceFilter> create(boolean hasPopup, IPicked<ServiceKey, Service> picked) {

                JExecutor<ServiceKey, Service, ServiceFilter> vExecutor = new JExecutor<>(requestHelperProvider, "service");
                JFieldsForService fields = new JFieldsForService("", null);
                return new JObject<>(fields, vExecutor, hasPopup, picked);
            }
        });

        factory.put(ReleaseGroup.class, new Creator<ReleaseGroupKey, ReleaseGroup, ReleaseGroupFilter>() {
            @Override
            public JObject<ReleaseGroupKey, ReleaseGroup, ReleaseGroupFilter> create(boolean hasPopup, IPicked<ReleaseGroupKey, ReleaseGroup> picked) {

                JExecutor<ReleaseGroupKey, ReleaseGroup, ReleaseGroupFilter> vExecutor = new JExecutor<>(requestHelperProvider, "releaseGroup");
                JFieldsForReleaseGroup fields = new JFieldsForReleaseGroup("", null);
                return new JObject<>(fields, vExecutor, hasPopup, picked);
            }
        });

        factory.put(Tenant.class, new Creator<TenantKey, Tenant, TenantFilter>() {
            @Override
            public JObject<TenantKey, Tenant, TenantFilter> create(boolean hasPopup, IPicked<TenantKey, Tenant> picked) {

                JExecutor<TenantKey, Tenant, TenantFilter> vExecutor = new JExecutor<>(requestHelperProvider, "tenant");
                JFieldsTenant fields = new JFieldsTenant(JObjectFactory.this, "", null);
                return new JObject<>(fields, vExecutor, hasPopup, picked);
            }
        });

    }

    JObject<? extends Key, ?, ?> create(Class _class, boolean hasPopup, IPicked picked) {
        Creator<? extends Key, ?, ?> got = factory.get(_class);
        return got.create(hasPopup, picked);
    }

    static interface Creator<K extends Key, V extends Stored, F extends KeyValueFilter<K, V>> {

        JObject<K, V, F> create(boolean hasPopup, IPicked<K, V> picked);
    }
}
