/*
 * UV.java.java
 *
 * Created on 01-03-2010 01:34:44 PM
 *
 * Copyright 2010 Jonathan Colt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jivesoftware.os.upena.deployable.profiler.visualize.paint;

/**
 *
 * @author Administrator
 */
public class UV {



    /**
     *
     */
    public static final float cS = -1.0f;// S = shrink = -1;
    /**
     *
     */
    public static final float cI = 0.0f;// I = ignore = 0;
    /**
     *
     */
    public static final float cG = 1.0f;// G = grow = 1;

    /**
     *
     */
    public static AFont[] fonts = new AFont[]{
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 8),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 10),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 18),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 24),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 32),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 18),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 18),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cItalic | IFontConstants.cBold, 14),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cBold, 15),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 15),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cItalic, 14),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cItalic, 24),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 15),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 15)
    };
    /**
     *
     */
    public static int cMicro = 0;
    /**
     *
     */
    public static int cSmall = 1;
    /**
     *
     */
    public static int cMedium = 2;
    /**
     *
     */
    public static int cLarge = 3;
    /**
     *
     */
    public static int cJumbo = 4;
    /**
     *
     */
    public static int cMessage = 5;
    /**
     *
     */
    public static int cTitle = 6;
    /**
     *
     */
    public static int cMenu = 7;
    /**
     *
     */
    public static int cBold = 8;
    /**
     *
     */
    public static int cText = 9;
    /**
     *
     */
    public static int cItalic = 10;
    /**
     *
     */
    public static int cTab = 11;
    /**
     *
     */
    public static int cButton = 12;
    /**
     *
     */
    public static int cToolTip = 13;
    /**
     *
     */
    public static String[] cFontUsageNames = new String[]{
        "Micro",
        "Small",
        "Large",
        "Jumbo",
        "Message",
        "Title",
        "Menu",
        "Bold",
        "Text",
        "Italic",
        "Tab",
        "Button",
        "ToolTip"
    };
    /**
     *
     */
    public static AFont[] cTexts = new AFont[]{
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 10),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 11),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 12),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 13),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 14),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 15),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 16),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 17),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 18),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 19),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 20),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 21),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 22),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 23),
        new AFont(IFontConstants.cDefaultFontName, IFontConstants.cPlain, 24),};

    public static final Place cWW = new Place(0.0f, 0.5f, 0.0f, 0.5f, 0, 0);

}
