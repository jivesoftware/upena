/*
 * AColor.java.java
 *
 * Created on 01-03-2010 01:29:41 PM
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
public class AColor extends ASetObject {

    /**
     *
     */
    public final static AColor offWhite = new AColor(250, 250, 255);
    /**
     *
     */
    public final static AColor lightBrown = new AColor(255, 160, 80);
    /**
     *
     */
    public final static AColor brown = new AColor(215, 120, 40);
    /**
     *
     */
    public final static AColor white = new AColor(255, 255, 255);
    /**
     *
     */
    public final static AColor lightestGray = new AColor(220, 220, 220);
    /**
     *
     */
    public final static AColor lightGray = new AColor(192, 192, 192);
    /**
     *
     */
    public final static AColor gray = new AColor(128, 128, 128);
    /**
     *
     */
    public final static AColor darkGray = new AColor(64, 64, 64);
    /**
     *
     */
    public final static AColor black = new AColor(0, 0, 0);
    /**
     *
     */
    public final static AColor red = new AColor(255, 0, 0);
    /**
     *
     */
    public final static AColor lightBlue = new AColor(136, 197, 255);
    /**
     *
     */
    public final static AColor lightRed = new AColor(255, 225, 225);
    /**
     *
     */
    public final static AColor lightRedGray = new AColor(255, 192, 192);
    /**
     *
     */
    public final static AColor lightPurple = new AColor(215, 128, 255);
    /**
     *
     */
    public final static AColor purple = new AColor(175, 0, 255);
    /**
     *
     */
    public final static AColor darkPurple = new AColor(88, 0, 128);
    /**
     *
     */
    public final static AColor pink = new AColor(255, 175, 255);
    /**
     *
     */
    public final static AColor aqua = new AColor(175, 255, 255);
    /**
     *
     */
    public final static AColor orange = new AColor(255, 200, 0);
    /**
     *
     */
    public final static AColor yellowOrange = new AColor(255, 227, 0);
    /**
     *
     */
    public final static AColor yellow = new AColor(255, 255, 0);
    /**
     *
     */
    public final static AColor green = new AColor(0, 255, 0);
    /**
     *
     */
    public final static AColor magenta = new AColor(255, 0, 255);
    /**
     *
     */
    public final static AColor cyan = new AColor(0, 255, 255);
    /**
     *
     */
    public final static AColor lightBlueGray = new AColor(192, 192, 255);
    /**
     *
     */
    public final static AColor grayBlue = new AColor(128, 128, 192);
    /**
     *
     */
    public final static AColor blue = new AColor(0, 0, 255);
    /**
     *
     */
    public final static AColor darkBlue = new AColor(0, 0, 128);
    /**
     *
     */
    public final static AColor salmon = new AColor(255, 0, 0).desaturate(0.70f);
    /**
     *
     */
    public final static AColor[] colors = new AColor[]{
        lightBrown,
        brown,
        white,
        lightestGray,
        lightGray,
        gray,
        darkGray,
        black,
        red,
        lightRed,
        lightRedGray,
        lightPurple,
        purple,
        pink,
        aqua,
        orange,
        yellowOrange,
        yellow,
        green,
        magenta,
        cyan,
        lightBlueGray,
        lightBlue,
        grayBlue,
        blue,
        salmon
    };

    /**
     *
     */
    protected int color = 0;
    /**
     *
     */
    protected Object cacheColor;

    /**
     *
     */
    public AColor() {
    }

    /**
     *
     * @param _rgba
     */
    public AColor(int _rgba) {
        color = _rgba;
    }

    /**
     *
     * @param _r
     * @param _g
     * @param _b
     */
    public AColor(int _r, int _g, int _b) {
        this(_r, _g, _b, 255);
    }

    /**
     *
     * @param _r
     * @param _g
     * @param _b
     * @param _a
     */
    public AColor(int _r, int _g, int _b, int _a) {
        color = rgba(_r, _g, _b, _a);
    }

    /**
     *
     * @param _hue
     * @param _sat
     * @param _bri
     */
    public AColor(float _hue, float _sat, float _bri) {
        this(HSBtoRGB(_hue, _sat, _bri));
    }

    /**
     *
     * @param _gray
     */
    public AColor(float _gray) {
        _gray = range(_gray, 0, 1);
        int gray = (int) (255 * _gray);
        color = rgba(gray, gray, gray, 255);
    }

    /**
     *
     * @param _gray
     * @param _alpha
     */
    public AColor(float _gray, float _alpha) {
        _gray = range(_gray, 0, 1);
        int gray = (int) (255 * _gray);
        color = rgba(gray, gray, gray, (int) (255 * _alpha));
    }

    /**
     *
     * @return
     */
    @Override
    public Object hashObject() {
        return color;
    }

    /**
     *
     * @param _color
     */
    public void set8BitColor(int _color) {
        cacheColor = null;
        color = _color;
    }

    /**
     *
     * @param _precision
     * @return
     */
    public AColor color(double _precision) {
        int max = (int) (255 * _precision);
        float[] hsb = RGBtoHSB(getR(), getG(), getB(), null);
        hsb[0] = (float) ((int) (hsb[0] * max)) / (float) max;
        hsb[1] = (float) ((int) (hsb[1] * max)) / (float) max;
        hsb[2] = (float) ((int) (hsb[2] * max)) / (float) max;
        return new AColor(HSBtoRGB(hsb[0], hsb[1], hsb[2]));
    }

    /**
     *
     * @return
     */
    public int getR() {
        return (color >> 16) & 0xFF;
    }

    /**
     *
     * @return
     */
    public int getG() {
        return (color >> 8) & 0xFF;
    }

    /**
     *
     * @return
     */
    public int getB() {
        return (color) & 0xFF;
    }

    /**
     *
     * @return
     */
    public int getA() {
        return (color >> 24) & 0xFF;
    }

    /**
     *
     * @param _v
     */
    public void setR(float _v) {
        setR((int) (_v * 255));
    }

    /**
     *
     * @param _v
     */
    public void setG(float _v) {
        setG((int) (_v * 255));
    }

    /**
     *
     * @param _v
     */
    public void setB(float _v) {
        setB((int) (_v * 255));
    }

    /**
     *
     * @param _v
     */
    public void setA(float _v) {
        setA((int) (_v * 255));
    }

    /**
     *
     * @param _r
     */
    public void setR(int _r) {
        color = rgba(_r, getG(), getB(), getA());
    }

    /**
     *
     * @param _g
     */
    public void setG(int _g) {
        color = rgba(getR(), _g, getB(), getA());
    }

    /**
     *
     * @param _b
     */
    public void setB(int _b) {
        color = rgba(getR(), getG(), _b, getA());
    }

    /**
     *
     * @param _a
     */
    public void setA(int _a) {
        color = rgba(getR(), getG(), getB(), _a);
    }

    /**
     *
     * @param _color
     */
    public void setColor(AColor _color) {
        color = _color.color;
        cacheColor = null;
    }

    /**
     *
     * @param _color
     */
    public void setColor(int _color) {
        color = _color;
        cacheColor = null;
    }

    private int rgba(int _r, int _g, int _b, int _a) {
        _r = range(_r, 0, 255);
        _g = range(_g, 0, 255);
        _b = range(_b, 0, 255);
        _a = range(_a, 0, 255);

        cacheColor = null;

        return ((_a & 0xFF) << 24)
            | ((_r & 0xFF) << 16)
            | ((_g & 0xFF) << 8)
            | ((_b & 0xFF));
    }

    /**
     *
     * @param _h
     * @param _s
     * @param _b
     * @return
     */
    private static final float cFactor = 0.7f;

    /**
     *
     * @return
     */
    public AColor brighter() {
        int r = getR();
        int g = getG();
        int b = getB();

        int i = (int) (1.0f / (1.0f - cFactor));
        if (r == 0 && g == 0 && b == 0) {
            return new AColor(i, i, i);
        }

        if (r > 0 && r < i) {
            r = i;
        }
        if (g > 0 && g < i) {
            g = i;
        }
        if (b > 0 && b < i) {
            b = i;
        }

        return new AColor((int) (r / cFactor), (int) (g / cFactor), (int) (b / cFactor));
    }

    /**
     *
     * @return
     */
    public AColor darker() {
        int r = getR();
        int g = getG();
        int b = getB();
        return new AColor((int) (r * cFactor), (int) (g * cFactor), (int) (b * cFactor));
    }

    private AColor role(int _index, float _amount) {
        float[] hsb = RGBtoHSB(getR(), getG(), getB(), null);
        hsb[_index] += _amount;
        if (hsb[_index] < 0.0f) {
            hsb[_index] = 0.0f;
        }
        if (hsb[_index] > 1.0f) {
            hsb[_index] = 1.0f;
        }
        return new AColor(HSBtoRGB(hsb[0], hsb[1], hsb[2]));
    }

    /**
     *
     * @param _amount
     * @return
     */
    public AColor desaturate(float _amount) {
        return role(1, -_amount);
    }

    /**
     *
     * @param _amount
     * @return
     */
    public AColor saturate(float _amount) {
        return role(1, _amount);
    }

    /**
     *
     * @param _amount
     * @return
     */
    public AColor darken(float _amount) {
        return role(2, -_amount);
    }

    /**
     *
     * @param _amount
     * @return
     */
    public AColor lighten(float _amount) {
        return role(2, _amount);
    }

    /**
     *
     * @return
     */
    public int get8BitRGBA() {
        return color;
    }//(0xff000000 | color); }

    /**
     *
     * @return
     */
    public int intValue() {
        return color;
    }//(0xff000000 | color); }

    /**
     *
     * @return
     */
    public Object getColor() {
        if (cacheColor != null) {
            return cacheColor;
        }
        cacheColor = VS.systemColor(this);
        return cacheColor;
    }

    @Override
    public String toString() {
        return getR() + " " + getG() + " " + getB() + " " + getA();
    }

    /**
     *
     * @param _zeroToOne
     * @return
     */
    public static AColor getWarmToCool(double _zeroToOne) {
        float h = (float) (_zeroToOne * 0.7);
        return new AColor(h, 1f, 1f);
    }

    /**
     *
     * @param _instance
     * @return
     */
    public static AColor getHashSolid(Object _instance) {
        int h = URandom.rand(new URandom.Seed(_instance.hashCode()));

        int b = (h % 96) + 128;
        h >>= 2;
        int g = (h % 96) + 128;
        h >>= 4;
        int r = (h % 96) + 128;
        h >>= 8;
        return new AColor(r, g, b);
    }

    /**
     *
     * @param _color
     * @return
     */
    public AColor darken(AColor _color) {
        if (_color == null) {
            return new AColor(color);
        }
        return new AColor(Math.min(getR(), _color.getR()), Math.min(getG(), _color.getG()), Math.min(getB(), _color.getB()));
    }

    /**
     *
     * @param _color
     * @return
     */
    public AColor lighten(AColor _color) {
        if (_color == null) {
            return new AColor(color);
        }
        return new AColor(Math.max(getR(), _color.getR()), Math.max(getG(), _color.getG()), Math.max(getB(), _color.getB()));
    }

    /**
     *
     * @param _color
     * @return
     */
    public AColor add(AColor _color) {
        if (_color == null) {
            return new AColor(color);
        }
        return new AColor((getR() + _color.getR()) / 2, (getG() + _color.getG()) / 2, (getB() + _color.getB()) / 2);
    }

    @Override
    public int hashCode() {
        return color;
    }

    @Override
    public boolean equals(Object _instance) {
        if (_instance == this) {
            return true;
        }
        if (_instance instanceof Integer) {
            return color == ((Integer) _instance);
        }
        if (_instance instanceof AColor) {
            return color == ((AColor) _instance).color;
        }
        return false;
    }

    /**
     *
     * @param r
     * @param g
     * @param b
     * @param hsbvals
     * @return
     */
    public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
        float hue, saturation, brightness;
        if (hsbvals == null) {
            hsbvals = new float[3];
        }
        int cmax = (r > g) ? r : g;
        if (b > cmax) {
            cmax = b;
        }
        int cmin = (r < g) ? r : g;
        if (b < cmin) {
            cmin = b;
        }

        brightness = ((float) cmax) / 255.0f;
        if (cmax != 0) {
            saturation = ((float) (cmax - cmin)) / ((float) cmax);
        } else {
            saturation = 0;
        }
        if (saturation == 0) {
            hue = 0;
        } else {
            float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
            float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
            float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
            if (r == cmax) {
                hue = bluec - greenc;
            } else if (g == cmax) {
                hue = 2.0f + redc - bluec;
            } else {
                hue = 4.0f + greenc - redc;
            }
            hue /= 6.0f;
            if (hue < 0) {
                hue += 1.0f;
            }
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }

    /**
     *
     * @param hue
     * @param saturation
     * @param brightness
     * @return
     */
    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) java.lang.Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 1:
                    r = (int) (q * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 2:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (t * 255.0f + 0.5f);
                    break;
                case 3:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 4:
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 5:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                    break;
            }
        }
        return 0xff000000 | (r << 16) | (g << 8) | (b);
    }

    public static float range(float _v, float _min, float _max) {
        if (_v < _min) {
            return _min;
        } else if (_v > _max) {
            return _max;
        }
        return _v;
    }

    public static int range(int _v, int _min, int _max) {
        if (_v < _min) {
            return _min;
        } else if (_v > _max) {
            return _max;
        }
        return _v;
    }

}
