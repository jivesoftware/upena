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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.Key;
import com.jivesoftware.os.upena.shared.KeyValueFilter;
import com.jivesoftware.os.upena.shared.Stored;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SpringLayout;
import javax.swing.event.MouseInputAdapter;

public class JExecutor<K extends Key, V extends Stored, F extends KeyValueFilter<K, V>> {

    private final Executor thread = Executors.newSingleThreadExecutor();
    private final RequestHelperProvider requestHelperProvider;
    private final String context;
    private final ObjectMapper mapper = new ObjectMapper();

    public JExecutor(RequestHelperProvider requestHelperProvider, String context) {
        this.requestHelperProvider = requestHelperProvider;
        this.context = context;
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    public JComponent toListItem(final JPanel results,
        final K k,
        final V v,
        final JObjectFields<K, V, F> objectFields,
        final Color background,
        final boolean hasPopup,
        final IPicked<K, V> picked) {

        final JObjectFields<K, V, F> copy = objectFields.copy();
        copy.update(k, v);

        final JPopupMenu menu = new JPopupMenu();
        if (v instanceof Instance) {



            JMenuItem menuItem = new JMenuItem("Tools");
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
//                    final Instance instance = (Instance)v;
//
//                    new InstanceDescriptor(instance.clusterKey.getKey(), , context, context, context, context, context, instanceName, context, context)
//
//                    JFrame f = new JFrame();
//                    f.setTitle("Editing " + copy.shortName(v));
//                    f.add(new JNannyReport(instance, null, context));
//                    f.setPreferredSize(new Dimension(1000, 600));
//                    f.pack();
//                    f.setLocationRelativeTo(null);
//                    f.setVisible(true);
                }
            });
            menu.add(menuItem);
            menu.addSeparator();
        }

        JMenuItem menuItem = new JMenuItem("Filter");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                objectFields.updateFilter(k, v);
                if (picked != null) {
                    picked.picked(k, v);
                }
            }
        });
        menu.add(menuItem);
        menuItem = new JMenuItem("Edit");
        menu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JObjectEditor editor = new JObjectEditor(JObjectEditor.UPDATE, true, results, copy, JExecutor.this, picked);

                        JFrame f = new JFrame();
                        f.setTitle("Editing " + copy.shortName(v));
                        f.add(editor);
                        f.setPreferredSize(new Dimension(1000, 600));
                        f.pack();
                        f.setLocationRelativeTo(null);
                        f.setVisible(true);

                    }
                });
            }
        });
        menu.addSeparator();
        menuItem = new JMenuItem("Copy");
        menu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JObjectEditor editor = new JObjectEditor(JObjectEditor.CREATE, true, results, copy, JExecutor.this, picked);

                        JFrame f = new JFrame();
                        f.setTitle("Copying " + copy.shortName(v));
                        f.add(editor);
                        f.setPreferredSize(new Dimension(1000, 600));
                        f.pack();
                        f.setLocationRelativeTo(null);
                        f.setVisible(true);
                    }
                });
            }
        });
        menu.addSeparator();
        menuItem = new JMenuItem("Remove");
        menu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (picked != null) {
                    Util.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            remove(copy, results);
                        }
                    });
                    picked.picked(null, null);
                }
            }
        });

        final List<JComponent> allViews = new ArrayList<>();
        Collection<JField> values = copy.objectFields().values();
        for (JField f : values) {
            final JComponent viewer = f.getViewer(48);
            allViews.add(viewer);
            viewer.setOpaque(true);
            viewer.setBackground(background);
            viewer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            viewer.addMouseListener(new MouseInputAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (hasPopup) {
                        menu.show(viewer, e.getX(), e.getY());
                    } else {
                        if (picked != null) {
                            picked.picked(k, v);
                        }
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    super.mouseEntered(e);
                    for (JComponent a : allViews) {
                        a.setBackground(new Color(240, 240, 255));
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    super.mouseExited(e);
                    for (JComponent a : allViews) {
                        a.setBackground(background);
                    }
                }
            });
            results.add(viewer);
        }
        return null;
    }



    public BufferedImage createImage(JComponent c) {

        Dimension size = c.getSize();
        if (size.width <= 0 || size.height <= 0) {
            size = c.getPreferredSize();
        }
        BufferedImage bi = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        c.print(g);
        return bi;
    }

    public void get(final Class<V> valueClass, final K key, final JPanel viewResults, final IPicked<K, V> picked) {
        if (picked != null) {
            thread.execute(new Runnable() {
                @Override
                public void run() {
                    V result = requestHelperProvider.get().executeRequest(key, "/upena/" + context + "/get", valueClass, null);
                    if (result != null) {
                        picked.picked(key, result);
                    } else {
                        picked.picked(null, null);
                    }
                    if (viewResults != null && viewResults.getParent() != null) {
                        viewResults.getParent().revalidate();
                        viewResults.getParent().repaint();
                    }
                }
            });
        }
    }

    public void remove(final JObjectFields<K, V, F> objectFields, final JPanel viewResults) {
        thread.execute(new Runnable() {
            @Override
            public void run() {

                Boolean key = requestHelperProvider.get().executeRequest(objectFields.key(), "/upena/" + context + "/remove", Boolean.class, null);
                if (key != null) {
                    viewResults.removeAll();
                    viewResults.add(new JLabel("Removed:" + key));
                    viewResults.revalidate();
                } else {
                    viewResults.removeAll();
                    viewResults.add(new JLabel("Failed to remove:" + key));
                    viewResults.revalidate();
                }
                viewResults.getParent().revalidate();
                viewResults.getParent().repaint();
            }
        });
    }

    public void create(final JObjectFields<K, V, F> objectFields, final IPicked<K, V> picked) {
        thread.execute(new Runnable() {
            @Override
            public void run() {
                V v = objectFields.fieldsToObject();
                K key = requestHelperProvider.get().executeRequest(v, "/upena/" + context + "/add", objectFields.keyClass(), null);
                if (key != null) {
                    V result = requestHelperProvider.get().executeRequest(key, "/upena/" + context + "/get", objectFields.valueClass(), null);
                    if (result != null) {
                        picked.picked(key, v);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, v.toString(), "Failed to Create", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

    }

    public void update(final JObjectFields<K, V, F> objectFields, final IPicked<K, V> picked) {
        thread.execute(new Runnable() {
            @Override
            public void run() {
                V v = objectFields.fieldsToObject();
                String objectKey = objectFields.key().getKey();
                K key = requestHelperProvider.get().executeRequest(v, "/upena/" + context + "/update?key=" + objectKey, objectFields.keyClass(), null);
                if (key != null) {
                    V result = requestHelperProvider.get().executeRequest(key, "/upena/" + context + "/get", objectFields.valueClass(), null);
                    if (result != null) {
                        picked.picked(key, v);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, v.toString(), "Failed to Create", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

    }

    public void find(final F filter, final JObjectFields<K, V, F> objectFields, final boolean hasPopup, final JPanel viewResults, final IPicked<K, V> picked) {

        thread.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ConcurrentSkipListMap<String, TimestampedValue<V>> results = requestHelperProvider.get().executeRequest(filter,
                        "/upena/" + context + "/find", objectFields.responseClass(), null);
                    if (results != null) {
                        viewResults.removeAll();

                        int columns = addTitles(viewResults, objectFields);

                        int count = 0;
                        for (final Map.Entry<String, TimestampedValue<V>> e : results.entrySet()) {
                            if (!e.getValue().getTombstoned()) {
                                Color color = Color.white;
                                if (count % 2 == 0) {
                                    color = Color.lightGray;
                                }
                                toListItem(viewResults, objectFields.key(e.getKey()), e.getValue().getValue(), objectFields, color, hasPopup, picked);
                            }
                            count++;
                        }
                        SpringUtils.makeCompactGrid(viewResults, count + 1, columns, 0, 0, 0, 0);

                        viewResults.revalidate();
                        viewResults.repaint();
                    } else {
                        viewResults.removeAll();
                        viewResults.add(new JLabel("No results"));
                        viewResults.revalidate();
                    }
                    viewResults.getParent().revalidate();
                    viewResults.getParent().repaint();

                } catch (Exception x) {
                    x.printStackTrace();
                }
            }
        });
    }

    int addTitles(JComponent viewResults, final JObjectFields<K, V, F> objectFields) {
        Collection<JField> values = objectFields.objectFields().values();
        JPanel m = new JPanel(new SpringLayout());
        for (JField f : values) {
            JLabel jLabel = new JLabel(f.name());
            jLabel.setFont(new Font("system", Font.ITALIC, 12));
            jLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            jLabel.setOpaque(true);
            jLabel.setBackground(Color.darkGray);
            jLabel.setForeground(Color.white);
            viewResults.add(jLabel);
        }
        return values.size();
    }
}
