/*
 * Copyright 2013 jonathan.colt.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jivesoftware.os.upena.deployable.profiler.visualize;

import com.jivesoftware.os.mlogger.core.MinMaxInt;
import com.jivesoftware.os.upena.deployable.profiler.model.Call;
import com.jivesoftware.os.upena.deployable.profiler.model.CallClass;
import com.jivesoftware.os.upena.deployable.profiler.model.CallDepth;
import com.jivesoftware.os.upena.deployable.profiler.model.CallStack;
import com.jivesoftware.os.upena.deployable.profiler.model.ClassMethod;
import com.jivesoftware.os.upena.deployable.profiler.model.ServicesCallDepthStack;
import com.jivesoftware.os.upena.deployable.profiler.visualize.VStrategies.Background;
import com.jivesoftware.os.upena.deployable.profiler.visualize.VStrategies.BarStrat;
import com.jivesoftware.os.upena.deployable.profiler.visualize.VStrategies.ClassNameStrat;
import com.jivesoftware.os.upena.deployable.profiler.visualize.VStrategies.Colorings;
import com.jivesoftware.os.upena.deployable.profiler.visualize.VStrategies.StackOrder;
import com.jivesoftware.os.upena.deployable.profiler.visualize.VStrategies.StackStrat;
import com.jivesoftware.os.upena.deployable.profiler.visualize.VStrategies.ValueStrat;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.AColor;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.AFont;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.ICanvas;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.IImage;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.IPath;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.MinMaxLong;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.Place;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.UV;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.VS;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.ViewColor;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.XYWH_I;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.XY_I;
import com.jivesoftware.os.upena.deployable.region.MinMaxDouble;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.jivesoftware.os.upena.deployable.profiler.visualize.paint.VS.c32BitARGB;

/**
 *
 * @author jonathan.colt
 */
public class VisualizeProfile {

    static BarFlavor interfaceBar = new BarFlavor(8);
    static BarFlavor methodBar = new BarFlavor(3);
    AtomicReference<String> selectedServiceName = new AtomicReference<>();
    final ServicesCallDepthStack servicesCallDepthStack;
    InterfaceArea over;
    InterfaceArea lastOver;
    InterfaceArea.MethodArea overMethod;
    InterfaceArea.MethodArea lastOverMethod;
    final ValuesHistogram valuesHistogram = new ValuesHistogram();
    Set<String> hideClass = new HashSet<>();
    Set<String> pinnedClass = new HashSet<>();
    Map<String, InterfaceArea> unique = new ConcurrentHashMap<>();
    Double shift = 0.0d;

    NameUtils nameUtils;

    public VisualizeProfile(NameUtils nameUtils,
        ServicesCallDepthStack callDepthStack) {
        this.nameUtils = nameUtils;
        this.servicesCallDepthStack = callDepthStack;
    }

    public List<Map<String, String>> getPossibleServiceNames() {
        return servicesCallDepthStack.getServiceNames();
    }

    public void setServicName(String serviceName) {
        selectedServiceName.set(serviceName);
    }

