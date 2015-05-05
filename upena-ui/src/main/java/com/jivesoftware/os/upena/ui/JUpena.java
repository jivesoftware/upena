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

import java.awt.BorderLayout;
import javax.swing.JPanel;

public class JUpena extends javax.swing.JFrame {

    private final String defaultHost;

    public JUpena(String defaultHost) {
        this.defaultHost = defaultHost;
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Upena - (hawaiian 'net' pron. OohPenAh)");

        JPanel helpers = new JPanel(new BorderLayout(1, 1));
        RequestHelperProvider helperProviderA = new RequestHelperProvider("A", defaultHost);
        helpers.add(helperProviderA, BorderLayout.NORTH);
        RequestHelperProvider helperProviderB = new RequestHelperProvider("B", defaultHost);
        helpers.add(helperProviderB, BorderLayout.SOUTH);

        JPanel v = new JPanel(new BorderLayout(1, 1));
        v.add(helpers, BorderLayout.NORTH);

        JUpenaServices upena = new JUpenaServices(helperProviderA, helperProviderB, new JObjectFactory(helperProviderA));
        v.add(upena, BorderLayout.CENTER);

        add(v);
        pack();
    }

}
