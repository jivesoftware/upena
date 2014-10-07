/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.upena.ui;

import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;
import javax.swing.JOptionPane;

/**
 *
 * @author jonathan.colt
 */
public class MacControllers extends ApplicationAdapter {

    public void handleQuit(ApplicationEvent e) {
        System.exit(0);
    }

    public void handleAbout(ApplicationEvent e) {
    // tell the system we're handling this, so it won't display
        // the default system "about" dialog after ours is shown.
        e.setHandled(true);
        JOptionPane.showMessageDialog(null, "Show About dialog here");
    }

    public void handlePreferences(ApplicationEvent e) {
        JOptionPane.showMessageDialog(null, "Show Preferences dialog here");
    }
}
