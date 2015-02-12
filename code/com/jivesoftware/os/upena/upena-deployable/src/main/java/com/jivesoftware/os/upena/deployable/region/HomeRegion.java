package com.jivesoftware.os.upena.deployable.region;

import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import java.util.Collections;

/**
 *
 */
public class HomeRegion implements PageRegion<Void> {

    private final String template;
    private final SoyRenderer renderer;

    public HomeRegion(String template, SoyRenderer renderer) {
        this.template = template;
        this.renderer = renderer;
    }

    @Override
    public String render(Void input) {
        return renderer.render(template, Collections.<String, Object>emptyMap());
    }

    @Override
    public String getTitle() {
        return "Home";
    }
}
