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
import com.jivesoftware.os.amza.shared.KeyValueFilter;
import com.jivesoftware.os.upena.shared.Key;
import com.jivesoftware.os.upena.shared.Stored;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.SoftBevelBorder;

public class JObject<K extends Key, V extends Stored, F extends KeyValueFilter<K, V>>
        extends JPanel {

    final JPanel viewResults;
    final JObjectFields<K, V, F> objectFields;
    final JExecutor<K, V, F> executor;
    final IPicked<K, V> picked;
    final JButton reset;
    final JButton filter;
    final JTextField query;
    final boolean hasPopup;

    public JObject(
            final JObjectFields<K, V, F> objectFields,
            final JExecutor<K, V, F> executor,
            final boolean hasPopup,
            final IPicked<K, V> picked) {

        this.viewResults = new JPanel(new SpringLayout());;
        this.objectFields = objectFields;
        this.executor = executor;
        this.picked = picked;
        this.hasPopup = hasPopup;

        final JPopupMenu filterMenu = new JPopupMenu();
        filterMenu.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel inputView = inputView();
        JScrollPane scrollInput = new JScrollPane(inputView,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        filterMenu.add(scrollInput);

        JPanel menu = new JPanel(new SpringLayout());

        JButton create = new JButton(" create ");
        create.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JObjectEditor editor = new JObjectEditor(JObjectEditor.CREATE, true,
                                new JPanel(), objectFields.copy(), executor, new IPicked<K, V>() {
                            @Override
                            public void picked(K key, V v) {
                                find(null);
                            }
                        });

                        JFrame f = new JFrame();
                        f.setTitle("New " + objectFields.valueClass().getSimpleName());
                        f.add(editor);
                        f.setPreferredSize(new Dimension(1000, 600));
                        f.pack();
                        f.setLocationRelativeTo(null);
                        f.setVisible(true);
                    }
                });
            }
        });
        menu.add(create);

        filter = new JButton(" filter ");
        filter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        find(null);
                        filterMenu.show(filter, filter.getX(), filter.getY());
                    }
                });
            }
        });
        menu.add(filter);


        query = new JTextField("", 50);
        query.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        F filterObject = null;
                        try {
                            filterObject = new ObjectMapper().readValue(query.getText(), objectFields.filterClass());
                        } catch (Exception x) {
                            x.printStackTrace();
                        }
                        find(filterObject);
                    }
                });

            }
        });
        menu.add(query);

        reset = new JButton(Util.icon("clear-left"));
        reset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        for (JField f : objectFields.objectFields().values()) {
                            f.clear();
                        }
                        find(null);
                    }
                });

            }
        });
        menu.add(reset);

        JButton refresh = new JButton(Util.icon("refresh"));
        refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                find(null);
            }
        });
        menu.add(refresh);
        menu.setMinimumSize(new Dimension(100, 36));
        menu.setMaximumSize(new Dimension(1600, 36));
        SpringUtils.makeCompactGrid(menu, 1, 5, 6, 6, 6, 6);


        JScrollPane scrollResult = new JScrollPane(viewResults);
        scrollResult.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollResult.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);


        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(menu);
        add(scrollResult);

    }

    public void clear() {
    }

    public void find(final F filter) {
        Util.invokeLater(new Runnable() {
            @Override
            public void run() {
                F f = filter;
                if (filter == null) {
                    f = objectFields.fieldsToFilter();
                }
                String filterString = "";
                try {
                    filterString = new ObjectMapper().writeValueAsString(f);
                } catch (Exception x) {
                    filterString = x.getMessage();
                }
                query.setText(filterString);
                viewResults.getParent().revalidate();
                viewResults.getParent().repaint();

                final F find = f;
                executor.find(find, objectFields, hasPopup, viewResults, new IPicked<K, V>() {
                    @Override
                    public void picked(K key, V v) {
                        find(find);
                        if (picked != null) {
                            picked.picked(key, v);
                        }
                    }
                });
            }
        });

    }

    public void get(K key, IPicked<K, V> picked) {
        executor.get(objectFields.valueClass(), key, viewResults, picked);
    }

    public JPanel inputView() {
        Map<String, JField> fields = objectFields.objectFields();
        JPanel input = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.ipadx = 4;
        c.ipady = 4;
        c.anchor = GridBagConstraints.WEST;
        int r = 0;
        for (JField f : fields.values()) {
            if (!f.isFilterable()) {
                continue;
            }
            c.weightx = 0;
            c.gridy = r;
            c.gridx = 0;
            input.add(new JLabel(f.name()), c);
            c.weightx = 1;
            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;

            JComponent editor = f.getEditor(50, new IPicked() {
                @Override
                public void picked(Object key, Stored v) {
                    find(null);
                }
            });
            editor.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            input.add(editor, c);

            c.weightx = 0;
            c.gridx = 2;
            input.add(f.getClear(new IPicked() {
                @Override
                public void picked(Object key, Stored v) {
                    find(null);
                }
            }), c);
            r++;
        }
        input.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel padded = new JPanel();
        SoftBevelBorder softBevelBorder = new SoftBevelBorder(SoftBevelBorder.LOWERED);
        padded.setBorder(softBevelBorder);
        padded.add(input);
        return padded;
    }
}