    public IImage render(Map<String, Object> data,
        boolean enabled,
        int _h,
        ValueStrat valueStrategy,
        StackStrat stackStrategy,
        BarStrat barStrategy,
        ClassNameStrat classNameStrat,
        Colorings coloring,
        Background background,
        StackOrder stackOrder,
        XY_I mp) {

        String serviceName = selectedServiceName.get();
        CallStack callStack = servicesCallDepthStack.callStackForServiceName(serviceName);
        if (callStack != null) {
            callStack.enabled.set(enabled);
            data.put("age", String.valueOf(System.currentTimeMillis() - callStack.lastSampleTimestampMillis.get()));
        }

        valuesHistogram.reset();

        int _x = 0;
        int _y = 0;
        int ox = _x;
        int oy = _y;

        if (selectedServiceName.get() == null) {
            return null;
        }

        CallDepth[] copy = servicesCallDepthStack.getCopy(serviceName);
        int totalDepth = copy.length;
        if (totalDepth == 0) {
            return null;
        }

        unique.clear();
        for (CallDepth callDepth : copy) {
            if (callDepth == null) {
                continue;
            }

            CallClass[] callClasses = callDepth.getCopy();
            int d = 0;
            for (CallClass callClass : callClasses) {
                String className = callClass.getName();
                if (!hideClass.contains(className)) {
                    InterfaceArea callArea = unique.get(className);
                    if (callArea == null) {
                        callArea = new InterfaceArea(callClass, valueStrategy);
                        unique.put(className, callArea);
                    }
                    callArea.joinCallsTo(d, callClass);
                }
                d++;
            }
        }

        for (InterfaceArea area : unique.values()) {
            for (Call call : area.callsTo) {
                InterfaceArea called = unique.get(call.getClassName());
                if (called != null) {
                    called.calledByCount++;
                }
            }
        }

        Map<String, Long> names = new HashMap<>();
        long maxAvg = 0;
        for (InterfaceArea area : unique.values()) {
            Object v = barStrategy.getStrategy().value(area);
            if (v instanceof Long) {
                maxAvg = Math.max(maxAvg, (Long) v);
            } else {
                if (!names.containsKey(area.getName())) {
                    names.put(v.toString(), maxAvg);
                    maxAvg++;
                }
            }
        }

        List<String> keys = new ArrayList<>(names.keySet());
        Collections.sort(keys);
        for (int i = 0; i < keys.size(); i++) {
            names.put(keys.get(i), (long) i);
        }

        List<CallDepthAreas> bars = new ArrayList<>();
        for (int i = 0; i < maxAvg + 1; i++) {
            bars.add(new CallDepthAreas(valueStrategy, stackStrategy));
        }

        for (InterfaceArea area : unique.values()) {
            long depth;
            Object v = barStrategy.getStrategy().value(area);
            if (v instanceof Long) {
                depth = maxAvg - (Long) v;
            } else {
                depth = names.get(v.toString());
            }
            CallDepthAreas callDepthAreas = bars.get((int) depth);
            callDepthAreas.add(area.getName(), area);

            Object cn = classNameStrat.getStrategy().value(area);
            callDepthAreas.name = ((cn == null) ? "" : cn + " ") + barStrategy.getStrategy().name(v);
        }

        for (Iterator<CallDepthAreas> it = bars.iterator(); it.hasNext();) {
            CallDepthAreas callDepthAreas = it.next();
            if (callDepthAreas.areas.isEmpty()) {
                it.remove();
            }
        }

        int widthPerDepth = 32;
        CallDepthAreas[] callDepthAreas = bars.toArray(new CallDepthAreas[bars.size()]);
        int _w = (16 * 40) + (callDepthAreas.length * (widthPerDepth + 96));

        IImage ii = VS.systemImage(_w, _h, c32BitARGB);
        ICanvas canvas = ii.canvas(0);
        if (background == Background.black) {
            ViewColor.onBlack();
            canvas.setColor(AColor.black);
            canvas.rect(true, 0, 0, _w, _h);
        } else if (background == Background.white) {
            ViewColor.onWhite();
            canvas.setColor(AColor.white);
            canvas.rect(true, 0, 0, _w, _h);
        } else {
        }

        _x += (16 * 20);
        _y += (16 * 10);
        _w -= (16 * 40);
        _h -= (16 * 20);

        int gap = Math.max(96, ((_w - bars.size() * widthPerDepth) / bars.size()));

        _x -= (shift * ((callDepthAreas.length * (widthPerDepth + gap)) - _w));
        int x = _x;
        if (stackOrder == StackOrder.desending) {
            for (int i = 0; i < callDepthAreas.length; i++) {
                callDepthAreas[i].assignArea(x, _y, widthPerDepth, _h);
                x += widthPerDepth + gap;
            }
        } else {
            for (int i = callDepthAreas.length - 1; i > -1; i--) {
                callDepthAreas[i].assignArea(x, _y, widthPerDepth, _h);
                x += widthPerDepth + gap;
            }
        }

        long maxV = 0;
        for (CallDepthAreas callDepthArea : callDepthAreas) {
            maxV = Math.max(maxV, callDepthArea.normalize());
            callDepthArea.stack();
            callDepthArea.size();
        }

        for (CallDepthAreas callDepthArea : callDepthAreas) {
            callDepthArea.histogram(maxV);
        }

        if (overMethod != null) {
            lastOverMethod = overMethod;
        }
        overMethod = null;

        if (over != null) {
            lastOver = over;
        }
        over = null;
        for (InterfaceArea callArea : unique.values()) {
            callArea.color = coloring.getColoring().value(callArea, maxV);
            if (new XYWH_I(callArea.x, callArea.y, callArea.w, callArea.h).contains(mp)) {
                over = callArea;
                for (InterfaceArea.MethodArea methodArea : callArea.methodAreas) {
                    if (new XYWH_I(methodArea.x, methodArea.y, methodArea.w, methodArea.h).contains(mp)) {
                        overMethod = methodArea;
                    }
                }
            }
        }
        for (InterfaceArea callArea : unique.values()) {
            callArea.paint(canvas);
        }

        List<Line> lines = new ArrayList<>();
        for (InterfaceArea area : unique.values()) {
            if (hideClass.contains(area.getName())) {
                continue;
            }
            List<InterfaceArea.MethodArea> toAreas = new ArrayList<>();
            int totalH = 0;
            for (Call call : area.callsTo) {
                InterfaceArea called = unique.get(call.getClassName());
                if (called != null) {
                    called.calledByCount++;
                    InterfaceArea.MethodArea methodArea = called.mapMethodAreas.get(call.getMethodName());
                    if (methodArea == null) {
                        System.out.println("Missing method for " + call.getClassName() + " " + call.getMethodName());
                    } else {
                        toAreas.add(methodArea);
                        totalH += methodArea.h;
                    }
                } else {
                    System.out.println("Missing area for " + call.getClassName());
                }
            }

            double s = 0;
            for (InterfaceArea.MethodArea toArea : toAreas) {
                double ph = MinMaxDouble.zeroToOne(0, totalH, toArea.h);
                XYWH_I fromRect = area.rect(s, ph);
                s += ph;
                XYWH_I toRect = new XYWH_I(toArea.x, toArea.y, toArea.w, toArea.h);
                lines.add(new Line(area, fromRect, toArea, toRect));
            }
        }

        for (Line l : lines) {
            l.paint(canvas, _x, _y, _w, _h);
        }

        for (int i = 0; i < callDepthAreas.length; i++) {
            callDepthAreas[i].paint(canvas);
        }

        if (over != null || lastOver != null) {
            InterfaceArea paint = over;
            if (over == null) {
                paint = lastOver;
            }

            if (paint != null) {
                Map<String, Object> over = new HashMap<>();
                
                CallClass callClass = paint.callClass;
                List<Map<String, Object>> methods = new ArrayList<>();
                for (String methodName : callClass.getMethods()) {

                    ClassMethod method = callClass.getMethod(methodName);
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", method.getMethodName());
                    m.put("success", String.valueOf(method.getCalled()));
                    m.put("successLatency", String.valueOf(method.getSuccesslatency()));
                    m.put("failed", String.valueOf(method.getFailed()));
                    m.put("failedLatency", String.valueOf(method.getFailedlatency()));
                    methods.add(m);
                }
                over.put("methods", methods);


                over.put("name", callClass.getName());
                over.put("success", String.valueOf(callClass.getCalled()));
                over.put("successLatency", String.valueOf(callClass.getSuccesslatency()));
                over.put("failed", String.valueOf(callClass.getFailed()));
                over.put("failedLatency", String.valueOf(callClass.getFailedlatency()));

                List<Map<String, Object>> callsTo = new ArrayList<>();
                for (Call call : callClass.getCalls()) {

                    Map<String, Object> c = new HashMap<>();
                    c.put("className", call.getClassName());
                    c.put("methodName", call.getMethodName());
                    callsTo.add(c);
                }
                over.put("calls", callsTo);
                
                data.put("over", over);
            }

            XY_I op = new XY_I(paint.x + ((paint.w / 4) * 3), paint.y + (paint.h / 2));
            if (overMethod != null) {
                op = new XY_I(overMethod.x + (overMethod.w / 2), overMethod.y + (overMethod.h / 2));
            }

            XYWH_I rect = paint(canvas, op,
                new XY_I(op.x + (16 * 5), op.y - (16 * 5)),
                nameUtils.simpleInterfaceName(paint),
                UV.fonts[UV.cText], paint.color);

            if (overMethod != null) {
                paint(canvas, new XY_I(rect.x + rect.w, rect.y + rect.h - 8),
                    new XY_I(rect.x + rect.w + 32, rect.y + rect.h - 8),
                    nameUtils.simpleMethodName(overMethod),
                    UV.fonts[UV.cText], AColor.gray);
            }

        }

        int bh = widthPerDepth;
        int bx = ox + bh;
        int by = oy + bh;
        int bw = 160;

        for (int i = 0; i < valuesHistogram.histogram.length; i++) {
            if (valuesHistogram.histogram[i] == 0) {
                continue;
            }
            interfaceBar.paintFlavor(canvas, bx, by, bw, bh, AColor.darkGray);
            interfaceBar.paintFlavor(canvas, bx + 4, by + 4,
                bh + (int) ((bw - bh) * (MinMaxInt.zeroToOne(0, valuesHistogram.max, valuesHistogram.histogram[i]))) - 8, bh - 8, AColor.getWarmToCool(
                    1f - (float) (i + 1) / (float) valuesHistogram.histogram.length));

            canvas.setFont(UV.fonts[UV.cText]);
            canvas.setColor(AColor.white);
            string(canvas, "" + valuesHistogram.histogram[i], UV.fonts[UV.cText], bx + 8, by - 4, UV.cWW, 0);

            by += bh;
        }

        return ii;

    }

