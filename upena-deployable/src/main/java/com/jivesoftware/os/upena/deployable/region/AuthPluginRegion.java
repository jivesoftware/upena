package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.AuthPluginRegion.AuthInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import java.util.Map;

/**
 *
 */
// soy.page.authPluginRegion
public class AuthPluginRegion implements PageRegion<AuthInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;

    public AuthPluginRegion(String template,
        SoyRenderer renderer) {
        this.template = template;
        this.renderer = renderer;
    }

    @Override
    public String getRootPath() {
        return "/ui/auth";
    }

    public static class AuthInput implements PluginInput {

        private final String username;
        private final boolean unauthorized;

        public AuthInput(String username, boolean unauthorized) {
            this.username = username;
            this.unauthorized = unauthorized;
        }

        @Override
        public String name() {
            return "Auth";
        }
    }

    @Override
    public String render(String user, AuthInput input) {
        Map<String, Object> data = Maps.newHashMap();
        data.put("username", input.username);
        data.put("message", input.unauthorized ? "Login failed." : "");
        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Auth";
    }

}
