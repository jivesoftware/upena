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
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class JExecutor<K extends Key, V extends Stored, F extends KeyValueFilter<K, V>> {

    private final Executor thread = Executors.newCachedThreadPool();
    private final RequestHelperProvider requestHelperProvider;
    private final String context;

    public JExecutor(RequestHelperProvider requestHelperProvider, String context) {
        this.requestHelperProvider = requestHelperProvider;
        this.context = context;
    }

    public void get(final Class<V> valueClass, final K key, final IPicked<K, V> picked) {
        if (picked != null) {
//            thread.execute(new Runnable() {
//                @Override
//                public void run() {
            V result = requestHelperProvider.get().executeRequest(key, "/upena/" + context + "/get", valueClass, null);
            if (result != null) {
                picked.picked(key, result);
            } else {
                picked.picked(null, null);
            }

//                }
//            });
        }
    }

    public void remove(final JObjectFields<K, V, F> objectFields, final JPanel viewResults) {
        thread.execute(new Runnable() {
            @Override
            public void run() {

                Boolean key = requestHelperProvider.get().executeRequest(objectFields.key(), "/upena/" + context + "/remove", Boolean.class, null);
                if (key != null) {
                    viewResults.removeAll();
                    viewResults.add(new JLabel("Removed:" + key));
                    viewResults.revalidate();
                } else {
                    viewResults.removeAll();
                    viewResults.add(new JLabel("Failed to remove:" + key));
                    viewResults.revalidate();
                }
                viewResults.getParent().revalidate();
                viewResults.getParent().repaint();
            }
        });
    }

    public void create(final JObjectFields<K, V, F> objectFields, final IPicked<K, V> picked) {
        thread.execute(new Runnable() {
            @Override
            public void run() {
                V v = objectFields.fieldsToObject();
                K key = requestHelperProvider.get().executeRequest(v, "/upena/" + context + "/add", objectFields.keyClass(), null);
                if (key != null) {
                    V result = requestHelperProvider.get().executeRequest(key, "/upena/" + context + "/get", objectFields.valueClass(), null);
                    if (result != null) {
                        picked.picked(key, v);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, v.toString(), "Failed to Create", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

    }

    public void update(final JObjectFields<K, V, F> objectFields, final IPicked<K, V> picked) {
        thread.execute(new Runnable() {
            @Override
            public void run() {
                V v = objectFields.fieldsToObject();
                String objectKey = objectFields.key().getKey();
                K key = requestHelperProvider.get().executeRequest(v, "/upena/" + context + "/update?key=" + objectKey, objectFields.keyClass(), null);
                if (key != null) {
                    V result = requestHelperProvider.get().executeRequest(key, "/upena/" + context + "/get", objectFields.valueClass(), null);
                    if (result != null) {
                        picked.picked(key, v);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, v.toString(), "Failed to Create", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

    }

    public List<V> find(final F filter, final Class<? extends ConcurrentSkipListMap<String, TimestampedValue<V>>> responseClass) {

        List<V> found = new ArrayList<>();
        try {
            ConcurrentSkipListMap<String, TimestampedValue<V>> results = requestHelperProvider.get().executeRequest(filter,
                "/upena/" + context + "/find", responseClass, null);
            if (results != null) {

                int count = 0;
                for (final Map.Entry<String, TimestampedValue<V>> e : results.entrySet()) {
                    if (!e.getValue().getTombstoned()) {
                        Color color = Color.white;
                        if (count % 2 == 0) {
                            color = Color.lightGray;
                        }
                        V value = e.getValue().getValue();
                        found.add(value);
                    }

                }

            }

        } catch (Exception x) {
            x.printStackTrace();
        }
        return found;
    }

    public void find(final F filter, final JObjectFieldsTableModel<K, V, F> objectFieldsTableModel, final boolean hasPopup, final IPicked<K, V> picked) {

        thread.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ConcurrentSkipListMap<String, TimestampedValue<V>> results = requestHelperProvider.get().executeRequest(filter,
                        "/upena/" + context + "/find", objectFieldsTableModel.objectFields.responseClass(), null);
                    if (results != null) {
                        objectFieldsTableModel.clear();

                        int row = 0;
                        for (final Map.Entry<String, TimestampedValue<V>> e : results.entrySet()) {
                            if (!e.getValue().getTombstoned()) {
                                objectFieldsTableModel.setRowAt(objectFieldsTableModel.objectFields.key(e.getKey()), e.getValue().getValue(), row);
                                row++;
                            }
                        }

                    } else {

                    }

                } catch (Exception x) {
                    x.printStackTrace();
                }
            }
        });
    }
}
