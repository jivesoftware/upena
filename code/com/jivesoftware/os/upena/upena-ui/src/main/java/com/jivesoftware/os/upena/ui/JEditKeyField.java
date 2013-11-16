package com.jivesoftware.os.upena.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

public class JEditKeyField implements JField {

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

    public String getValue() {
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