    XYWH_I paint(ICanvas _g, XY_I from, XY_I at, String[] message, AFont font, AColor background) {

        _g.setFont(font);
        float mw = 0;
        float mh = 0;
        for (String m : message) {
            mw = Math.max(mw, font.getW(m));
            mh += font.getH(m);
        }

        _g.setColor(background);
        _g.setAlpha(0.9f, 0);
        _g.roundRect(true, at.x - 8, at.y - (int) (mh) - 8, (int) mw + 16, 16 + (int) mh, 8, 8);
        _g.roundRect(false, at.x - 8, at.y - (int) (mh) - 8, (int) mw + 16, 16 + (int) mh, 8, 8);
        _g.setAlpha(1f, 0);

        _g.setColor(AColor.white); //ViewColor.cThemeFont);
        float _y = at.y;
        for (String m : message) {
            float sh = font.getH(m);
            string(_g, m, font, at.x, (int) _y - (int) (mh), UV.cWW);
            _y += sh;
        }

        _g.setColor(ViewColor.cThemeFont);
        _g.line(from.x, from.y, at.x, at.y);

        _g.setColor(AColor.orange);
        _g.oval(true, from.x - 4, from.y - 4, 8, 8);
        _g.oval(true, at.x - 4, at.y - 4, 8, 8);
        return new XYWH_I(at.x - 8, at.y - (int) (mh) - 8, (int) mw, 16 + (int) mh);
    }

