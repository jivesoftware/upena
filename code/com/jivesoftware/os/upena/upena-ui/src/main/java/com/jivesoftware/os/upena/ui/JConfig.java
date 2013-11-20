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

import com.jivesoftware.os.amza.shared.TimestampedValue;
import com.jivesoftware.os.jive.utils.http.client.rest.RequestHelper;
import com.jivesoftware.os.upena.config.shared.UpenaConfig;
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
import com.jivesoftware.os.upena.shared.Stored;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpringLayout;
import javax.swing.border.EtchedBorder;
import javax.swing.border.SoftBevelBorder;

public class JConfig extends JPanel {

    RequestHelper requestHelper;
    JObjectFactory factory;
    JPanel viewResults;

    Map<String, UpenaConfig> aConfigs = new ConcurrentSkipListMap<>();
    Map<String, String> aConfigKeys = new ConcurrentSkipListMap<>();

    Map<String, UpenaConfig> bConfigs = new ConcurrentSkipListMap<>();
    Map<String, String> bConfigKeys = new ConcurrentSkipListMap<>();

    FindInstances aFindInstances;
    FindInstances bFindInstances;

    JTextField filterKeys;
    JTextField filterValues;

    public JConfig(RequestHelper requestHelper, JObjectFactory factory) {
        this.requestHelper = requestHelper;
        this.factory = factory;
        aFindInstances = new FindInstances(new Color(255, 245, 240));
        bFindInstances = new FindInstances(new Color(245, 240, 255));
        initComponents();
    }

