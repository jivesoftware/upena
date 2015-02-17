package com.jivesoftware.os.upena.deployable.soy;

import com.google.common.collect.Lists;
import com.jivesoftware.os.upena.deployable.region.ChromeRegion;
import com.jivesoftware.os.upena.deployable.region.HeaderRegion;
import com.jivesoftware.os.upena.deployable.region.HomeRegion.HomeInput;
import com.jivesoftware.os.upena.deployable.region.ManagePlugin;
import com.jivesoftware.os.upena.deployable.region.PageRegion;
import java.util.List;

/**
 *
 */
public class SoyService {

    private final SoyRenderer renderer;
    private final HeaderRegion headerRegion;
    private final PageRegion<HomeInput> homeRegion;

    private final List<ManagePlugin> plugins = Lists.newCopyOnWriteArrayList();

    public SoyService(
        SoyRenderer renderer,
        HeaderRegion headerRegion,
        PageRegion<HomeInput> homeRegion
    ) {
        this.renderer = renderer;
        this.headerRegion = headerRegion;
        this.homeRegion = homeRegion;

    }

    public String render(String upenaJarWGetURL, String upenaClusterName) {

        return chrome(homeRegion).render(new HomeInput(upenaJarWGetURL, upenaClusterName));
    }

    public void registerPlugin(ManagePlugin plugin) {
        plugins.add(plugin);
    }

    private <I, R extends PageRegion<I>> ChromeRegion<I, R> chrome(R region) {
        return new ChromeRegion<>("soy.chrome.chromeRegion", renderer, headerRegion, plugins, region);
    }

    public <I> String renderPlugin(PageRegion<I> pluginRegion, I input) {
        return chrome(pluginRegion).render(input);
    }
}
