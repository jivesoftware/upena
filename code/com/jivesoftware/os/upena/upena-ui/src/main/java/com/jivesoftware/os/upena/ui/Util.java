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
