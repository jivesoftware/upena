package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.OktaMFAAuthPluginRegion.OktaAuthInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import java.util.Map;

/**
 *
 */
// soy.page.authPluginRegion
public class OktaMFAAuthPluginRegion implements PageRegion<OktaAuthInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;

    public OktaMFAAuthPluginRegion(String template,
        SoyRenderer renderer) {
        this.template = template;
        this.renderer = renderer;
    }

    @Override
    public String getRootPath() {
        return "/ui/auth/okta/mfa";
    }

    public static class OktaAuthInput implements PluginInput {
        private final boolean unauthorized;

        public OktaAuthInput(boolean unauthorized) {
            this.unauthorized = unauthorized;
        }


        @Override
        public String name() {
            return "Okta MFA Auth";
        }
    }

    @Override
    public String render(String user, OktaAuthInput input) {
        Map<String, Object> data = Maps.newHashMap();
        data.put("message", input.unauthorized ? "MFA passcode failed." : "");
        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Okta MFA Auth";
    }

}
