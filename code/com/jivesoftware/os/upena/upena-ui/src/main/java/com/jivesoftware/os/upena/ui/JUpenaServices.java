package com.jivesoftware.os.upena.ui;

import com.jivesoftware.os.jive.utils.http.client.rest.RequestHelper;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.Key;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.Tenant;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class JUpenaServices extends javax.swing.JFrame {

    RequestHelper requestHelper;
    JObjectFactory factory;

    public JUpenaServices(RequestHelper requestHelper, JObjectFactory factory) {
        this.requestHelper = requestHelper;
        this.factory = factory;
        initComponents();
    }

    private void initComponents() {

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.SCROLL_TAB_LAYOUT);
        final JObject<? extends Key, ?, ?> instances = factory.create(Instance.class, true, null);
        tabbedPane.add("Instances", instances);
        tabbedPane.add("Clusters", factory.create(Cluster.class, true, null));
        tabbedPane.add("Releases", factory.create(ReleaseGroup.class, true, null));
        tabbedPane.add("Hosts", factory.create(Host.class, true, null));
        tabbedPane.add("Services", factory.create(Service.class, true, null));
        tabbedPane.add("Tenants", factory.create(Tenant.class, true, null));

        JRoutes routes = new JRoutes(requestHelper, factory);
        tabbedPane.add("Routes", routes);

        JConfig config = new JConfig(requestHelper, factory);
        tabbedPane.add("Config", config);

        JAmza amza = new JAmza(requestHelper);
        tabbedPane.add("Admin", amza);
        tabbedPane.setPreferredSize(new Dimension(800, 250));

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JTabbedPane jTabbedPane = (JTabbedPane) e.getSource();
                        if (jTabbedPane.getSelectedComponent() instanceof JObject) {
                            JObject jObject = (JObject) jTabbedPane.getSelectedComponent();
                            jObject.clear();
                            jObject.find(null);
                        }
                    }
                });

            }
        });

        JPanel m = new JPanel();
        m.setLayout(new BorderLayout(8, 8));

        m.add(tabbedPane, BorderLayout.CENTER);

        add(m);
        setSize(1024, 768);
        setPreferredSize(new Dimension(1024, 768));
        pack();

        Util.invokeLater(new Runnable() {
            @Override
            public void run() {
                instances.clear();
                instances.find(null);
            }
        });
    }
}