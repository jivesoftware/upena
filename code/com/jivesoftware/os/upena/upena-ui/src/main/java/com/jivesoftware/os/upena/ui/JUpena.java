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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.jive.utils.http.client.HttpClient;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfig;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfiguration;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactory;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.jive.utils.http.client.rest.RequestHelper;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

public class JUpena extends javax.swing.JFrame {

    private final Map<String, JUpenaServices> jUpenaServices = new HashMap<>();
    private final Map<String, JCluster> jClusters = new HashMap<>();

    public JUpena() {
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Upena - (hawaiian 'net' pron. OohPenAh)");

        JPanel connectTo = new JPanel();
        connectTo.setLayout(new BoxLayout(connectTo, BoxLayout.X_AXIS));
        connectTo.add(new JLabel("host:"));
        final JTextField editHost = new JTextField("localhost", 120);
        editHost.setMinimumSize(new Dimension(120, 24));
        editHost.setMaximumSize(new Dimension(120, 24));
        connectTo.add(editHost);
        connectTo.add(Box.createRigidArea(new Dimension(10, 0)));
        connectTo.add(new JLabel("port:"));
        final JTextField editPort = new JTextField("56300", 48);
        editPort.setMinimumSize(new Dimension(120, 24));
        editPort.setMaximumSize(new Dimension(120, 24));
        connectTo.add(editPort);
        connectTo.setOpaque(true);
        connectTo.setBackground(Color.white);

        JPanel v = new JPanel(new SpringLayout());

        final ImageIcon background = Util.icon("cluster");
        JLabel banner = new JLabel(background);
        banner.setPreferredSize(new Dimension(400, 48));
        v.add(banner);
        v.add(connectTo);

        addDeclaredServices(editHost, editPort, v);
        addUba(editHost, editPort, v);

        v.setOpaque(false);
        SpringUtils.makeCompactGrid(v, 4, 1, 10, 10, 10, 10);

        add(v);
        setPreferredSize(new Dimension(400, 300));
        setMaximumSize(new Dimension(400, 300));
        pack();
    }

    RequestHelper buildHelper(String host, String port) {

        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider()
                .createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(host, Integer.parseInt(port));
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new RequestHelper(httpClient, mapper);
    }

    protected void addDeclaredServices(final JTextField editHost, final JTextField editPort, JPanel v) {
        JButton declareServices = new JButton("Service Declarations");
        declareServices.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        String key = editHost.getText() + ":" + editPort.getText();
                        if (jUpenaServices.get(key) == null) {
                            RequestHelper requestHelper = buildHelper(editHost.getText(), editPort.getText());
                            final JObjectFactory factory = new JObjectFactory(requestHelper);
                            JUpenaServices v = new JUpenaServices(requestHelper, factory);
                            v.setTitle("Upena Services " + editHost.getText() + ":" + editPort.getText());
                            v.setLocationRelativeTo(null);
                            v.setVisible(true);
                            jUpenaServices.put(key, v);
                        } else {
                            jUpenaServices.get(key).setVisible(true);
                            jUpenaServices.get(key).toFront();
                        }
                    }
                });
            }
        });
        v.add(declareServices);
    }

    protected void addUba(final JTextField editHost, final JTextField editPort, JPanel v) {
        JButton uba = new JButton("Cluster");
        uba.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        String key = editHost.getText() + ":" + editPort.getText();
                        if (jClusters.get(key) == null) {
                            RequestHelper buildHelper = buildHelper(editHost.getText(), editPort.getText());
                            JObjectFactory factory = new JObjectFactory(buildHelper(editHost.getText(), editPort.getText()));
                            JCluster v = new JCluster(buildHelper, factory);
                            v.setTitle("Cluster " + editHost.getText() + ":" + editPort.getText());
                            v.setLocationRelativeTo(null);
                            v.setVisible(true);
                            jClusters.put(key, v);
                        } else {
                            jClusters.get(key).setVisible(true);
                            jClusters.get(key).toFront();
                        }
                    }
                });
            }
        });
        v.add(uba);
    }
}