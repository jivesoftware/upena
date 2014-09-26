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
import com.jivesoftware.os.upena.shared.HostFilter;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

public class JCluster extends JPanel implements DocumentListener {

    RequestHelperProvider requestHelperProvider;
    JObjectFactory factory;

    DefaultListModel<HostAndNannyReport> listModel = new DefaultListModel<>();
    JList<HostAndNannyReport> jList = new JList<>(listModel);
    JTextArea tail;
    Highlighter hilit;
    Highlighter.HighlightPainter painter;
    private JTextField entry;
    JLabel status;

    public JCluster(RequestHelperProvider requestHelperProvider, JObjectFactory factory) {
        this.requestHelperProvider = requestHelperProvider;
        this.factory = factory;
        initComponents();
    }

    @Override
    public void insertUpdate(DocumentEvent ev) {
        search();
    }

    @Override
    public void removeUpdate(DocumentEvent ev) {
        search();
    }

    @Override
    public void changedUpdate(DocumentEvent ev) {
    }

    public void search() {
        hilit.removeAllHighlights();

        String s = entry.getText();
        if (s.length() <= 0) {
            message("Nothing to search");
            return;
        }

        String content = tail.getText();
        int start = 0;
        int index = content.indexOf(s, start);
        int found = 0;
        while (index > -1) {

            if (index >= 0) {   // match found
                try {
                    int end = index + s.length();
                    hilit.addHighlight(index, end, painter);
                    tail.setCaretPosition(end);
                    found++;
                    start = end;
                    index = content.indexOf(s, start);
                    //entry.setBackground(entryBg);
                    message("'" + s + "' found. Press ESC to end search");
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }
        if (found == 0) {
            //entry.setBackground(ERROR_COLOR);
            message("'" + s + "' not found. Press ESC to start a new search");
        }
    }

    void message(String msg) {
        status.setText(msg);
    }

    final static String CANCEL_ACTION = "cancel-search";

    class CancelAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent ev) {
            hilit.removeAllHighlights();
            entry.setText("");
            //entry.setBackground(entryBg);
        }
    }

