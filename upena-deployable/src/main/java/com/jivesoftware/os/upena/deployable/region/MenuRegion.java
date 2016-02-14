package com.jivesoftware.os.upena.deployable.region;

import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;

// soy.chrome.headerRegion
public class MenuRegion implements Region<HeaderRegion.HeaderInput> {

    private final String template;
    private final SoyRenderer renderer;

    public MenuRegion(String template, SoyRenderer renderer) {
        this.template = template;
        this.renderer = renderer;
    }


    @Override
    public String render(String user, HeaderRegion.HeaderInput input) {
        return renderer.render(template, input);
    }
}
