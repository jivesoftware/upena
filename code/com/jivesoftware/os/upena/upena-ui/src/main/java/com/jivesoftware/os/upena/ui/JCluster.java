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
import com.jivesoftware.os.jive.utils.http.client.rest.RequestHelper;
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

public class JCluster extends javax.swing.JFrame {

    List<JNannyReport> visibleInstanceDescriptors = new ArrayList<>();
    RequestHelper requestHelper;
    JObjectFactory factory;
    JPanel viewResults;
    JEditRef hostId;
    Host host;

    public JCluster(RequestHelper requestHelper, JObjectFactory factory) {
        this.requestHelper = requestHelper;
        this.factory = factory;
        initComponents();
    }

    private void initComponents() {
        setTitle("Cluster");

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

        add(panel);
        setPreferredSize(new Dimension(900, 600));
        pack();

    }

    public void refresh() {

        viewResults.removeAll();
        visibleInstanceDescriptors.clear();
        try {
            String reportString = Curl.create().curl("http://" + host.hostName + ":" + host.port + "/uba/report");
            if (reportString != null) {
                UbaReport ubaReport = new ObjectMapper().readValue(reportString, UbaReport.class);
                for (NannyReport report : ubaReport.nannyReports) {

                    JNannyReport jid = new JNannyReport(report);
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

    class JNannyReport extends JPanel {

        private final NannyReport nannyReport;
        private final JTextArea tail;

        public JNannyReport(final NannyReport nannyReport) {
            this.nannyReport = nannyReport;
            final InstanceDescriptor id = nannyReport.instanceDescriptor;

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            JPanel name = new JPanel();
            name.setLayout(new BoxLayout(name, BoxLayout.X_AXIS));

            name.add(new JLabel(id.clusterName));
            name.add(Box.createHorizontalStrut(10));
            name.add(new JLabel(id.serviceName));
            name.add(Box.createHorizontalStrut(10));
            name.add(new JLabel("" + id.instanceName));
            name.add(Box.createHorizontalStrut(10));
            name.add(new JLabel(id.releaseGroupName));
            name.add(Box.createHorizontalStrut(10));
            for (Entry<String, InstanceDescriptorPort> p : id.ports.entrySet()) {
                name.add(new JLabel(p.getKey() + "=" + p.getValue().port));
                name.add(Box.createHorizontalStrut(10));
            }

            JPanel buttons = new JPanel();
            buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
            JButton button = new JButton("Manage");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Util.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                openWebpage(new URI("http://" + host.hostName + ":" + id.ports.get("manage").port + "/manage/help"));
                            } catch (URISyntaxException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            });

            buttons.add(button);

            button = new JButton("Status");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Util.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                status("/manage/status");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            });

            buttons.add(button);

            button = new JButton("Ping");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Util.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                status("/manage/ping");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            });

            buttons.add(button);

            button = new JButton("Tail");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Util.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                status("/manage/tail");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            });

            buttons.add(button);

            button = new JButton("Routes");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Util.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                status("/manage/tenant/routing/report");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            });

            buttons.add(button);

            button = new JButton("Purge Routes");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Util.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                status("/manage/tenant/routing/invaliateAll");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            });

            buttons.add(button);

            tail = new JTextArea();
            JScrollPane scrollTail = new JScrollPane(tail,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollTail.setPreferredSize(new Dimension(-1, 400));

            for (String m : nannyReport.messages) {
                tail.append(m + "\n");
            }

            add(name);
            add(buttons);
            add(scrollTail);
        }

        private void status(String manageEndpoint) throws IOException {
            String url = "http://" + host.hostName + ":" + nannyReport.instanceDescriptor.ports.get("manage").port + manageEndpoint;
            try {
                String curl = Curl.create().curl(url);
                tail.setText(curl);
            } catch (Exception x) {
                tail.setText("failed to call " + url + " " + new Date());
            }
            tail.revalidate();
            revalidate();
            Container parent = getParent();
            if (parent != null) {
                revalidate();
            }
        }
    }

    public static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
