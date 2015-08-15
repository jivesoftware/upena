package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.HomeRegion.HomeInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import java.util.Map;

/**
 *
 */
public class HomeRegion implements PageRegion<HomeInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    
    public HomeRegion(String template, SoyRenderer renderer) {
        this.template = template;
        this.renderer = renderer;
    }

    @Override
    public String getRootPath() {
        return "/";
    }

    public static class HomeInput implements PluginInput {

        final String wgetURL;
        final String upenaClusterName;

        public HomeInput(String wgetURL, String upenaClusterName) {
            this.wgetURL = wgetURL;
            this.upenaClusterName = upenaClusterName;
        }

        @Override
        public String name() {
            return "Upana";
        }

    }

    @Override
    public String render(String user, HomeInput input) {
        Map<String, Object> data = Maps.newHashMap();
        data.put("wgetURL", input.wgetURL);
        data.put("upenaClusterName", input.upenaClusterName);
        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Upana";
    }

}
