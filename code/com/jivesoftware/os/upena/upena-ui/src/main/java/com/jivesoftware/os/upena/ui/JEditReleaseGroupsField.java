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

import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.Stored;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

class JEditReleaseGroupsField implements JField<Map<ServiceKey, ReleaseGroupKey>> {

    JObjectFactory factory;
    public String name;
    public Map<ServiceKey, ReleaseGroupKey> field;
    public JPanel editField;
    public JLabel viewer;

    public JEditReleaseGroupsField(JObjectFactory factory, String name, Map<ServiceKey, ReleaseGroupKey> field) {
        this.factory = factory;
        this.name = name;
        if (field == null) {
            field = new HashMap<>();
        }
        this.field = field;
    }

    @Override
    public boolean isFilterable() {
        return false;
    }

    @Override
    public JComponent getEditor(int w, final IPicked picked) {
        editField = new JPanel(new SpringLayout());
        rebuild();
        return editField;
    }

    void rebuild() {
        if (editField != null) {
            editField.removeAll();

            int count = 1;
            editField.add(new JLabel("Service"));
            editField.add(new JLabel("Release"));
            editField.add(new JLabel(""));

            for (Map.Entry<ServiceKey, ReleaseGroupKey> entry : field.entrySet()) {
                final ServiceKey serviceKey = entry.getKey();
                final ReleaseGroupKey releaseGroupKey = entry.getValue();
                final JEditRef viewService = new JEditRef(factory, "service", Service.class, "");
                editField.add(viewService.getViewer(50));
                viewService.setValue(serviceKey.getKey());

                final JEditRef release = new JEditRef(factory, "release", ReleaseGroup.class, "");
                editField.add(release.getEditor(40, new IPicked() {
                    @Override
                    public void picked(Object key, Stored v) {
                        field.put(serviceKey, (ReleaseGroupKey) key);
                        rebuild();
                    }
                }));
                release.setValue(releaseGroupKey.getKey());

                JButton b = new JButton(Util.icon("clear-left"));
                b.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Util.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                field.remove(serviceKey);
                                rebuild();
                            }
                        });

                    }
                });
                editField.add(b);

                count++;
            }


            final JEditRef service = new JEditRef(factory, "service", Service.class, "");
            editField.add(service.getEditor(40, new IPicked() {
                @Override
                public void picked(Object key, Stored v) {
                    //refresh();
                }
            }));

            final JEditRef release = new JEditRef(factory, "release", ReleaseGroup.class, "");
            editField.add(release.getEditor(40, new IPicked() {
                @Override
                public void picked(Object key, Stored v) {
                    //refresh();
                }
            }));

            JButton b = new JButton("add");
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                     Util.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            field.put(new ServiceKey(service.getValue()), new ReleaseGroupKey(release.getValue()));
                            rebuild();
                        }
                    });
                }
            });
            editField.add(b);

            count++;
            SpringUtils.makeCompactGrid(editField, count, 3, 0, 0, 0, 0);

            editField.revalidate();
            editField.repaint();
            Container parent = editField.getParent();
            if (parent != null) {
                editField.getParent().revalidate();
                editField.getParent().repaint();
            }
        }
    }

    @Override
    public JComponent getViewer(int w) {
        viewer = new JLabel(field.toString());
        return viewer;
    }

    @Override
    public void clear() {
        Map<ServiceKey, ReleaseGroupKey> empty = new HashMap<>();
        setValue(empty);
    }

    @Override
    public void setValue(Map<ServiceKey, ReleaseGroupKey> value) {
        field = value;
        rebuild();
    }

    @Override
    public Map<ServiceKey, ReleaseGroupKey> getValue() {
        return field;
    }

    @Override
    public String getViewValue() {
        return field.toString();
    }

    @Override
    public JComponent getClear(final IPicked picked) {
        JButton b = new JButton(Util.icon("clear-left"));
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        clear();
                        if (picked != null) {
                            picked.picked(null, null);
                        }
                    }
                });
            }
        });
        return b;
    }

    @Override
    public String name() {
        return name;
    }
}