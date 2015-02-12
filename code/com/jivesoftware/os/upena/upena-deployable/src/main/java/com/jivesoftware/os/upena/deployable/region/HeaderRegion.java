package com.jivesoftware.os.upena.deployable.region;

import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import java.util.Collections;

// soy.chrome.headerRegion
public class HeaderRegion implements Region<Void> {

    private final String template;
    private final SoyRenderer renderer;

    public HeaderRegion(String template, SoyRenderer renderer) {
        this.template = template;
        this.renderer = renderer;
    }

    @Override
    public String render(Void input) {
        return renderer.render(template, Collections.<String, Object>emptyMap());
    }
}
