package com.jivesoftware.os.upena.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

class JEditField implements JField {

    public String name;
    public String field;
    public JTextField editField;
    public JLabel viewer;

    public JEditField(String name, String field) {
        this.name = name;
        this.field = field;
    }

    @Override
    public boolean isFilterable() {
        return true;
    }

    @Override
    public JComponent getEditor(int w, final IPicked picked) {
        editField = new JTextField(field, w);
        editField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (picked != null) {
                            picked.picked(null, null);
                        }
                    }
                });

            }
        });
        return editField;
    }

    @Override
    public JComponent getViewer(int w) {
        viewer = new JLabel(field);
        return viewer;
    }

    public String getValue() {
        field = editField.getText();
        return field;
    }

    @Override
    public void clear() {
        setValue("");
    }

    public void setValue(String value) {
        field = value;
        if (editField != null) {
            editField.setText(value);
            editField.revalidate();
        }
        if (viewer != null) {
            viewer.setText(value);
            viewer.revalidate();
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