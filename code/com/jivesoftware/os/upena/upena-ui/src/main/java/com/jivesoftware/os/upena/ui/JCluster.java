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
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
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
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

public class JCluster extends JPanel implements DocumentListener {

    RequestHelperProvider requestHelperProvider;
    JObjectFactory factory;

    JTable jTable;
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

    class StatusTable extends AbstractTableModel {

        Table<String, String, Object> table = TreeBasedTable.create();
        String[] columnNames = new String[]{
            "status", //0
            "load",//1
            "host",//2
            "cluster",//3
            "service",//4
            "version",//5
            "instance",//6,
            "uptime",//7
            "errors",//8
            "latency",//9
            "heap%",//10
            "gc%",//11
            "main",//12
            "manage",//13
            "jmx",//14
            "debug"//15
        };

        public void set(StatusReport statuReport, int row) {
            setValueAt(statuReport.load, row, 1);
            setValueAt(elapse(statuReport.timestampInSeconds - statuReport.startupTimestampInSeconds), row, 7);
            setValueAt(statuReport.interactionErrors + statuReport.internalErrors, row, 8);
            setValueAt("?", row, 9);
            setValueAt("?", row, 10);
            setValueAt((int) ((statuReport.percentageOfCPUTimeInGC) * 1000) / 10f, row, 11);
        }

        public void set(HostAndNannyReport hostAndNannyReport, int row) {
            setValueAt(hostAndNannyReport, row, 0);
            setValueAt("?", row, 1);
            setValueAt(hostAndNannyReport.host.hostName, row, 2);
            setValueAt(hostAndNannyReport.nannyReport.instanceDescriptor.clusterName, row, 3);
            setValueAt(hostAndNannyReport.nannyReport.instanceDescriptor.serviceName, row, 4);
            setValueAt(hostAndNannyReport.nannyReport.instanceDescriptor.versionName, row, 5);
            setValueAt(hostAndNannyReport.nannyReport.instanceDescriptor.instanceName, row, 6);
            setValueAt("?", row, 7);
            setValueAt("?", row, 8);
            setValueAt("?", row, 9);
            setValueAt("?", row, 10);
            setValueAt("?", row, 11);
            setValueAt(hostAndNannyReport.nannyReport.instanceDescriptor.ports.get("main").port, row, 12);
            setValueAt(hostAndNannyReport.nannyReport.instanceDescriptor.ports.get("manage").port, row, 13);
            setValueAt(hostAndNannyReport.nannyReport.instanceDescriptor.ports.get("jmx").port, row, 14);
            setValueAt(hostAndNannyReport.nannyReport.instanceDescriptor.ports.get("debug").port, row, 15);
        }

