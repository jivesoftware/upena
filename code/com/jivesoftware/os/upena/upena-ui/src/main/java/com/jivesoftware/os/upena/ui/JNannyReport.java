/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */

package com.jivesoftware.os.upena.ui;

import com.jivesoftware.os.jive.utils.shell.utils.Curl;
import com.jivesoftware.os.uba.shared.NannyReport;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
import com.jivesoftware.os.upena.shared.Host;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 *
 * @author jonathan.colt
 */
class JNannyReport extends JPanel {
    private final NannyReport nannyReport;
    private final JTextArea tail;
    private final Host host;
    private final String userName;

    public JNannyReport(final Host host, final NannyReport nannyReport, final String userName) {
        this.host = host;
        this.nannyReport = nannyReport;
        this.userName = userName;
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
        for (Map.Entry<String, InstanceDescriptor.InstanceDescriptorPort> p : id.ports.entrySet()) {
            name.add(new JLabel(p.getKey() + "=" + p.getValue().port));
            name.add(Box.createHorizontalStrut(10));
        }
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        JButton button = new JButton("Manage");
        button.addActionListener(new JCluster.Action(new Runnable() {
            @Override
            public void run() {
                try {
                    openWebpage(new URI("http://" + host.hostName + ":" + id.ports.get("manage").port + "/manage/help"));
                } catch (URISyntaxException ex) {
                    ex.printStackTrace();
                }
            }
        }));
        buttons.add(button);
        button = new JButton("Properties");
        button.addActionListener(new JCluster.Action(new Runnable() {
            @Override
            public void run() {
                try {
                    status("/manage/configuration/properties");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }));
        buttons.add(button);
        button = new JButton("Status");
        button.addActionListener(new JCluster.Action(new Runnable() {
            @Override
            public void run() {
                try {
                    status("/manage/status");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }));
        buttons.add(button);
        button = new JButton("Ping");
        button.addActionListener(new JCluster.Action(new Runnable() {
            @Override
            public void run() {
                try {
                    status("/manage/ping");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }));
        buttons.add(button);
        button = new JButton("Tail");
        button.addActionListener(new JCluster.Action(new Runnable() {
            @Override
            public void run() {
                try {
                    status("/manage/tail");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }));
        buttons.add(button);
        button = new JButton("Counters");
        button.addActionListener(new JCluster.Action(new Runnable() {
            @Override
            public void run() {
                try {
                    status("/manage/logging/metric/listCounters?logger=ALL");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }));
        buttons.add(button);
        button = new JButton("Timers");
        button.addActionListener(new JCluster.Action(new Runnable() {
            @Override
            public void run() {
                try {
                    status("/manage/logging/metric/listTimers?logger=ALL");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }));
        buttons.add(button);
        button = new JButton("Routes");
        button.addActionListener(new JCluster.Action(new Runnable() {
            @Override
            public void run() {
                try {
                    status("/manage/tenant/routing/report");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }));
        buttons.add(button);
        button = new JButton("Purge Routes");
        button.addActionListener(new JCluster.Action(new Runnable() {
            @Override
            public void run() {
                try {
                    status("/manage/tenant/routing/invaliateAll");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }));
        buttons.add(button);
        button = new JButton("Shutdown");
        button.addActionListener(new JCluster.Action(new Runnable() {
            @Override
            public void run() {
                try {
                    status("/manage/shutdown?userName=" + userName);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }));
        buttons.add(button);
        tail = new JTextArea();
        JScrollPane scrollTail = new JScrollPane(tail, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
            x.printStackTrace();
        }
        tail.revalidate();
        revalidate();
        Container parent = getParent();
        if (parent != null) {
            revalidate();
        }
    }

    public void openWebpage(URI uri) {
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
