/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.upena.ui;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.jivesoftware.os.upena.shared.Key;
import com.jivesoftware.os.upena.shared.KeyValueFilter;
import com.jivesoftware.os.upena.shared.Stored;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author jonathan.colt
 * @param <K>
 * @param <V>
 * @param <F>
 */
public class JObjectFieldsTableModel<K extends Key, V extends Stored, F extends KeyValueFilter<K, V>> extends AbstractTableModel {

    Table<String, String, Object> table = TreeBasedTable.create();
    public final JObjectFields<K, V, F> objectFields;

    public JObjectFieldsTableModel(JObjectFields<K, V, F> objectFields) {
        this.objectFields = objectFields;
    }

    void setRowAt(K key, V value, int row) {
        table.put(String.valueOf(row), "key", key);
        table.put(String.valueOf(row), "value", value);

        final JObjectFields<K, V, F> copy = objectFields.copy();
        copy.update(key, value);
        Collection<JField> values = copy.objectFields().values();
        int col = 0;
        for (JField f : values) {
            setValueAt(f.getViewValue(), row, col);
            col++;
        }
    }

    @Override
    public String getColumnName(int col) {
        List<JField> values = new ArrayList<>(objectFields.objectFields().values());
        return values.get(col).name();
    }

    @Override
    public int getRowCount() {
        return table.rowKeySet().size();
    }

    @Override
    public int getColumnCount() {
        return objectFields.objectFields().values().size();
    }

    @Override
    public Object getValueAt(int row, int col) {
        return table.get(String.valueOf(row), String.valueOf(col));
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        if (col == 0) {
            return false;
        }
        return true;
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        table.put(String.valueOf(row), String.valueOf(col), value);
        fireTableCellUpdated(row, col);
    }

    void clear() {
        table.clear();
        fireTableStructureChanged();
    }

}
