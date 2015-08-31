package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.profiler.visualize.VStrategies.BarStrat;
import com.jivesoftware.os.upena.deployable.profiler.visualize.VStrategies.Colorings;
import com.jivesoftware.os.upena.deployable.profiler.visualize.VStrategies.StackOrder;
import com.jivesoftware.os.upena.deployable.profiler.visualize.VStrategies.StackStrat;
import com.jivesoftware.os.upena.deployable.profiler.visualize.VStrategies.ValueStrat;
import com.jivesoftware.os.upena.deployable.profiler.visualize.VisualizeProfile;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.IImage;
import com.jivesoftware.os.upena.deployable.profiler.visualize.paint.XY_I;
import com.jivesoftware.os.upena.deployable.region.ProfilerPluginRegion.ProfilerPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.commons.codec.binary.Base64;

/**
 *
 */
// soy.page.profilerPluginRegion
public class ProfilerPluginRegion implements PageRegion<ProfilerPluginRegionInput> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final VisualizeProfile visualizeProfile;

    public ProfilerPluginRegion(String template,
        SoyRenderer renderer,
        VisualizeProfile visualizeProfile) {
        this.template = template;
        this.renderer = renderer;
        this.visualizeProfile = visualizeProfile;
    }

    @Override
    public String getRootPath() {
        return "/ui/profiler";
    }

    public static class ProfilerPluginRegionInput implements PluginInput {

        String serviceName;
        int height;
        String valueStrategy;
        String stackStrategy;
        String barStrategy;
        String coloring;
        String stackOrder;
        int mouseX;
        int mouseY;

        public ProfilerPluginRegionInput(String serviceName,
            int height,
            String valueStrategy,
            String stackStrategy,
            String barStrategy,
            String coloring,
            String stackOrder,
            int mouseX,
            int mouseY) {
            this.serviceName = serviceName;
            this.height = height;
            this.valueStrategy = valueStrategy;
            this.stackStrategy = stackStrategy;
            this.barStrategy = barStrategy;
            this.coloring = coloring;
            this.stackOrder = stackOrder;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        @Override
        public String name() {
            return "Profiler";
        }

    }

    @Override
    public String render(String user, ProfilerPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();

        try {

            data.put("serviceName", String.valueOf(input.serviceName));
            data.put("serviceNames", visualizeProfile.getPossibleServiceNames());
            data.put("height", String.valueOf(input.height));
            data.put("valueStrategy", String.valueOf(input.valueStrategy));
            data.put("valueStrategies", Lists.transform(Arrays.asList(ValueStrat.values()), Enum::name));
            data.put("stackStrategy", String.valueOf(input.stackStrategy));
            data.put("stackStrategies", Lists.transform(Arrays.asList(StackStrat.values()), Enum::name));
            data.put("barStrategy", String.valueOf(input.barStrategy));
            data.put("barStrategies", Lists.transform(Arrays.asList(BarStrat.values()), Enum::name));
            data.put("coloring", String.valueOf(input.coloring));
            data.put("colorings", Lists.transform(Arrays.asList(Colorings.values()), Enum::name));
            data.put("stackOrder", String.valueOf(input.stackOrder));
            data.put("stackOrders", Lists.transform(Arrays.asList(StackOrder.values()), Enum::name));
            data.put("mouseX", String.valueOf(input.mouseX));
            data.put("mouseY", String.valueOf(input.mouseY));

            visualizeProfile.setServicName(input.serviceName);
            IImage ii = visualizeProfile.render(input.height,
                ValueStrat.valueOf(input.valueStrategy),
                StackStrat.valueOf(input.stackStrategy),
                BarStrat.valueOf(input.barStrategy),
                Colorings.valueOf(input.coloring),
                StackOrder.valueOf(input.stackOrder),
                new XY_I(input.mouseX, input.mouseY));

            System.out.println("II:" + ii);
            if (ii != null) {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    ImageIO.write((BufferedImage) ii.data(0), "PNG", baos);
                } catch (IOException x) {
                    throw new RuntimeException(x);
                }

                String base64ii = Base64.encodeBase64String(baos.toByteArray());
                data.put("profile", "data:image/png;base64," + base64ii);
            }

        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Profiler";
    }

}