        public String elapse(long elapseInSeconds) {
            String format = String.format("%%0%dd", 2);
            String seconds = String.format(format, elapseInSeconds % 60);
            String minutes = String.format(format, (elapseInSeconds % 3600) / 60);
            String hours = String.format(format, elapseInSeconds / 3600);
            String time = hours + ":" + minutes + ":" + seconds;
            return time;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return HostAndNannyReport.class;
            }
            return String.class;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public int getRowCount() {
            return table.rowKeySet().size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
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

    StatusTable statusTable = new StatusTable();

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
                        int rowCount = statusTable.getRowCount();
                        for (int row = 0; row < rowCount; row++) {

                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) statusTable.getValueAt(row, 0);
                            if (hostAndNannyReport != null) {
                                String url = "http://" + hostAndNannyReport.host.hostName
                                        + ":" + hostAndNannyReport.nannyReport.instanceDescriptor.ports.get("manage").port + "/manage/ping";
                                try {
                                    String curl = Curl.create().curl(url);
                                    if ("ping".equals(curl)) {
                                        if (!hostAndNannyReport.online) {
                                            hostAndNannyReport.checked = true;
                                            hostAndNannyReport.online = true;
                                            statusTable.fireTableCellUpdated(row, 0);

                                        }
                                        try {
                                            String statuUrl = "http://" + hostAndNannyReport.host.hostName
                                                    + ":" + hostAndNannyReport.nannyReport.instanceDescriptor.ports.get("manage").port + "/manage/announcement/json";

                                            String statusJson = Curl.create().curl(statuUrl);
                                            if (statusJson != null) {
                                                StatusReport readValue = new ObjectMapper().readValue(statusJson, StatusReport.class);
                                                statusTable.set(readValue, row);
                                            }
                                        } catch (Exception x) {
                                            x.printStackTrace();
                                        }
                                    } else {
                                        if (!hostAndNannyReport.checked) {
                                            hostAndNannyReport.checked = true;
                                            statusTable.fireTableCellUpdated(row, 0);
                                        }
                                    }
                                } catch (Exception x) {
                                    if (hostAndNannyReport.online) {
                                        hostAndNannyReport.checked = true;
                                        hostAndNannyReport.online = false;
                                        statusTable.fireTableCellUpdated(row, 0);
                                    }
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

        final JPopupMenu buttons = new JPopupMenu();

        JMenuItem button = new JMenuItem("Manage");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
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

        button = new JMenuItem("Properties");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
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

        button = new JMenuItem("Upena Report");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
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

        button = new JMenuItem("Errors");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
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

        button = new JMenuItem("Thread Dump");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
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

        button = new JMenuItem("Counters");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
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

        button = new JMenuItem("Timers");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
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

        button = new JMenuItem("Status");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
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

        button = new JMenuItem("Ping");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
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

        button = new JMenuItem("Tail");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
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

        button = new JMenuItem("Force GC");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
                            tail.append("" + hostAndNannyReport.nannyReport.instanceDescriptor.toString() + " :{\n");
                            try {
                                status(hostAndNannyReport, "/manage/forceGC");
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

        button = new JMenuItem("Reset Errors");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
                            tail.append("" + hostAndNannyReport.nannyReport.instanceDescriptor.toString() + " :{\n");
                            try {
                                status(hostAndNannyReport, "/manage/resetErrors");
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

        button = new JMenuItem("Reset Interaction Errors");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
                            tail.append("" + hostAndNannyReport.nannyReport.instanceDescriptor.toString() + " :{\n");
                            try {
                                status(hostAndNannyReport, "/manage/resetInteractionErrors");
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

        button = new JMenuItem("Routes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
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

        button = new JMenuItem("Purge Routes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
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

        button = new JMenuItem("Shutdown");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tail.setText("");
                        for (int row : jTable.getSelectedRows()) {
                            HostAndNannyReport hostAndNannyReport = (HostAndNannyReport) jTable.getModel().getValueAt(row, 0);
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

        button = new JMenuItem("Refresh");
        button.addActionListener(new ActionListener() {
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

        buttons.add(button);

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

        jTable = new JTable(statusTable);
        jTable.getTableHeader().setReorderingAllowed(true);
        jTable.setFillsViewportHeight(true);
        jTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {

                        buttons.show(jTable, 0, 0);
                    }
                });

        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jTable.getColumnModel().getColumn(0).setPreferredWidth(27);
        jTable.getColumnModel().getColumn(0).setMaxWidth(27);
        jTable.getColumnModel().getColumn(0).setMinWidth(27);

        StatuCell statusCell = new StatuCell();
        jTable.setDefaultEditor(HostAndNannyReport.class, statusCell);
        jTable.setDefaultRenderer(HostAndNannyReport.class, statusCell);

        //jTable.setAutoCreateRowSorter(true);
        JScrollPane scrollJList = new JScrollPane(jTable);
        scrollJList.setPreferredSize(new Dimension(800, 400));

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        //panel.add(filterInstances, BorderLayout.NORTH);
        panel.add(scrollJList, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                panel, searchable);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(400);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);

        //setPreferredSize(new Dimension(900, 600));
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

    @SuppressWarnings("unchecked")
    public void refresh() {

        HostFilter hostFilter = new HostFilter(null, null, null, null, null, 0, 1000);
        JExecutor<HostKey, Host, HostFilter> vExecutor = new JExecutor<>(requestHelperProvider, "host");
        List<Host> found = vExecutor.find(hostFilter, Results.class);
        statusTable.clear();

        int row = 0;
        for (Host host : found) {
            System.out.println("host:" + host);
            //viewResults.add(new JLabel("Host:" + host.hostName));
            try {
                String reportString = Curl.create().curl("http://" + host.hostName + ":" + host.port + "/uba/report");
                if (reportString != null) {

                    UbaReport ubaReport = new ObjectMapper().readValue(reportString, UbaReport.class);
                    for (NannyReport report : ubaReport.nannyReports) {

                        final InstanceDescriptor id = report.instanceDescriptor;
//                        System.out.println("id=" + id);
//                        if (filterInstances.clusterId.getValue() != null && !id.clusterKey.equals(filterInstances.clusterId.getValue())) {
//                            System.out.println("????" + filterInstances.clusterId.getValue());
//                            break;
//                        }
//
//                        if (filterInstances.serviceId.getValue() != null && !id.serviceKey.equals(filterInstances.serviceId.getValue())) {
//                            System.out.println("????" + filterInstances.serviceId.getValue());
//                            break;
//                        }
                        //listModel.addElement(new HostAndNannyReport(host, report));
                        HostAndNannyReport hostAndNannyReport = new HostAndNannyReport(host, report);
                        statusTable.set(hostAndNannyReport, row);
                        row++;

                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        statusTable.fireTableStructureChanged();
        jTable.doLayout();
        jTable.getParent().revalidate();
        jTable.getParent().repaint();
    }

    public class StatuCell extends AbstractCellEditor implements TableCellEditor, ActionListener, TableCellRenderer {

        JButton button;
        Object value;
        protected static final String EDIT = "edit";

        public StatuCell() {
            setOpaque(true); //MUST do this for background to show up.
            button = new JButton();
            button.setActionCommand(EDIT);
            button.addActionListener(this);
            button.setBorderPainted(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (EDIT.equals(e.getActionCommand())) {
                fireEditingStopped(); //Make the renderer reappear.
            }
        }

        @Override
        public Object getCellEditorValue() {
            return value;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.value = value;
            HostAndNannyReport report = (HostAndNannyReport) value;
            int size = 18;
            if (report.online) {
                button.setIcon(Util.resize(Util.icon("online"), size, size));
            } else if (report.checked) {
                button.setIcon(Util.resize(Util.icon("offline"), size, size));
            } else {
                button.setIcon(Util.resize(Util.icon("unknown"), size, size));
            }
            return button;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            HostAndNannyReport report = (HostAndNannyReport) value;
            int size = 18;
            if (report.online) {
                button.setIcon(Util.resize(Util.icon("online"), size, size));
            } else if (report.checked) {
                button.setIcon(Util.resize(Util.icon("offline"), size, size));
            } else {
                button.setIcon(Util.resize(Util.icon("unknown"), size, size));
            }
            return button;
        }
    }

    class HostAndNannyReport {

        boolean online = false;
        boolean checked = false;
        final Host host;
        final NannyReport nannyReport;

        public HostAndNannyReport(Host host, NannyReport nannyReport) {
            this.host = host;
            this.nannyReport = nannyReport;
        }

        @Override
        public String toString() {
            final InstanceDescriptor id = nannyReport.instanceDescriptor;
            String string = "";
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

    static public class StatusReport {

        public String jvmUID;
        public String jvmHome;
        public String jvmName;
        public String jvmVender;
        public String jvmVersion;
        public List<String> jvmHostnames;
        public List<String> jvmIpAddrs;
        public int timestampInSeconds;
        public int startupTimestampInSeconds;
        public float load;
        public double memoryLoad;
        public float percentageOfCPUTimeInGC;

        public long internalErrors = 0;
        public long interactionErrors = 0;

        public StatusReport() {
        }

    }

}