    class Line {

        final InterfaceArea fromArea;
        final XYWH_I fromRect;
        final InterfaceArea.MethodArea toArea;
        final XYWH_I toRect;

        public Line(InterfaceArea fromArea, XYWH_I fromRect, InterfaceArea.MethodArea toArea, XYWH_I toRect) {
            this.fromArea = fromArea;
            this.fromRect = fromRect;
            this.toArea = toArea;
            this.toRect = toRect;
        }

        void paint(ICanvas _g, int _x, int _y, int _w, int _h) {
            if (overMethod == null
                && (over == toArea.interfaceArea
                || over == fromArea
                || pinnedClass.contains(fromArea.getName())
                || pinnedClass.contains(toArea.interfaceArea.getName()))) {
                _g.setAlpha(1f, 0);
                _g.setColor(toArea.interfaceArea.color.brighter());

            } else if (overMethod != null && overMethod == toArea) {
                _g.setAlpha(1f, 0);
                _g.setColor(toArea.interfaceArea.color.brighter());
            } else {
                _g.setColor(AColor.gray);
                _g.setAlpha(0.5f, 0);
            }

            IPath path = VS.path();
            if (fromArea == toArea.interfaceArea) {
                fromToSelf(_h, path, _y, _g);
            } else if (fromArea.x > toArea.x) {
                fromToBehind(_h, path, _y, _g);
            } else {
                fromToAhead(path, _g);
            }

            interfaceBar.paintFlavor(_g, fromRect.x + fromRect.w, fromRect.y, 4, fromRect.h, toArea.interfaceArea.color);
            _g.setAlpha(1f, 0);

        }

