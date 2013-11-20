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
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.swing.ImageIcon;

public class Util {

    public static Color getHashSolid(Object _instance) {
        int h = new Random(_instance.hashCode()).nextInt();

        int b = (h % 32) + 220;
        h >>= 2;
        int g = (h % 32) + 220;
        h >>= 4;
        int r = (h % 32) + 220;
        h >>= 8;
        return new Color(r, g, b);
    }

    public static ImageIcon icon(String name) {
        java.net.URL imgURL = Util.class.getResource("/images/" + name + ".jpg");
        if (imgURL == null) {
            imgURL = Util.class.getResource("/images/" + name + ".png");
        }
        return new ImageIcon(imgURL);
    }
    public static final Executor executor = Executors.newCachedThreadPool();

    public static void invokeLater(Runnable runnable) {
        executor.execute(runnable);
    }
}
