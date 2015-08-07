package com.jivesoftware.os.upena.deployable.region;

import com.jivesoftware.os.upena.deployable.region.HeaderRegion.HeaderInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import java.util.HashMap;

// soy.chrome.headerRegion
public class HeaderRegion implements Region<HeaderInput> {

    private final String template;
    private final SoyRenderer renderer;

    public HeaderRegion(String template, SoyRenderer renderer) {
        this.template = template;
        this.renderer = renderer;
    }

    static public class HeaderInput extends HashMap<String, Object> implements PluginInput {

        @Override
        public String name() {
            return "Upena";
        }
    }

    @Override
    public String render(String user, HeaderInput input) {
        return renderer.render(template, input);
    }
}
