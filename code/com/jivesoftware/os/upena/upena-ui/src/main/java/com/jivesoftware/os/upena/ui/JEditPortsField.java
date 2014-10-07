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

import com.jivesoftware.os.upena.shared.Instance;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

class JEditPortsField implements JField<Map<String, Instance.Port>> {

    JObjectFactory factory;
    public String name;
    public Map<String, Instance.Port> field;
    public JPanel editField;
    public JLabel viewer;

    public JEditPortsField(JObjectFactory factory, String name, Map<String, Instance.Port> field) {
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
            editField.add(new JLabel("PortName"));
            editField.add(new JLabel("PortNumber"));
            editField.add(new JLabel(""));

            for (Map.Entry<String, Instance.Port> entry : field.entrySet()) {
                final Instance.Port port = entry.getValue();
                final JTextField portName = new JTextField(entry.getKey());
                editField.add(portName);
                final JTextField portNumber = new JTextField("" + port.port);
                editField.add(portNumber);

                JButton b = new JButton(Util.icon("clear-left"));
                b.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Util.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                field.remove(portName.getText());
                                rebuild();
                            }
                        });

                    }
                });
                editField.add(b);

                count++;
            }

            final JTextField portName = new JTextField("");
            editField.add(portName);
            final JTextField portNumber = new JTextField("");
            editField.add(portNumber);
            JButton b = new JButton("add");
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Util.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            Map<String, String> properties = new HashMap<>();
                            Instance.Port port = new Instance.Port(Integer.parseInt(portNumber.getText()), properties);
                            field.put(portName.getText(), port);
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
        Map<String, Instance.Port> empty = new HashMap<>();
        setValue(empty);
    }

    @Override
    public Map<String, Instance.Port> getValue() {
        return field;
    }

    @Override
    public String getViewValue() {
        return field.toString();
    }
    @Override
    public void setValue(Map<String, Instance.Port> value) {
        field = value;
        rebuild();
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
