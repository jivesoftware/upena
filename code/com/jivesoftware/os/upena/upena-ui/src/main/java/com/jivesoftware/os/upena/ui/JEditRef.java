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

import com.google.common.cache.LoadingCache;
import com.jivesoftware.os.upena.shared.Key;
import com.jivesoftware.os.upena.shared.Stored;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class JEditRef implements JField<String> {

    public JObjectFactory factory;
    public String name;
    public Class valueClass;
    public String ref;
    public String viewValue;
    public JButton editField;
    public JLabel viewField;

    public JEditRef(JObjectFactory factory, String name, Class valueClass, String ref) {
        this.factory = factory;
        this.name = name;
        this.valueClass = valueClass;
        this.ref = ref;
        this.viewValue = "null";
    }

    @Override
    public boolean isFilterable() {
        return true;
    }

    @Override
    public JComponent getEditor(int w, final IPicked picked) {
        editField = new JButton(viewValue);
        editField.setBackground(Util.getHashSolid(valueClass.getSimpleName()));
        editField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String title = "Choose a " + valueClass.getSimpleName();

                final JFrame d = new JFrame(title);
                final AtomicReference<JObject> hack = new AtomicReference<>();
                JObject<? extends Key, ?, ?> create = factory.create(valueClass, false, new IPicked() {
                    @Override
                    public void picked(Object key, Stored v) {
                        d.setVisible(false);
                        if (key == null) {
                            setValue("");
                        } else {
                            setValue(key.toString()); // hack
                        }
                        if (picked != null) {
                            picked.picked(key, v);
                        }
                    }
                });
                hack.set(create);
                JPanel p = new JPanel(new BorderLayout());
                p.add(create, BorderLayout.CENTER);
                p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                d.setPreferredSize(new Dimension(800, 600));
                d.getContentPane().add(p);
                d.pack();
                d.setLocationRelativeTo(null);
                d.setVisible(true);
                create.find(null);
            }
        });
        return editField;
    }

    @Override
    public JComponent getViewer(int w) {
        viewField = new JLabel(viewValue);
        return viewField;
    }

    @Override
    public String getValue() {
        return ref;
    }

    @Override
    public String getViewValue() {
        return viewValue;
    }

    @Override
    public void clear() {
        setValue("");
    }

    @Override
    public void setValue(String value) {
        ref = value;
        if (value == null || value.length() == 0) {
            viewValue = "null";
            if (editField != null) {
                editField.setText("Pick a " + valueClass.getSimpleName());
                editField.revalidate();
            }
            if (viewField != null) {
                viewField.setText("Pick a " + valueClass.getSimpleName());
                viewField.revalidate();
            }
        } else {
            try {
                JObject vobjects = factory.create(valueClass, false, null);
                LoadingCache<String, Object> cache = factory.getCache(valueClass);
                Stored v = (Stored)cache.get(value);
                if (v != null) {

                    String shortName = vobjects.objectFields.shortName(v);
                    viewValue = shortName;
                    if (editField != null) {
                        editField.setText(shortName);
                        editField.revalidate();
                        Container parent = editField.getParent();
                        if (parent != null) {
                            parent.revalidate();
                        }
                    }
                    if (viewField != null) {
                        viewField.setText(viewValue);
                        viewField.revalidate();
                        Container parent = viewField.getParent();
                        if (parent != null) {
                            parent.revalidate();
                        }
                    }
                }

            } catch (ExecutionException ex) {
                ex.printStackTrace();
            }
        }
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
                        setValue("");
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