        private void fromToAhead(IPath path, ICanvas _g) {
            moon(path, new XY_I(fromRect.x + fromRect.w, fromRect.y + (fromRect.h / 2)), new XY_I(toRect.x, toRect.y + (toRect.h / 2)), -6, 6);
            _g.fill(path);
            _g.setColor(AColor.darkGray);
            _g.draw(path);
        }

        private void fromToBehind(int _h, IPath path, int _y, ICanvas _g) {
            int pad = 20 + (int) (48 * (1f - ((float) fromRect.y / (float) _h)));

            path.moveTo(fromRect.x + fromRect.w, fromRect.y);
            path.curveTo(fromRect.x + fromRect.w, fromRect.y,
                fromRect.x + fromRect.w + pad, fromRect.y,
                fromRect.x + fromRect.w + pad, fromRect.y + (fromRect.h / 2) + pad);

            path.lineTo(fromRect.x + fromRect.w + pad, _y + _h + pad);
            path.lineTo(fromRect.x + fromRect.w + pad, fromRect.y + (fromRect.h / 2) + pad);

            path.curveTo(fromRect.x + fromRect.w + pad, fromRect.y + (fromRect.h / 2) + pad,
                fromRect.x + fromRect.w + pad / 2, fromRect.y + (fromRect.h / 2),
                fromRect.x + fromRect.w, fromRect.y + fromRect.h);

            path.closePath();
            _g.fill(path);

            path = VS.path();
            path.moveTo(fromRect.x + fromRect.w + pad, fromRect.y + (fromRect.h / 2) + pad);
            path.lineTo(fromRect.x + fromRect.w + pad, _y + _h + pad);
            path.curveTo(fromRect.x + fromRect.w + pad, _y + _h + pad,
                fromRect.x + fromRect.w + pad, _y + _h + pad + pad,
                fromRect.x + fromRect.w, _y + _h + pad + pad);

            path.lineTo(toRect.x, _y + _h + pad + pad);

            path.curveTo(toRect.x, _y + _h + pad + pad,
                toRect.x - pad, _y + _h + pad + pad,
                toRect.x - pad, _y + _h + pad);

            path.lineTo(toRect.x - pad, toRect.y + (toRect.h / 2) + pad);
            path.curveTo(toRect.x - pad, toRect.y + (toRect.h / 2) + pad,
                toRect.x - pad, toRect.y + (toRect.h / 2),
                toRect.x, toRect.y + (toRect.h / 2));
            arrowHead(path, toRect.x, toRect.y + (toRect.h / 2), 270, 10, 45);
            _g.draw(path);
        }

        private void fromToSelf(int _h, IPath path, int _y, ICanvas _g) {
            int pad = 20 + (int) (48 * ((float) fromRect.y / (float) _h));

            path.moveTo(fromRect.x + fromRect.w, fromRect.y);
            path.curveTo(fromRect.x + fromRect.w, fromRect.y,
                fromRect.x + fromRect.w + pad / 2, fromRect.y + (fromRect.h / 2),
                fromRect.x + fromRect.w + pad, fromRect.y + (fromRect.h / 2) - pad);

            path.lineTo(fromRect.x + fromRect.w + pad, _y - pad);
            path.lineTo(fromRect.x + fromRect.w + pad, fromRect.y + (fromRect.h / 2) - pad);

            path.curveTo(fromRect.x + fromRect.w + pad, fromRect.y + (fromRect.h / 2) - pad,
                fromRect.x + fromRect.w + pad, fromRect.y + (fromRect.h / 2),
                fromRect.x + fromRect.w, fromRect.y + (fromRect.h));

            path.closePath();
            _g.fill(path);

            path = VS.path();
            path.moveTo(fromRect.x + fromRect.w + pad, fromRect.y + (fromRect.h / 2) - pad);
            path.lineTo(fromRect.x + fromRect.w + pad, _y - pad);
            path.curveTo(fromRect.x + fromRect.w + pad, _y - pad,
                fromRect.x + fromRect.w + pad, _y - (pad + pad),
                fromRect.x + fromRect.w, _y - (pad + pad));

            path.lineTo(toRect.x, _y - (pad + pad));

            path.curveTo(toRect.x - pad, _y - (pad + pad),
                toRect.x - pad, _y - (pad + pad),
                toRect.x - pad, _y - pad);

            path.lineTo(toRect.x - pad, toRect.y + (toRect.h / 2) - pad);

            path.curveTo(toRect.x - pad, toRect.y + (toRect.h / 2) - pad,
                toRect.x - pad, toRect.y + (toRect.h / 2),
                toRect.x, toRect.y + (toRect.h / 2));
            arrowHead(path, toRect.x, toRect.y + (toRect.h / 2), 270, 10, 45);
            _g.draw(path);
        }
    }

