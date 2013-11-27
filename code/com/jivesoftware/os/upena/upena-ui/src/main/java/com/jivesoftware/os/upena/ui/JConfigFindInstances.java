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

import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.Stored;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

public class JConfigFindInstances extends JPanel {
    Color color;
    JEditRef clusterId;
    JEditRef hostId;
    JEditRef serviceId;
    JEditRef releaseGroupId;
    JTextField instanceId;

    public JConfigFindInstances(JObjectFactory factory, Color color, final Runnable changed) {
        this.color = color;
        setLayout(new SpringLayout());
        clusterId = new JEditRef(factory, "cluster", Cluster.class, "");
        add(clusterId.getEditor(40, new IPicked() {
            @Override
            public void picked(Object key, Stored v) {
                Util.invokeLater(changed);
            }
        }));
        clusterId.setValue("");
        hostId = new JEditRef(factory, "host", Host.class, "");
        add(hostId.getEditor(40, new IPicked() {
            @Override
            public void picked(Object key, Stored v) {
                Util.invokeLater(changed);
            }
        }));
        hostId.setValue("");
        serviceId = new JEditRef(factory, "service", Service.class, "");
        add(serviceId.getEditor(40, new IPicked() {
            @Override
            public void picked(Object key, Stored v) {
                Util.invokeLater(changed);
            }
        }));
        serviceId.setValue("");
        releaseGroupId = new JEditRef(factory, "release-group", ReleaseGroup.class, "");
        add(releaseGroupId.getEditor(40, new IPicked() {
            @Override
            public void picked(Object key, Stored v) {
                Util.invokeLater(changed);
            }
        }));
        releaseGroupId.setValue("");
        instanceId = new JTextField("", 120);
        instanceId.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(changed);
            }
        });
        add(instanceId);
        JButton clear = new JButton(Util.icon("clear-left"));
        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        clusterId.setValue("");
                        hostId.setValue("");
                        serviceId.setValue("");
                        releaseGroupId.setValue("");
                        instanceId.setText("");
                        changed.run();
                    }
                });
            }
        });
        add(clear);
        SpringUtils.makeCompactGrid(this, 1, 6, 10, 10, 10, 10);
        setBackground(color);
    }

}