    private void initComponents() {
        entry = new JTextField();
        entry.getDocument().addDocumentListener(this);
        InputMap im = entry.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = entry.getActionMap();
        im.put(KeyStroke.getKeyStroke("ESCAPE"), CANCEL_ACTION);
        am.put(CANCEL_ACTION, new CancelAction());

        JPanel menu = new JPanel();
        menu.setLayout(new BorderLayout(10, 10));

        Thread nag = new Thread() {

            @Override
            public void run() {
                while (true) {
                    try {
                        //List<HostAndNannyReport> selectedValuesList = listModel.elements());
                        Enumeration<HostAndNannyReport> elements = listModel.elements();
                        while (elements.hasMoreElements()) {
                            HostAndNannyReport hostAndNannyReport = elements.nextElement();
                            String url = "http://" + hostAndNannyReport.host.hostName
                                + ":" + hostAndNannyReport.nannyReport.instanceDescriptor.ports.get("manage").port + "/manage/ping";
                            try {
                                String curl = Curl.create().curl(url);
                                if ("ping".equals(curl)) {
                                    if (!hostAndNannyReport.online) {
                                        hostAndNannyReport.online = true;
                                        jList.updateUI();
                                    }
                                }
                            } catch (Exception x) {
                                if (hostAndNannyReport.online) {
                                    hostAndNannyReport.online = false;
                                    jList.updateUI();
                                }
                            }
                        }
                        Thread.sleep(1000);

                    } catch (Exception x) {
                        x.printStackTrace();
                    }
                }
            }

        };
        nag.start();

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

        JButton button = new JButton("Manage");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        List<HostAndNannyReport> selectedValuesList = jList.getSelectedValuesList();
                        for (HostAndNannyReport hostAndNannyReport : selectedValuesList) {
                            try {
                                openWebpage(new URI(
                                    "http://" + hostAndNannyReport.host.hostName
                                    + ":" + hostAndNannyReport.nannyReport.instanceDescriptor.ports.get("manage").port + "/manage/help"));
                            } catch (URISyntaxException ex) {
                                ex.printStackTrace();
                            }
                        }

                    }
                });
            }
        });

        buttons.add(button);

        button = new JButton("Properties");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        List<HostAndNannyReport> selectedValuesList = jList.getSelectedValuesList();
                        for (HostAndNannyReport hostAndNannyReport : selectedValuesList) {
                            tail.append("" + hostAndNannyReport.nannyReport.instanceDescriptor.toString() + " :{\n");
                            try {
                                status(hostAndNannyReport, "/manage/configuration/properties");
                                tail.append("\n}\n\n");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });
            }
        });

        buttons.add(button);

        button = new JButton("Upena Report");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        List<HostAndNannyReport> selectedValuesList = jList.getSelectedValuesList();
                        for (HostAndNannyReport hostAndNannyReport : selectedValuesList) {
                            tail.append("" + hostAndNannyReport.nannyReport.instanceDescriptor.toString() + " :{\n");

                            for (String m : hostAndNannyReport.nannyReport.messages) {
                                tail.append(m + "\n");
                            }
                            tail.append("\n}\n\n");
                        }
                    }
                });
            }
        });

        buttons.add(button);

        button = new JButton("Errors");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        List<HostAndNannyReport> selectedValuesList = jList.getSelectedValuesList();
                        for (HostAndNannyReport hostAndNannyReport : selectedValuesList) {
                            tail.append("" + hostAndNannyReport.nannyReport.instanceDescriptor.toString() + " :{\n");
                            try {
                                status(hostAndNannyReport, "/manage/errors ");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            tail.append("\n}\n\n");
                        }
                    }
                });
            }
        });
        buttons.add(button);

        button = new JButton("Thread Dump");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        List<HostAndNannyReport> selectedValuesList = jList.getSelectedValuesList();
                        for (HostAndNannyReport hostAndNannyReport : selectedValuesList) {
                            tail.append("" + hostAndNannyReport.nannyReport.instanceDescriptor.toString() + " :{\n");
                            try {
                                status(hostAndNannyReport, "/manage/threadDump ");

                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            tail.append("\n}\n\n");
                        }
                    }
                });
            }
        });
        buttons.add(button);

        button = new JButton("Counters");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        List<HostAndNannyReport> selectedValuesList = jList.getSelectedValuesList();
                        for (HostAndNannyReport hostAndNannyReport : selectedValuesList) {
                            tail.append("" + hostAndNannyReport.nannyReport.instanceDescriptor.toString() + " :{\n");
                            try {
                                status(hostAndNannyReport, "/manage/logging/metric/listCounters?logger=ALL ");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            tail.append("\n}\n\n");
                        }
                    }
                });
            }
        });
        buttons.add(button);

        button = new JButton("Timers");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        List<HostAndNannyReport> selectedValuesList = jList.getSelectedValuesList();
                        for (HostAndNannyReport hostAndNannyReport : selectedValuesList) {
                            tail.append("" + hostAndNannyReport.nannyReport.instanceDescriptor.toString() + " :{\n");
                            try {
                                status(hostAndNannyReport, "/manage/logging/metric/listTimers?logger=ALL ");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            tail.append("\n}\n\n");
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
                        tail.setText("");
                        List<HostAndNannyReport> selectedValuesList = jList.getSelectedValuesList();
                        for (HostAndNannyReport hostAndNannyReport : selectedValuesList) {
                            tail.append("" + hostAndNannyReport.nannyReport.instanceDescriptor.toString() + " :{\n");
                            try {
                                status(hostAndNannyReport, "/manage/status");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            tail.append("\n}\n\n");
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
                        tail.setText("");
                        List<HostAndNannyReport> selectedValuesList = jList.getSelectedValuesList();
                        for (HostAndNannyReport hostAndNannyReport : selectedValuesList) {
                            tail.append("" + hostAndNannyReport.nannyReport.instanceDescriptor.toString() + " :{\n");
                            try {
                                status(hostAndNannyReport, "/manage/ping");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            tail.append("\n}\n\n");
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
                        tail.setText("");
                        List<HostAndNannyReport> selectedValuesList = jList.getSelectedValuesList();
                        for (HostAndNannyReport hostAndNannyReport : selectedValuesList) {
                            tail.append("" + hostAndNannyReport.nannyReport.instanceDescriptor.toString() + " :{\n");
                            try {
                                status(hostAndNannyReport, "/manage/tail");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            tail.append("\n}\n\n");
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
                        tail.setText("");
                        List<HostAndNannyReport> selectedValuesList = jList.getSelectedValuesList();
                        for (HostAndNannyReport hostAndNannyReport : selectedValuesList) {
                            tail.append("" + hostAndNannyReport.nannyReport.instanceDescriptor.toString() + " :{\n");
                            try {
                                status(hostAndNannyReport, "/manage/tenant/routing/report");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            tail.append("\n}\n\n");
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
                        tail.setText("");
                        List<HostAndNannyReport> selectedValuesList = jList.getSelectedValuesList();
                        for (HostAndNannyReport hostAndNannyReport : selectedValuesList) {
                            tail.append("" + hostAndNannyReport.nannyReport.instanceDescriptor.toString() + " :{\n");
                            try {
                                status(hostAndNannyReport, "/manage/tenant/routing/invaliateAll");

                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            tail.append("\n}\n\n");
                        }
                    }
                });
            }
        });

        buttons.add(button);

        button = new JButton("Shutdown");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        List<HostAndNannyReport> selectedValuesList = jList.getSelectedValuesList();
                        for (HostAndNannyReport hostAndNannyReport : selectedValuesList) {
                            tail.append("" + hostAndNannyReport.nannyReport.instanceDescriptor.toString() + " :{\n");
                            try {
                                status(hostAndNannyReport, "/manage/shutdown?userName=" + requestHelperProvider.editUserName.getText());
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            tail.append("\n}\n\n");
                        }
                    }
                });
            }
        });

        buttons.add(button);

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

        buttons.add(refresh);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.add(buttons, BorderLayout.NORTH);

        panel.add(jList, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);

        tail = new JTextArea();

        hilit = new DefaultHighlighter();
        painter = new DefaultHighlighter.DefaultHighlightPainter(Color.CYAN);
        tail.setHighlighter(hilit);

        JScrollPane scrollTail = new JScrollPane(tail,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollTail.setPreferredSize(new Dimension(-1, 400));

        status = new JLabel();

        JPanel find = new JPanel();
        find.setLayout(new BorderLayout(3, 3));
        find.add(new JLabel("Find:"), BorderLayout.WEST);
        find.add(entry, BorderLayout.CENTER);

        JPanel searchable = new JPanel();
        searchable.setLayout(new BorderLayout(10, 10));
        searchable.add(find, BorderLayout.NORTH);
        searchable.add(scrollTail, BorderLayout.CENTER);
        searchable.add(status, BorderLayout.SOUTH);

        add(searchable, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(900, 600));

        Util.invokeLater(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        });
    }

    private void status(HostAndNannyReport hostAndNannyReport, String manageEndpoint) throws IOException {
        String url = "http://" + hostAndNannyReport.host.hostName
            + ":" + hostAndNannyReport.nannyReport.instanceDescriptor.ports.get("manage").port + manageEndpoint;
        try {
            String curl = Curl.create().curl(url);
            tail.append(curl);
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

    public static class Results extends ConcurrentSkipListMap<String, TimestampedValue<Host>> {
    }

    @SuppressWarnings ("unchecked")
    public void refresh() {

        HostFilter hostFilter = new HostFilter(null, null, null, null, null, 0, 1000);
        JExecutor<HostKey, Host, HostFilter> vExecutor = new JExecutor<>(requestHelperProvider, "host");
        List<Host> found = vExecutor.find(hostFilter, Results.class);
        listModel.clear();

        for (Host host : found) {

            //viewResults.add(new JLabel("Host:" + host.hostName));
            try {
                String reportString = Curl.create().curl("http://" + host.hostName + ":" + host.port + "/uba/report");
                if (reportString != null) {
                    UbaReport ubaReport = new ObjectMapper().readValue(reportString, UbaReport.class);
                    for (NannyReport report : ubaReport.nannyReports) {

                        // We need to keep track of the selections
                        // and make the selection state available to the renderer.
                        final InstanceDescriptor id = report.instanceDescriptor;
                        listModel.addElement(new HostAndNannyReport(host, report));

//                        JNannyReport jid = new JNannyReport(host, report);
//                        viewResults.add(jid);
//                        visibleInstanceDescriptors.add(jid);
                    }
                } else {
                    //viewResults.add(new JLabel("No results"));
                    //viewResults.revalidate();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        jList.getParent().revalidate();
        jList.getParent().repaint();
    }

    class HostAndNannyReport {

        boolean online = false;
        final Host host;
        final NannyReport nannyReport;

        public HostAndNannyReport(Host host, NannyReport nannyReport) {
            this.host = host;
            this.nannyReport = nannyReport;
        }

        @Override
        public String toString() {
            final InstanceDescriptor id = nannyReport.instanceDescriptor;
            String string = online ? " ONLINE " : "UNKNOWN ";
            string += host.hostName + " ";
            string += id.clusterName + " " + id.serviceName + " " + id.instanceName + " " + id.releaseGroupName;;
            for (Entry<String, InstanceDescriptorPort> p : id.ports.entrySet()) {
                string += " " + p.getKey() + "=" + p.getValue().port;
            }
            return string;
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

    class SelectionManager implements ListSelectionListener {

        List<Object> selectedItems = new ArrayList<Object>();
        List<Object> nonSelectables = new ArrayList<Object>();

        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                Object value = ((JList) e.getSource()).getSelectedValue();
                // Toggle the selection state for value.
                if (selectedItems.contains(value)) {
                    selectedItems.remove(value);
                } else if (!nonSelectables.contains(value)) {
                    selectedItems.add(value);
                }
            }
        }

        public void setNonSelectables(Object... args) {
            for (int j = 0; j < args.length; j++) {
                nonSelectables.add(args[j]);
            }
        }

        public boolean isSelected(Object value) {
            return selectedItems.contains(value);
        }
    }

    /** Implementation copied from source code. */
    class MultiRenderer extends DefaultListCellRenderer {

        SelectionManager selectionManager;

        public MultiRenderer(SelectionManager sm) {
            selectionManager = sm;
        }

        public Component getListCellRendererComponent(JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
            setComponentOrientation(list.getComponentOrientation());
            if (selectionManager.isSelected(value)) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            if (value instanceof Icon) {
                setIcon((Icon) value);
                setText("");
            } else {
                setIcon(null);
                setText((value == null) ? "" : value.toString());
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            setBorder((cellHasFocus) ? UIManager.getBorder("List.focusCellHighlightBorder")
                : noFocusBorder);
            return this;
        }
    }
}
