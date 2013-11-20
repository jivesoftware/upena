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
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

public class JCollapsablePanel extends JPanel {

    private boolean selected;
    JPanel contentPanel_;
    HeaderPanel headerPanel_;

    private class HeaderPanel extends JPanel implements MouseListener {

        String text_;
        Font font;
        BufferedImage open, closed;
        final int OFFSET = 30;
        final int PAD = 5;

        public HeaderPanel(JPanel text) {
            addMouseListener(this);
            setLayout(new BorderLayout(1, 1));
            add(text, BorderLayout.CENTER);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            toggleSelection();
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }

    }

    public JCollapsablePanel(JPanel header, JPanel panel) {
        super(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(1, 3, 0, 3);
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        selected = false;
        headerPanel_ = new HeaderPanel(header);

        setBackground(new Color(200, 200, 220));
        contentPanel_ = panel;

        add(headerPanel_, gbc);
        add(contentPanel_, gbc);
        contentPanel_.setVisible(false);

    }

    public void toggleSelection() {
        selected = !selected;

        if (contentPanel_.isShowing()) {
            contentPanel_.setVisible(false);
        } else {
            contentPanel_.setVisible(true);
        }

        validate();

        headerPanel_.repaint();
    }

}