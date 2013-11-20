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

import com.jivesoftware.os.jive.utils.http.client.rest.RequestHelper;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.Key;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.Tenant;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class JUpenaServices extends javax.swing.JFrame {

    RequestHelper requestHelper;
    JObjectFactory factory;

    public JUpenaServices(RequestHelper requestHelper, JObjectFactory factory) {
        this.requestHelper = requestHelper;
        this.factory = factory;
        initComponents();
    }

    private void initComponents() {

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.SCROLL_TAB_LAYOUT);
        final JObject<? extends Key, ?, ?> instances = factory.create(Instance.class, true, null);
        tabbedPane.add("Instances", instances);
        tabbedPane.add("Clusters", factory.create(Cluster.class, true, null));
        tabbedPane.add("Releases", factory.create(ReleaseGroup.class, true, null));
        tabbedPane.add("Hosts", factory.create(Host.class, true, null));
        tabbedPane.add("Services", factory.create(Service.class, true, null));
        tabbedPane.add("Tenants", factory.create(Tenant.class, true, null));

        JRoutes routes = new JRoutes(requestHelper, factory);
        tabbedPane.add("Routes", routes);

        JConfig config = new JConfig(requestHelper, factory);
        tabbedPane.add("Config", config);

        JAmza amza = new JAmza(requestHelper);
        tabbedPane.add("Admin", amza);
        tabbedPane.setPreferredSize(new Dimension(800, 250));

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

        JPanel m = new JPanel();
        m.setLayout(new BorderLayout(8, 8));

        m.add(tabbedPane, BorderLayout.CENTER);

        add(m);
        setSize(1024, 768);
        setPreferredSize(new Dimension(1024, 768));
        pack();

        Util.invokeLater(new Runnable() {
            @Override
            public void run() {
                instances.clear();
                instances.find(null);
            }
        });
    }
}