package com.jivesoftware.os.upena.ui;

import javax.swing.JComponent;

public interface JField {

    JComponent getEditor(int w, IPicked picked);

    JComponent getViewer(int w);

    void clear();

    JComponent getClear(IPicked picked);

    String name();

    boolean isFilterable();
}