    private void initComponents() {

        viewResults = new JPanel(new BorderLayout());

        JPanel filter = new JPanel(new SpringLayout());
        filter.add(new JLabel("filter keys:"));
        filter.add(Box.createHorizontalStrut(10));
        filterKeys = new JTextField("", 120);
        filterKeys.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        filter();
                    }
                });
            }
        });
        filter.add(filterKeys);
        filter.add(Box.createHorizontalStrut(10));
        filter.add(new JLabel("filter values:"));
        filterValues = new JTextField("", 120);
        filterValues.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        filter();
                    }
                });
            }
        });
        filter.add(filterValues);

        SpringUtils.makeCompactGrid(filter, 1, 6, 24, 24, 16, 16);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(filter, BorderLayout.NORTH);
        JScrollPane scrollRoutes = new JScrollPane(viewResults,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        panel.add(scrollRoutes, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        setLayout(new BorderLayout(8, 8));

        JPanel avsb = new JPanel(new BorderLayout());
        avsb.add(aFindInstances, BorderLayout.NORTH);
        avsb.add(new JLabel("- vs -"), BorderLayout.CENTER);
        avsb.add(bFindInstances, BorderLayout.SOUTH);
        avsb.setBorder(new SoftBevelBorder(SoftBevelBorder.RAISED));

        add(avsb, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);

        JPanel commit = new JPanel(new FlowLayout());
        JButton commitButton = new JButton("Commit");
        commitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        });
        commit.add(commitButton);
        add(commit, BorderLayout.SOUTH);
    }

    public class FindInstances extends JPanel {

        Color color;
        JEditRef clusterId;
        JEditRef hostId;
        JEditRef serviceId;
        JEditRef releaseGroupId;
        JTextField instanceId;

        public FindInstances(Color color) {
            this.color = color;
            setLayout(new SpringLayout());

            clusterId = new JEditRef(factory, "cluster", Cluster.class, "");
            add(clusterId.getEditor(40, new IPicked() {
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
            clusterId.setValue("");

            hostId = new JEditRef(factory, "host", Host.class, "");
            add(hostId.getEditor(40, new IPicked() {
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
            hostId.setValue("");

            serviceId = new JEditRef(factory, "service", Service.class, "");
            add(serviceId.getEditor(40, new IPicked() {
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
            serviceId.setValue("");

            releaseGroupId = new JEditRef(factory, "release-group", ReleaseGroup.class, "");
            add(releaseGroupId.getEditor(40, new IPicked() {
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
            releaseGroupId.setValue("");

            instanceId = new JTextField("", 120);
            instanceId.addActionListener(new ActionListener() {

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
            add(instanceId);

            JButton clear = new JButton(Util.icon("clear-left"));
            clear.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Util.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            clusterId.setValue("");
                            hostId.setValue("");
                            serviceId.setValue("");
                            releaseGroupId.setValue("");
                            instanceId.setText("");
                            refresh();
                        }
                    });
                }
            });
            add(clear);

            SpringUtils.makeCompactGrid(this, 1, 6, 10, 10, 10, 10);
            setBackground(color);
        }
    }

    Map<String, String> aNames = new HashMap<>();
    Map<String, String> bNames = new HashMap<>();

    public void refresh() {
        aNames.clear();
        refresh(aFindInstances, aConfigKeys, aConfigs, aNames);
        refresh(bFindInstances, bConfigKeys, bConfigs, bNames);
        filter();

    }

    public void refresh(FindInstances find, Map<String, String> configKeys, Map<String, UpenaConfig> configs, Map<String, String> names) {

        try {
            configs.clear();
            configKeys.clear();

            InstanceFilter filter = new InstanceFilter((find.clusterId.getValue().length() > 0) ? new ClusterKey(find.clusterId.getValue()) : null,
                    (find.hostId.getValue().length() > 0) ? new HostKey(find.hostId.getValue()) : null,
                    (find.serviceId.getValue().length() > 0) ? new ServiceKey(find.serviceId.getValue()) : null,
                    (find.releaseGroupId.getValue().length() > 0) ? new ReleaseGroupKey(find.releaseGroupId.getValue()) : null,
                    (find.instanceId.getText().length() > 0) ? Integer.parseInt(find.instanceId.getText()) : null,
                    0, 1000);

            if (filter.clusterKey == null
                    && filter.hostKey == null
                    && filter.serviceKey == null
                    && filter.releaseGroupKey == null
                    && filter.logicalInstanceId == null) {
                return;
            }

            ConcurrentSkipListMap<InstanceKey, TimestampedValue<Instance>> results = requestHelper.executeRequest(filter,
                    "/upena/instance/find", InstanceFilter.Results.class, null);
            if (results != null) {

                for (Map.Entry<InstanceKey, TimestampedValue<Instance>> e : results.entrySet()) {
                    if (!e.getValue().getTombstoned()) {
                        UpenaConfig get = new UpenaConfig("default",
                                e.getKey().getKey(),
                                new HashMap<String, String>());

                        String key = key(get);
                        if (configs.containsKey(key)) {
                            continue;
                        }

                        Instance i = e.getValue().getValue();
                        String k = i.clusterKey.getKey();
                        if (!names.containsKey(k)) {
                            Cluster cluster = get(Cluster.class, new ClusterKey(k), "cluster");
                            if (cluster != null) {
                                names.put(k, cluster.name);
                            }
                        }

                        k = i.hostKey.getKey();
                        if (!names.containsKey(k)) {
                            Host host = get(Host.class, new HostKey(k), "host");
                            if (host != null) {
                                names.put(k, host.name);
                            }
                        }

                        k = i.serviceKey.getKey();
                        if (!names.containsKey(k)) {
                            Service service = get(Service.class, new ServiceKey(k), "service");
                            if (service != null) {
                                names.put(k, service.name);
                            }
                        }

                        k = i.releaseGroupKey.getKey();
                        if (!names.containsKey(k)) {
                            ReleaseGroup releaseGroup = get(ReleaseGroup.class, new ReleaseGroupKey(k), "releaseGroup");
                            if (releaseGroup != null) {
                                names.put(k, releaseGroup.name);
                            }
                        }

                        String name = "";
                        if (filter.clusterKey == null) {
                            name += names.get(i.clusterKey.getKey()) + " - ";
                        }
                        if (filter.hostKey == null) {
                            name += names.get(i.hostKey.getKey()) + " - ";
                        }
                        if (filter.serviceKey == null) {
                            name += names.get(i.serviceKey.getKey()) + " - ";
                        }
                        if (filter.releaseGroupKey == null) {
                            name += names.get(i.releaseGroupKey.getKey()) + " - ";
                        }
                        name += i.instanceId;

                        names.put(e.getKey().getKey(), name);

                        UpenaConfig got = requestHelper.executeRequest(get,
                                "/upenaConfig/get", UpenaConfig.class, null);

                        if (got != null) {
                            for (String pk : got.properties.keySet()) {
                                configKeys.put(pk, pk);
                            }
                            configs.put(key, got);
                        }

                    }
                }
            }

        } catch (Exception x) {
            x.printStackTrace();
        }

    }

    void filter() {
        viewResults.removeAll();
        Map<String, String> allKeys = new ConcurrentSkipListMap<>();
        allKeys.putAll(aConfigKeys);
        allKeys.putAll(bConfigKeys);

        JPanel r = new JPanel(new SpringLayout());
        int count = 0;
        for (String pk : allKeys.keySet()) {

            if (filterKeys.getText().length() > 0) {
                if (!pk.contains(filterKeys.getText())) {
                    continue;
                }
            }

            JPanel aInstances = new JPanel();
            aInstances.setLayout(new BoxLayout(aInstances, BoxLayout.Y_AXIS));
            List<JPanel> aValues = new ArrayList<>();
            for (UpenaConfig config : aConfigs.values()) {
                if (config.properties.containsKey(pk)) {
                    JPanel ev = new JPanel();
                    ev.setOpaque(false);
                    ev.setLayout(new BoxLayout(ev, BoxLayout.X_AXIS));
                    String value = config.properties.get(pk);

                    if (filterValues.getText().length() > 0) {
                        if (!value.contains(filterValues.getText())) {
                            continue;
                        }
                    }
                    JLabel instanceLabel = new JLabel(idToString(config.instanceKey, aNames));
                    ev.add(instanceLabel);
                    JTextField editValue = new JTextField(value);
                    editValue.setMinimumSize(new Dimension(50, 24));
                    editValue.setPreferredSize(new Dimension(100, 24));
                    editValue.setMaximumSize(new Dimension(600, 24));
                    ev.add(editValue);

                    final JToggleButton linked = new JToggleButton("", true);
                    linked.setIcon(Util.icon("linked"));
                    linked.setPressedIcon(Util.icon("unlinked"));
                    linked.setSelectedIcon(Util.icon("linked"));
                    linked.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Util.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    if (linked.isSelected()) {
                                        linked.setIcon(Util.icon("linked"));
                                        linked.setPressedIcon(Util.icon("unlinked"));
                                        linked.setSelectedIcon(Util.icon("linked"));
                                    } else {
                                        linked.setIcon(Util.icon("unlinked"));
                                        linked.setPressedIcon(Util.icon("linked"));
                                        linked.setSelectedIcon(Util.icon("unlinked"));
                                    }
                                }
                            });
                        }
                    });
                    ev.add(linked);

                    JButton copy = new JButton(Util.icon("copy-left"));
                    copy.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Util.invokeLater(new Runnable() {
                                @Override
                                public void run() {

                                }
                            });
                        }
                    });
                    ev.add(copy);

                    aInstances.add(ev);
                    aValues.add(ev);
                }
            }

            JPanel bInstances = new JPanel();
            bInstances.setLayout(new BoxLayout(bInstances, BoxLayout.Y_AXIS));
            List<JPanel> bValues = new ArrayList<>();
            for (UpenaConfig config : bConfigs.values()) {
                if (config.properties.containsKey(pk)) {
                    JPanel ev = new JPanel();
                    ev.setOpaque(false);
                    ev.setLayout(new BoxLayout(ev, BoxLayout.X_AXIS));
                    String value = config.properties.get(pk);

                    if (filterValues.getText().length() > 0) {
                        if (!value.contains(filterValues.getText())) {
                            continue;
                        }
                    }

                    JButton copy = new JButton(Util.icon("copy-right"));
                    copy.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Util.invokeLater(new Runnable() {
                                @Override
                                public void run() {

                                }
                            });
                        }
                    });
                    ev.add(copy);

                    final JToggleButton linked = new JToggleButton("", true);
                    linked.setIcon(Util.icon("linked"));
                    linked.setPressedIcon(Util.icon("unlinked"));
                    linked.setSelectedIcon(Util.icon("linked"));
                    linked.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Util.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    if (linked.isSelected()) {
                                        linked.setIcon(Util.icon("linked"));
                                        linked.setPressedIcon(Util.icon("unlinked"));
                                        linked.setSelectedIcon(Util.icon("linked"));
                                    } else {
                                        linked.setIcon(Util.icon("unlinked"));
                                        linked.setPressedIcon(Util.icon("linked"));
                                        linked.setSelectedIcon(Util.icon("unlinked"));
                                    }
                                }
                            });
                        }
                    });
                    ev.add(linked);

                    JLabel instanceLabel = new JLabel(idToString(config.instanceKey, bNames));
                    JTextField editValue = new JTextField(value);
                    editValue.setMinimumSize(new Dimension(50, 24));
                    editValue.setPreferredSize(new Dimension(100, 24));
                    editValue.setMaximumSize(new Dimension(600, 24));
                    ev.add(editValue);
                    ev.add(instanceLabel);
                    bInstances.add(ev);
                    bValues.add(ev);
                }
            }

            if (!aValues.isEmpty() || !bValues.isEmpty()) {

                aInstances.setOpaque(true);
                bInstances.setOpaque(true);

                if (count % 2 == 0) {
                    aInstances.setBackground(aFindInstances.color.darker());
                    bInstances.setBackground(bFindInstances.color.darker());
                } else {
                    aInstances.setBackground(aFindInstances.color);
                    bInstances.setBackground(bFindInstances.color);
                }

                JPanel v = new JPanel();
                v.setLayout(new BoxLayout(v, BoxLayout.X_AXIS));
                v.add(aInstances);
                v.add(bInstances);

                JPanel p = new JPanel();
                p.setLayout(new BorderLayout());
                JLabel keyLabel = new JLabel(pk);
                keyLabel.setFont(new Font("system", Font.BOLD, 16));
                p.add(keyLabel, BorderLayout.PAGE_START);
                p.add(v, BorderLayout.CENTER);
                p.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
                r.add(p);
                count++;
            }

        }

        SpringUtils.makeCompactGrid(r, count, 1, 8, 8, 8, 8);

        viewResults.add(r, BorderLayout.CENTER);
        viewResults.revalidate();
        viewResults.repaint();

        viewResults.getParent().revalidate();
        viewResults.getParent().repaint();
    }

    String key(UpenaConfig config) {
        return config.context + " " + config.instanceKey;
    }

    public String idToString(String id, Map<String, String> names) {
        String name = names.get(id);
        if (name == null) {
            System.out.println("hmm " + id);
            return id;
        }
        return name;
    }

    public <K, V> V get(final Class<V> valueClass, final K key, String context) {
        V v = requestHelper.executeRequest(key, "/upena/" + context + "/get", valueClass, null);
        System.out.println(v);
        return v;
    }
}