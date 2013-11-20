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
import com.jivesoftware.os.upena.routing.shared.ConnectionDescriptor;
import com.jivesoftware.os.upena.routing.shared.ConnectionDescriptorsRequest;
import com.jivesoftware.os.upena.routing.shared.ConnectionDescriptorsResponse;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceFilter;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.Stored;
import com.jivesoftware.os.upena.shared.Tenant;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import org.apache.commons.lang.mutable.MutableObject;

public class JRoutes extends JPanel {

    RequestHelper requestHelper;
    JObjectFactory factory;
    JPanel viewResults;
    JEditRef tenantId;
    JEditRef instanceId;
    JEditRef connectToServiceNamed;
    JTextField portName;

    public JRoutes(RequestHelper requestHelper, JObjectFactory factory) {
        this.requestHelper = requestHelper;
        this.factory = factory;
        initComponents();
    }

    private void initComponents() {

        viewResults = new JPanel(new SpringLayout());
        viewResults.setPreferredSize(new Dimension(800, 400));

        JPanel m = new JPanel(new SpringLayout());

        m.add(new JLabel("tenant-id"));
        m.add(new JLabel("instance"));
        m.add(new JLabel("connect-to"));
        m.add(new JLabel("port-name"));
        m.add(new JLabel(""));

        tenantId = new JEditRef(factory, "tenant", Tenant.class, "");
        m.add(tenantId.getEditor(40, new IPicked() {
            @Override
            public void picked(Object key, Stored v) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }
        }));
        tenantId.setValue("");

        instanceId = new JEditRef(factory, "instance", Instance.class, "");
        m.add(instanceId.getEditor(40, new IPicked() {
            @Override
            public void picked(Object key, Stored v) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }
        }));
        instanceId.setValue("");

        connectToServiceNamed = new JEditRef(factory, "service", Service.class, "");
        m.add(connectToServiceNamed.getEditor(40, new IPicked() {
            @Override
            public void picked(Object key, Stored v) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }
        }));
        connectToServiceNamed.setValue("");

        portName = new JTextField("main", 120);
        portName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }
        });
        portName.setMinimumSize(new Dimension(120, 24));
        portName.setMaximumSize(new Dimension(120, 24));
        m.add(portName);

        JButton refresh = new JButton(Util.icon("refresh"));
        refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }
        });
        m.add(refresh);

        SpringUtils.makeCompactGrid(m, 2, 5, 24, 24, 16, 16);

        JPanel routes = new JPanel(new BorderLayout(10, 10));
        routes.add(m, BorderLayout.NORTH);

        JScrollPane scrollRoutes = new JScrollPane(viewResults,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        routes.add(scrollRoutes, BorderLayout.CENTER);
        routes.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        setLayout(new BorderLayout(8, 8));
        add(routes, BorderLayout.CENTER);

    }

    public void refresh() {

        final MutableObject serviceName = new MutableObject("");
        JExecutor<ServiceKey, Service, ServiceFilter> vExecutor = new JExecutor<>(requestHelper, "service");
        final CountDownLatch latch = new CountDownLatch(1);
        vExecutor.get(Service.class, new ServiceKey(connectToServiceNamed.getValue()), new JPanel(), new IPicked<ServiceKey, Service>() {
            @Override
            public void picked(ServiceKey key, Service v) {
                if (v != null) {
                    System.out.println(v.name);
                    serviceName.setValue(v.name);
                }
                latch.countDown();
            }
        });
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (Exception x) {
            viewResults.removeAll();
            viewResults.add(new JLabel("Failed to find service name."));
            viewResults.revalidate();
            return;
        }

        ConnectionDescriptorsRequest connectionDescriptorsRequest = new ConnectionDescriptorsRequest(tenantId.getValue(),
                instanceId.getValue(), serviceName.getValue().toString(), portName.getText());
        ConnectionDescriptorsResponse connectionDescriptorsResponse = requestHelper.executeRequest(connectionDescriptorsRequest,
                "/upena/request/connections", ConnectionDescriptorsResponse.class, null);

        if (connectionDescriptorsResponse != null) {
            viewResults.removeAll();
            int count = 0;

            if (connectionDescriptorsResponse.getConnections() != null) {
                for (ConnectionDescriptor e : connectionDescriptorsResponse.getConnections()) {
                    viewResults.add(new JLabel("Route:" + e.getHost() + ":" + e.getPort() + " " + e.getProperties()));
                    count++;
                }
            }
            for (String e : connectionDescriptorsResponse.getMessages()) {
                viewResults.add(new JLabel("Message:" + e));
                count++;
            }
            SpringUtils.makeCompactGrid(viewResults, count, 1, 0, 0, 0, 0);
            viewResults.revalidate();
            viewResults.repaint();
        } else {
            viewResults.removeAll();
            viewResults.add(new JLabel("No results"));
            viewResults.revalidate();
        }
        viewResults.getParent().revalidate();
        viewResults.getParent().repaint();
    }
}