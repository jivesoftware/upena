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

import com.jivesoftware.os.upena.shared.Key;
import com.jivesoftware.os.upena.shared.KeyValueFilter;
import com.jivesoftware.os.upena.shared.Stored;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.border.SoftBevelBorder;

public class JObjectEditor<K extends Key, V extends Stored, F extends KeyValueFilter<K, V>>
        extends JPanel {

    public static final int CREATE = 0;
    public static final int UPDATE = 1;
    final JObjectFields<K, V, F> objectFields;
    final JExecutor<K, V, F> executor;
    final IPicked<K, V> picked;
    JButton create;
    JButton update;
    final int mode;

    public JObjectEditor(
            final int mode,
            final boolean closeOnChange,
            final JPanel viewResults,
            final JObjectFields<K, V, F> objectFields,
            final JExecutor<K, V, F> executor,
            final IPicked<K, V> picked) {

        this.mode = mode;
        this.objectFields = objectFields;
        this.executor = executor;
        this.picked = picked;

        JPanel inputMenu = new JPanel(new SpringLayout());

        if (mode == CREATE) {
            create = new JButton(" create ");
            create.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    executor.create(objectFields, picked);
                    if (closeOnChange) {
                        ((JFrame) getTopLevelAncestor()).dispose();
                    }

                }
            });

            inputMenu.add(create);
        }

        if (mode == UPDATE) {

            update = new JButton(" update ");
            update.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    executor.update(objectFields, picked);
                    if (closeOnChange) {
                        if (closeOnChange) {
                            ((JFrame) getTopLevelAncestor()).dispose();
                        }
                    }
                }
            });
            inputMenu.add(update);
        }

        SpringUtils.makeCompactGrid(inputMenu, 1, 1, 6, 6, 6, 6);

        JPanel inputView = inputView();
        JScrollPane scrollInput = new JScrollPane(inputView,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        //scrollInput.setMinimumSize(new Dimension(800, 1));
        JPanel input = new JPanel(new BorderLayout(16, 16));
        input.add(scrollInput, BorderLayout.CENTER);
        input.add(inputMenu, BorderLayout.SOUTH);
        input.setOpaque(true);
        input.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        setLayout(new BorderLayout());
        add(input, BorderLayout.CENTER);

    }

    public void get(K key, final JPanel viewResults, IPicked<K, V> picked) {
        executor.get(objectFields.valueClass(), key, picked);
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

            if (mode == CREATE && f instanceof JEditKeyField) {
                f.getClear(null);
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
                }
            });
            editor.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            input.add(editor, c);
            c.weightx = 0;
            c.gridx = 2;
            input.add(f.getClear(new IPicked() {
                @Override
                public void picked(Object key, Stored v) {
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
