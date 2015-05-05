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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

public class JEditKeyField implements JField<String> {

    public String name;
    public String field;
    public JButton editField;

    public JEditKeyField(String name, String field) {
        this.name = name;
        this.field = field;
    }

    @Override
    public boolean isFilterable() {
        return false;
    }

    @Override
    public JComponent getEditor(int w, final IPicked picked) {
        editField = new JButton(field);
        editField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (picked != null) {
                            picked.picked(null, null);
                        }
                        StringSelection selection = new StringSelection(field);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(selection, selection);
                    }
                });

            }
        });
        return editField;
    }

    @Override
    public JComponent getViewer(int w) {
        return new JLabel(field);
    }

    @Override
    public String getValue() {
        return field;
    }

    @Override
    public String getViewValue() {
        return field;
    }

    @Override
    public void clear() {
        setValue("");
    }

    @Override
    public void setValue(String value) {
        field = value;
        if (editField != null) {
            editField.setText(value);
            editField.revalidate();
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