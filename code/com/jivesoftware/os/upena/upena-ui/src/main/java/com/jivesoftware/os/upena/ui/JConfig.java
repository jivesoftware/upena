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
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpringLayout;
import javax.swing.border.EtchedBorder;
import javax.swing.border.SoftBevelBorder;

public class JConfig extends JPanel {

    RequestHelperProvider requestHelperProviderA;
    RequestHelperProvider requestHelperProviderB;
    JObjectFactory factory;
    JPanel viewResults;

    Map<String, String> aNames = new HashMap<>();
    Map<String, String> bNames = new HashMap<>();

    Map<String, String> aConfigKeys = new ConcurrentSkipListMap<>();
    Map<String, DefaultAndOverride> aConfigs = new ConcurrentSkipListMap<>();

    Map<String, String> bConfigKeys = new ConcurrentSkipListMap<>();
    Map<String, DefaultAndOverride> bConfigs = new ConcurrentSkipListMap<>();

    JFilterInstances aFindInstances;
    JFilterInstances bFindInstances;

    JTextField filterKeys;
    JTextField filterValues;
    JToggleButton hideDefaults;

    public JConfig(RequestHelperProvider requestHelperProviderA,
        RequestHelperProvider requestHelperProviderB,
        JObjectFactory factory) {
        this.requestHelperProviderA = requestHelperProviderA;
        this.requestHelperProviderB = requestHelperProviderB;
        this.factory = factory;
        Runnable changed = new Runnable() {

            @Override
            public void run() {
                refresh();
            }
        };
        aFindInstances = new JFilterInstances(factory, new Color(255, 245, 240), changed);
        bFindInstances = new JFilterInstances(factory, new Color(245, 240, 255), changed);
        initComponents();
    }

