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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.jive.utils.shell.utils.Curl;
import com.jivesoftware.os.uba.shared.NannyReport;
import com.jivesoftware.os.uba.shared.UbaReport;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor.InstanceDescriptorPort;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.Stored;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class JCluster extends JPanel {

    List<JNannyReport> visibleInstanceDescriptors = new ArrayList<>();
    RequestHelperProvider requestHelperProvider;
    JObjectFactory factory;
    JPanel viewResults;
    JEditRef hostId;
    Host host;

    public JCluster(RequestHelperProvider requestHelperProvider, JObjectFactory factory) {
        this.requestHelperProvider = requestHelperProvider;
        this.factory = factory;
        initComponents();
    }

    private void initComponents() {

        viewResults = new JPanel();
        viewResults.setLayout(new BoxLayout(viewResults, BoxLayout.Y_AXIS));
        viewResults.setPreferredSize(new Dimension(800, 400));

        hostId = new JEditRef(factory, "host", Host.class, "");
        JComponent editor = hostId.getEditor(40, new IPicked() {
            @Override
            public void picked(Object key, Stored v) {
                host = (Host) v;
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }
        });
        hostId.setValue("");

        JPanel menu = new JPanel();
        menu.setLayout(new BorderLayout(10, 10));

        JButton refresh = new JButton(Util.icon("refresh"));
        refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }
        });
        menu.add(editor, BorderLayout.CENTER);
        menu.add(refresh, BorderLayout.EAST);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.add(menu, BorderLayout.NORTH);

        JScrollPane scrollRoutes = new JScrollPane(viewResults,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scrollRoutes, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        setPreferredSize(new Dimension(900, 600));

    }

    public void refresh() {

        viewResults.removeAll();
        visibleInstanceDescriptors.clear();

        try {
            String reportString = Curl.create().curl("http://" + host.hostName + ":" + host.port + "/uba/report");
            if (reportString != null) {
                UbaReport ubaReport = new ObjectMapper().readValue(reportString, UbaReport.class);
                for (NannyReport report : ubaReport.nannyReports) {
                    JNannyReport jid = new JNannyReport(host, report, requestHelperProvider.editUserName.getText());
                    viewResults.add(jid);
                    visibleInstanceDescriptors.add(jid);
                }
            } else {
                viewResults.add(new JLabel("No results"));
                viewResults.revalidate();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        viewResults.getParent().revalidate();
        viewResults.getParent().repaint();
    }

    static class Action implements ActionListener {

        private final Runnable runnable;

        public Action(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Util.invokeLater(runnable);
        }
    }
}