    /*
     @Override
     public void mouseReleased(MouseReleased _e) {
     XY_I rp = _e.getPoint();
     for (InterfaceArea callArea : unique.values()) {
     if (new XYWH_I(callArea.x, callArea.y, callArea.w, callArea.h).contains(rp)) {
     if (_e.getClickCount() == 2) {
     hideClass.add(callArea.getName());
     return;
     } else {
     UV.popup(this, _e, new VPicked(callArea), true, true);
     return;
     }
     }
     }
     }

     class VPicked extends Viewer {

     public VPicked(final InterfaceArea callArea) {
     VChain c = new VChain(UV.cSWNW);
     if (pinnedClass.contains(callArea.getName())) {
     c.add(new VItem("un-pin") {
     @Override
     public void picked(IEvent _e) {
     super.picked(_e); //To change body of generated methods, choose Tools | Templates.
     pinnedClass.remove(callArea.getName());
     getRootView().dispose();
     }
     });
     } else {
     c.add(new VItem("pin") {
     @Override
     public void picked(IEvent _e) {
     super.picked(_e); //To change body of generated methods, choose Tools | Templates.
     pinnedClass.add(callArea.getName());
     getRootView().dispose();
     }
     });
     }

     c.add(new VItem("hide") {
     @Override
     public void picked(IEvent _e) {
     super.picked(_e); //To change body of generated methods, choose Tools | Templates.
     hideClass.add(callArea.getName());
     pinnedClass.remove(callArea.getName());
     getRootView().dispose();
     }
     });

     c.add(new VItem("showall") {
     @Override
     public void picked(IEvent _e) {
     super.picked(_e); //To change body of generated methods, choose Tools | Templates.
     hideClass.clear();
     getRootView().dispose();
     }
     });
     setContent(c);
     }
     }

   

     @Override
     public void mouseMoved(MouseMoved _e) {
     super.mouseMoved(_e); //To change body of generated methods, choose Tools | Templates.
     mp = _e.getPoint();
     repair();
     paint();
     }
     */
    class CallDepthAreas {

        private final ValueStrat valueStrategy;
        private final StackStrat stackStrategy;
        Map<String, InterfaceArea> areas = new HashMap<>();
        String name = "";
        int x;
        int y;
        int w;
        int h;
        InterfaceArea[] paintAreas;
        long total;

        public CallDepthAreas(ValueStrat valueStrat, StackStrat stackStrat) {
            this.valueStrategy = valueStrat;
            this.stackStrategy = stackStrat;
        }

