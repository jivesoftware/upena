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
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.Key;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.Tenant;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class JUpenaServices extends JPanel {

    RequestHelperProvider requestHelperProviderA;
    RequestHelperProvider requestHelperProviderB;
    JObjectFactory factory;

    public JUpenaServices(RequestHelperProvider requestHelperProviderA,
        RequestHelperProvider requestHelperProviderB,
        JObjectFactory factory) {
        this.requestHelperProviderA = requestHelperProviderA;
        this.requestHelperProviderB = requestHelperProviderB;
        this.factory = factory;
        initComponents();
    }

    private void initComponents() {

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.SCROLL_TAB_LAYOUT);
        JCluster cluster = new JCluster(requestHelperProviderA, factory);
        tabbedPane.add("Status", cluster);

        final JObject<? extends Key, ?, ?> instances = factory.create(Instance.class, true, null);
        tabbedPane.add("Instances", instances);
        tabbedPane.add("Clusters", factory.create(Cluster.class, true, null));
        tabbedPane.add("Releases", factory.create(ReleaseGroup.class, true, null));
        tabbedPane.add("Hosts", factory.create(Host.class, true, null));
        tabbedPane.add("Services", factory.create(Service.class, true, null));

        JConfig config = new JConfig(requestHelperProviderA, requestHelperProviderB, factory);
        tabbedPane.add("Config", config);

        tabbedPane.add(new JSeparator(SwingConstants.VERTICAL));

        tabbedPane.add("Tenants", factory.create(Tenant.class, true, null));

        JRoutes routes = new JRoutes(requestHelperProviderA, factory);
        tabbedPane.add("Routes", routes);

        tabbedPane.add(new JSeparator(SwingConstants.VERTICAL));

        JAmza amza = new JAmza(requestHelperProviderA);
        tabbedPane.add("Admin", amza);

        tabbedPane.setPreferredSize(new Dimension(800, 800));

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JTabbedPane jTabbedPane = (JTabbedPane) e.getSource();
                        if (jTabbedPane.getSelectedComponent() instanceof JObject) {
                            JObject jObject = (JObject) jTabbedPane.getSelectedComponent();
                            jObject.clear();
                            jObject.find(null);
                        }
                    }
                });

            }
        });
        tabbedPane.setOpaque(true);
        tabbedPane.setBackground(Color.white);

        JPanel m = new JPanel();
        m.setLayout(new BorderLayout(8, 8));

        m.add(tabbedPane, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(m, BorderLayout.CENTER);
//        setSize(1024, 768);
//        setPreferredSize(new Dimension(1024, 768));

        Util.invokeLater(new Runnable() {
            @Override
            public void run() {
                instances.clear();
                instances.find(null);
            }
        });
    }
}