    private void initComponents() {

        viewResults = new JPanel(new BorderLayout());

        JPanel filter = new JPanel(new SpringLayout());
        hideDefaults = new JToggleButton("Overriden", false);
        hideDefaults.addActionListener(new ActionListener() {

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
        filter.add(hideDefaults);
        filter.add(Box.createHorizontalStrut(10));
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

        SpringUtils.makeCompactGrid(filter, 1, 8, 16, 16, 16, 16);

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
                        CommitValues commitValues = new CommitValues();
                        commitValues.add();
                        final JFrame d = new JFrame("Save config changes?");
                        commitValues.d = d;
                        d.setPreferredSize(new Dimension(800, 600));
                        d.getContentPane().add(commitValues);
                        d.pack();
                        d.setLocationRelativeTo(null);
                        d.setVisible(true);
                    }
                });
            }
        });
        commit.add(commitButton);
        add(commit, BorderLayout.SOUTH);
    }

    class CommitValues extends JPanel {

        JFrame d;
        JPanel commitableValues;

        public CommitValues() {
            setLayout(new BorderLayout(10, 10));

            commitableValues = new JPanel(new BorderLayout());

            JScrollPane scrollRoutes = new JScrollPane(commitableValues,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            add(scrollRoutes, BorderLayout.CENTER);

            JButton save = new JButton("Save");
            save.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Util.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (d != null) {
                                d.setVisible(false);
                            }
                            save();
                        }
                    });
                }
            });
            add(save, BorderLayout.SOUTH);
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        }

        public void add() {
            JPanel changes = new JPanel(new SpringLayout());
            int count = 0;
            for (DefaultAndOverride dao : aConfigs.values()) {
                if (!dao.changes.isEmpty()) {
                    changes.add(new JLabel(idToString(dao.instanceKey(), aNames) + " " + dao.changes));
                    count++;
                }
            }
            for (DefaultAndOverride dao : bConfigs.values()) {
                if (!dao.changes.isEmpty()) {
                    changes.add(new JLabel(idToString(dao.instanceKey(), bNames) + " " + dao.changes));
                    count++;
                }
            }
            SpringUtils.makeCompactGrid(changes, count, 1, 24, 24, 16, 16);
            commitableValues.add(changes, BorderLayout.CENTER);
        }

        public void save() {
            for (DefaultAndOverride dao : aConfigs.values()) {
                if (!dao.changes.isEmpty()) {
                    dao.override.properties.clear();
                    boolean update = false;
                    for (String key : dao.changes.keySet()) {
                        String change = dao.changes.get(key);
                        if (change == null || change.length() == 0) { // clears out override
                        } else if (change.equals(dao.config.properties.get(key))) { // clears out override
                        } else { // apply override
                            dao.override.properties.put(key, change);
                            update = true;
                        }
                    }
                    if (update) {
                        UpenaConfig gotUpdated = requestHelperProviderA.get().executeRequest(dao.override, "/upenaConfig/set", UpenaConfig.class, null);
                        System.out.println("Updated:" + gotUpdated);
                    }
                    boolean remove = false;
                    for (String key : dao.changes.keySet()) {
                        String change = dao.changes.get(key);
                        if (change == null || change.length() == 0) { // clears out override
                            dao.override.properties.put(key, "");
                            remove = true;
                        } else if (change.equals(dao.config.properties.get(key))) { // clears out override
                            dao.override.properties.put(key, "");
                            remove = true;
                        }
                    }
                    if (remove) {
                        UpenaConfig gotRemoved = requestHelperProviderA.get().executeRequest(dao.override, "/upenaConfig/remove", UpenaConfig.class, null);
                        System.out.println("Removed:" + gotRemoved);
                    }
                }
            }
            refresh();
        }

    }

    public void refresh() {
        aNames.clear();
        bNames.clear();
        refresh(aFindInstances, aConfigKeys, aConfigs, aNames, requestHelperProviderA);
        refresh(bFindInstances, bConfigKeys, bConfigs, bNames, requestHelperProviderB);
        filter();

    }

    void refresh(JFilterInstances find,
            Map<String, String> configKeys,
            Map<String, DefaultAndOverride> configs,
            Map<String, String> names,
            RequestHelperProvider helperProvider) {

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

            ConcurrentSkipListMap<InstanceKey, TimestampedValue<Instance>> results = helperProvider.get().executeRequest(filter,
                    "/upena/instance/find", InstanceFilter.Results.class, null);
            if (results != null) {

                for (Map.Entry<InstanceKey, TimestampedValue<Instance>> e : results.entrySet()) {
                    if (!e.getValue().getTombstoned()) {
                        getProperties(e.getKey(), e.getValue().getValue(), configs, names, filter, configKeys, helperProvider);
                    }
                }
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    boolean getProperties(InstanceKey instanceKey,
            Instance instance,
            Map<String, DefaultAndOverride> configs,
            Map<String, String> names,
            InstanceFilter filter,
            Map<String, String> configKeys,
            RequestHelperProvider helperProvider) {
        UpenaConfig get = new UpenaConfig("default",
                instanceKey.getKey(),
                new HashMap<String, String>());

        String key = key(get);
        if (configs.containsKey(key)) {
            return true;
        }
        String k = instance.clusterKey.getKey();
        if (!names.containsKey(k)) {
            Cluster cluster = get(helperProvider, Cluster.class, new ClusterKey(k), "cluster");
            if (cluster != null) {
                names.put(k, cluster.name);
            }
        }
        k = instance.hostKey.getKey();
        if (!names.containsKey(k)) {
            Host host = get(helperProvider, Host.class, new HostKey(k), "host");
            if (host != null) {
                names.put(k, host.name);
            }
        }
        k = instance.serviceKey.getKey();
        if (!names.containsKey(k)) {
            Service service = get(helperProvider, Service.class, new ServiceKey(k), "service");
            if (service != null) {
                names.put(k, service.name);
            }
        }
        k = instance.releaseGroupKey.getKey();
        if (!names.containsKey(k)) {
            ReleaseGroup releaseGroup = get(helperProvider, ReleaseGroup.class, new ReleaseGroupKey(k), "releaseGroup");
            if (releaseGroup != null) {
                names.put(k, releaseGroup.name);
            }
        }
        String name = "";
        if (filter.clusterKey == null) {
            name += names.get(instance.clusterKey.getKey()) + " - ";
        }
        if (filter.hostKey == null) {
            name += names.get(instance.hostKey.getKey()) + " - ";
        }
        if (filter.serviceKey == null) {
            name += names.get(instance.serviceKey.getKey()) + " - ";
        }
        if (filter.releaseGroupKey == null) {
            name += names.get(instance.releaseGroupKey.getKey()) + " - ";
        }
        name += instance.instanceId;
        names.put(instanceKey.getKey(), name);

        UpenaConfig gotDefault = helperProvider.get().executeRequest(get, "/upenaConfig/get", UpenaConfig.class, null);
        UpenaConfig getOverride = new UpenaConfig("override",
                instanceKey.getKey(),
                new HashMap<String, String>());
        UpenaConfig gotOverride = helperProvider.get().executeRequest(getOverride, "/upenaConfig/get", UpenaConfig.class, null);

        if (gotDefault != null && gotOverride != null) {
            for (String pk : gotDefault.properties.keySet()) {
                configKeys.put(pk, pk);
            }
            configs.put(key, new DefaultAndOverride(gotDefault, gotOverride));
        }
        return false;
    }

    void filter() {
        viewResults.removeAll();
        Map<String, String> allKeys = new ConcurrentSkipListMap<>();
        allKeys.putAll(aConfigKeys);
        allKeys.putAll(bConfigKeys);

        JPanel r = new JPanel(new SpringLayout());
        int count = 0;
        for (final String propertyKey : allKeys.keySet()) {

            if (filterKeys.getText().length() > 0) {
                if (!propertyKey.contains(filterKeys.getText())) {
                    continue;
                }
            }

            PropertyValues aPropertyValues = new PropertyValues(propertyKey, aNames, false);
            for (final DefaultAndOverride config : aConfigs.values()) {
                if (config.containsKey(propertyKey)) {
                    String value = config.value(propertyKey);
                    String override = config.override(propertyKey);
                    if (filterValues.getText().length() > 0) {
                        if (!value.contains(filterValues.getText()) && !override.contains(filterValues.getText())) {
                            continue;
                        }
                    }
                    if (hideDefaults.isSelected()) {
                        if (override == null && !config.changes.containsKey(propertyKey)) {
                            continue;
                        }
                    }
                    aPropertyValues.add(config);
                }
            }

            PropertyValues bPropertyValues = new PropertyValues(propertyKey, bNames, true);
            for (final DefaultAndOverride config : bConfigs.values()) {
                if (config.containsKey(propertyKey)) {
                    String value = config.value(propertyKey);
                    String override = config.override(propertyKey);
                    if (filterValues.getText().length() > 0) {
                        if (!value.contains(filterValues.getText()) && !override.contains(filterValues.getText())) {
                            continue;
                        }
                    }
                    if (hideDefaults.isSelected()) {
                        if (override == null && !config.changes.containsKey(propertyKey)) {
                            continue;
                        }
                    }
                    bPropertyValues.add(config);
                }
            }

            if (!aPropertyValues.linkablePropertys.isEmpty() || !bPropertyValues.linkablePropertys.isEmpty()) {

                aPropertyValues.setBackground(aFindInstances.color);
                bPropertyValues.setBackground(bFindInstances.color);

                JPanel v = new JPanel();
                v.setLayout(new BoxLayout(v, BoxLayout.X_AXIS));
                v.add(aPropertyValues);
                v.add(bPropertyValues);

                JPanel p = new JPanel();
                p.setLayout(new BorderLayout());
                JLabel keyLabel = new JLabel(propertyKey);
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

    class PropertyValues extends JPanel {

        private final List<LinkableProperty> linkablePropertys = new ArrayList<>();
        private final String propertyKey;
        private final Map<String, String> idNames;
        private final boolean leftToRight;

        public PropertyValues(String propertyKey, Map<String, String> idNames, boolean leftToRight) {
            this.propertyKey = propertyKey;
            this.idNames = idNames;
            this.leftToRight = leftToRight;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        }

        class LinkableProperty extends JPanel {

            final JToggleButton linked;
            final DefaultAndOverride config;
            final JTextField editValue;

            LinkableProperty(final DefaultAndOverride config) {
                this.config = config;
                setOpaque(false);
                setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

                linked = new JToggleButton("", true);
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

                JLabel instanceLabel = new JLabel(idToString(config.instanceKey(), idNames));

                String override = config.override(propertyKey);
                if (config.changes.containsKey(propertyKey)) {
                    override = config.changes.get(propertyKey);
                }

                editValue = new JTextField(override) {

                    @Override
                    protected void paintBorder(Graphics g) {
                        super.paintBorder(g);
                        if (config.changes.containsKey(propertyKey) || config.override.properties.containsKey(propertyKey)) {
                            g.setColor(Color.orange);
                            g.drawRect(0, 0, getWidth(), getHeight());
                        }
                    }

                };
                editValue.setMinimumSize(new Dimension(50, 24));
                editValue.setPreferredSize(new Dimension(100, 24));
                editValue.setMaximumSize(new Dimension(600, 24));
                editValue.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (linked.isSelected()) {
                            for (LinkableProperty linkableProperty : linkablePropertys) {
                                if (linkableProperty.linked.isSelected()) {
                                    linkableProperty.editValue.setText(editValue.getText());
                                    linkableProperty.config.set(propertyKey, editValue.getText());
                                }
                            }
                        } else {
                            config.set(propertyKey, editValue.getText());
                        }
                    }
                });

                String value = config.value(propertyKey);
                JLabel defaultValue = new JLabel(value);
                defaultValue.setForeground(Color.gray);

                if (leftToRight) {
                    add(linked);
                    add(defaultValue);
                    add(editValue);
                    add(instanceLabel);

                } else {
                    add(instanceLabel);
                    add(editValue);
                    add(defaultValue);
                    add(linked);
                }
            }
        }

        public void add(DefaultAndOverride config) {
            LinkableProperty linkableProperty = new LinkableProperty(config);
            add(linkableProperty);
            linkablePropertys.add(linkableProperty);
        }
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

    public <K, V> V get(RequestHelperProvider helperProvider, final Class<V> valueClass, final K key, String context) {
        V v = helperProvider.get().executeRequest(key, "/upena/" + context + "/get", valueClass, null);
        System.out.println(v);
        return v;
    }

    static class DefaultAndOverride {

        final UpenaConfig config;
        final UpenaConfig override;
        final Map<String, String> changes = new ConcurrentHashMap<>();

        public DefaultAndOverride(UpenaConfig config, UpenaConfig override) {
            this.config = config;
            this.override = override;
        }

        String instanceKey() {
            return config.instanceKey;
        }

        boolean containsKey(String key) {
            if (config.properties.containsKey(key)) {
                return true;
            }
            return override.properties.containsKey(key);
        }

        String value(String key) {
            return config.properties.get(key);
        }

        String override(String key) {
            return override.properties.get(key);
        }

        void set(String key, String value) {
            changes.put(key, value);
        }
    }
}