        public void assignArea(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public void add(String className, InterfaceArea callArea) {
            areas.put(className, callArea);
        }

        public long normalize() {
            long max = 0;
            for (InterfaceArea callArea : areas.values()) {
                long v = valueStrategy.getStrategy().value(callArea);
                callArea.value = v;
                if (max < v) {
                    max = v;
                }
                total += v;
            }
            for (InterfaceArea callArea : areas.values()) {
                callArea.maxValue = max;
            }
            return max;
        }

        public void histogram(long maxValue) {
            for (InterfaceArea callArea : areas.values()) {
                valuesHistogram.value(MinMaxLong.zeroToOne(0, maxValue, callArea.value));
            }
        }

        // expected normalize() to have been called
        public void stack() {
            paintAreas = areas.values().toArray(new InterfaceArea[0]);
            Arrays.sort(paintAreas, (o1, o2) -> -new Long(stackStrategy.getStrategy().value(o1)).compareTo(stackStrategy.getStrategy().value(o2)));
        }

        // expected stack() to have been called
        public void size() {
            int ay = y;
            for (InterfaceArea interfaceArea : paintAreas) {
                int ah = (int) (h * MinMaxLong.zeroToOne(0, total, interfaceArea.value));
                interfaceArea.x = x;
                interfaceArea.y = ay;
                interfaceArea.w = w;
                interfaceArea.h = ah;
                ay += ah;
            }

            for (InterfaceArea interfaceArea : paintAreas) {
                interfaceArea.sizeMethods();
            }
        }

        // expected size() to hae been called
        public void paint(ICanvas _g) {
            _g.setColor(ViewColor.cThemeFont);
            _g.oval(true, x + w, y + h, 7, 7);
            _g.setFont(UV.fonts[UV.cTitle]);
            string(_g, name, UV.fonts[UV.cTitle], x + w, y + h, UV.cWW, 45);
        }
    }

    public class InterfaceArea {

        final CallClass callClass;
        final ValueStrat valueStrat;
        Set<Integer> depths = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
        int x;
        int y;
        int w;
        int h;
        long value;
        long maxValue;
        AColor color;
        int calledByCount;
        Set<Call> callsTo = new HashSet<>();
        ConcurrentHashMap<String, ClassMethod> classMethods = new ConcurrentHashMap<>();

        public InterfaceArea(CallClass callClass, ValueStrat valueStrat) {
            this.callClass = callClass;
            this.valueStrat = valueStrat;
            callsTo.addAll(callClass.getCalls());
            classMethods.putAll(callClass.getClassMethods());
        }

        String getName() {
            return callClass.getName();
        }

        public int averageDepth() {
            int sum = 0;
            for (int d : depths) {
                sum += d;
            }
            return sum / depths.size();
        }

        public void joinCallsTo(int depth, CallClass add) {
            depths.add(depth);
            callsTo.addAll(add.getCalls());
            classMethods.putAll(add.getClassMethods());
        }

        XYWH_I rect(double s, double ph) {

            int _y = y + (int) (h * s);
            int _h = (int) (h * ph);

            return new XYWH_I(x, _y, w, _h);
        }

        void paint(ICanvas _g) {
            _g.setAlpha(1f, 0);
            InterfaceArea _over = over;
            if (_over != null || pinnedClass.size() > 0) {
                if (_over != null && _over.callClass != null && !this.getName().equals(_over.getName())) {
                    _g.setAlpha(0.25f, 0);
                }
            }

            interfaceBar.paintFlavor(_g, x, y, w, h, color);

            _g.setColor(ViewColor.cThemeFont);
            _g.oval(true, x + w, y + h, 7, 7);
            _g.setFont(UV.fonts[UV.cText]);
            string(_g, "    " + valueStrat.getStrategy().name(value), UV.fonts[UV.cSmall], x + w, y, UV.cWW, 45);

            for (MethodArea methodArea : methodAreas) {
                methodArea.paint(_g);
            }

            _g.setAlpha(1f, 0);

        }
        MethodArea[] methodAreas = new MethodArea[0];
        Map<String, MethodArea> mapMethodAreas = new HashMap<>();

        private void sizeMethods() {
            ClassMethod[] cms = classMethods.values().toArray(new ClassMethod[0]);
            methodAreas = new MethodArea[cms.length];
            long total = 0;
            for (int i = 0; i < methodAreas.length; i++) {
                methodAreas[i] = new MethodArea(this, cms[i]);
                mapMethodAreas.put(methodAreas[i].classMethod.getMethodName(), methodAreas[i]);
                total += methodAreas[i].value;
            }

            long t = 0;
            for (int i = 0; i < methodAreas.length; i++) {
                long v = methodAreas[i].value;
                float p = (float) t / (float) total;
                float ph = (float) MinMaxLong.zeroToOne(0, total, v);
                methodAreas[i].x = x;
                methodAreas[i].y = y + (int) (h * p);
                methodAreas[i].w = (w / 2);
                methodAreas[i].h = (int) (ph * h);
                t += v;
            }

        }

