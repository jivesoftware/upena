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

    public JUpena() {
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Upena - (hawaiian 'net' pron. OohPenAh)");

        JPanel v = new JPanel(new BorderLayout(1, 1));

        RequestHelperProvider helperProvider = new RequestHelperProvider();
        v.add(helperProvider, BorderLayout.NORTH);

        JUpenaServices upena = new JUpenaServices(helperProvider, new JObjectFactory(helperProvider));
        v.add(upena, BorderLayout.CENTER);

        add(v);
        //setPreferredSize(new Dimension(800, 300));
        //setMaximumSize(new Dimension(400, 300));
        pack();
    }

}