package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.UnauthorizedPluginRegion.UnauthorizedInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import java.util.Map;

/**
 *
 */
// soy.page.unauthorizedPluginRegion
public class UnauthorizedPluginRegion implements PageRegion<UnauthorizedInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;

    public UnauthorizedPluginRegion(String template,
        SoyRenderer renderer) {
        this.template = template;
        this.renderer = renderer;
    }

    @Override
    public String getRootPath() {
        return "/ui/auth";
    }

    public static class UnauthorizedInput implements PluginInput {

       
        public UnauthorizedInput() {
        }

        @Override
        public String name() {
            return "Unauthorized";
        }
    }

    @Override
    public String render(String user, UnauthorizedInput input) {
        Map<String, Object> data = Maps.newHashMap();
        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Unauthorized";
    }

}