        class MethodArea {

            InterfaceArea interfaceArea;
            ClassMethod classMethod;
            int x;
            int y;
            int w;
            int h;
            long value;

            public MethodArea(InterfaceArea interfaceArea, ClassMethod classMethod) {
                this.interfaceArea = interfaceArea;
                this.classMethod = classMethod;
                this.value = 1;//classMethod.getSuccesslatency();
            }

            public void paint(ICanvas _g) {
                methodBar.paintFlavor(_g, x, y, w, h, AColor.gray);
                if (interfaceArea.depths.contains(0)) {
                    _g.setColor(AColor.gray);
                    _g.line(x + (w / 2) - 32, y + (h / 2), x + (w / 2) - 3, y + (h / 2));
                    _g.setColor(AColor.green);
                    _g.oval(true, x + (w / 2) - 32, y + (h / 2) - 3, 6, 6);
                }
            }
        }
    }

    XY_I string(
        ICanvas _g, String _string, AFont _font, int _x, int _y, Place _place) {
        XY_I p = _font.place(_string, _x, _y, _place);
        _g.drawString(_string, p.x, p.y);
        return p;
    }

    XY_I string(
        ICanvas _g, String _string, AFont _font, int _x, int _y, Place _place, int _degress) {
        XY_I p = _font.place(_string, _x, _y, _place);
        _g.translate(p.x, p.y);
        _g.rotate(Math.toRadians(-_degress));
        _g.drawString(_string, 0, _font.getSize());
        _g.rotate(Math.toRadians(_degress));
        _g.translate(-p.x, -p.y);
        return p;
    }

    void arrowHead(IPath _p, float _x, float _y, float _direction, float _length, float _angle) {
        _p.moveTo(_x, _y);
        _direction -= (_angle / 2);
        _p.lineTo((float) (_x + (Math.sin(Math.toRadians(_direction)) * _length)), _y + (float) ((Math.sin(Math.toRadians(_direction + 90)) * _length)));

        _direction += _angle;
        _p.moveTo(_x, _y);
        _p.lineTo((float) (_x + (Math.sin(Math.toRadians(_direction)) * _length)), _y + (float) ((Math.sin(Math.toRadians(_direction + 90)) * _length)));
    }

    void moon(
        IPath _path,
        XY_I _f, XY_I _t,
        int _deflect1, int _deflect2) {

        double[] mxy1 = middle(_f.x, _f.y, _t.x, _t.y);
        double deflectX1 = (_f.y > _t.y) ? (_deflect1) : -(_deflect1);
        double deflectY1 = (_f.x > _t.x) ? -(_deflect1) : (_deflect1);
        mxy1[0] += deflectX1;
        mxy1[1] += deflectY1;

        double[] mxy2 = middle(_f.x, _f.y, _t.x, _t.y);
        double deflectX2 = (_f.y > _t.y) ? (_deflect2) : -(_deflect2);
        double deflectY2 = (_f.x > _t.x) ? -(_deflect2) : (_deflect2);
        mxy2[0] += deflectX2;
        mxy2[1] += deflectY2;

        _path.moveTo(_f.x, _f.y);
        _path.quadTo((float) mxy1[0], (float) mxy1[1], _t.x, _t.y);
        _path.moveTo(_t.x, _t.y);
        _path.quadTo((float) mxy2[0], (float) mxy2[1], _f.x, _f.y);

    }

    double middle(double a, double b) {
        return middle(a, b, 0.5d);
    }

    double middle(double a, double b, double _percentage) {
        double gap = a - b;
        double mid = a - (gap * _percentage);
        return mid;
    }

    double[] middle(double sx, double sy, double ex, double ey) {
        return middle(sx, sy, ex, ey, 0.5d);
    }

    double[] middle(double sx, double sy, double ex, double ey, double _percentage) {
        double gapX = (ex - sx);
        double gapY = (ey - sy);
        double midX = sx + (gapX * _percentage);
        double midY = sy + (gapY * _percentage);
        return new double[]{midX, midY};
    }
}
