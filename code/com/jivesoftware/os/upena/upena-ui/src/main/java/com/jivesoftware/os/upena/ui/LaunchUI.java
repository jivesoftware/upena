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

import java.awt.Color;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *
 */
public class LaunchUI {

    public static void main(String[] args) throws IOException {
        Logger.getRootLogger().setLevel(Level.OFF);

        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Upena");
        UIManager.put("Button.background", new Color(250, 230, 190));
        UIManager.put("Panel.background", new Color(250, 250, 250));

        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("GTK+".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(JUpenaServices.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(JUpenaServices.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(JUpenaServices.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(JUpenaServices.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        final JUpena upena = new JUpena();
        ImageIcon icon = Util.icon("cluster");
        if (icon != null) {
            upena.setIconImage(icon.getImage());
        }

        /* Create and display the form */
        Util.invokeLater(new Runnable() {
            @Override
            public void run() {
                upena.setVisible(true);
            }
        });
    }
}