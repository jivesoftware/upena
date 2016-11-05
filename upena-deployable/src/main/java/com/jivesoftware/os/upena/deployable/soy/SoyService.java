package com.jivesoftware.os.upena.deployable.soy;

import com.google.common.collect.Lists;
import com.jivesoftware.os.upena.deployable.region.ChromeRegion;
import com.jivesoftware.os.upena.deployable.region.HeaderRegion;
import com.jivesoftware.os.upena.deployable.region.HomeRegion;
import com.jivesoftware.os.upena.deployable.region.HomeRegion.HomeInput;
import com.jivesoftware.os.upena.deployable.region.MenuRegion;
import com.jivesoftware.os.upena.deployable.region.PageRegion;
import com.jivesoftware.os.upena.deployable.region.PluginHandle;
import com.jivesoftware.os.upena.deployable.region.PluginInput;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.HostKey;
import java.util.List;

/**
 *
 */
public class SoyService {

    private final SoyRenderer renderer;
    private final HeaderRegion headerRegion;
    private final MenuRegion menuRegion;
    private final HomeRegion homeRegion;
    private final String cluster;
    private final HostKey hostKey;
    private final UpenaStore upenaStore;

    private final List<PluginHandle> plugins = Lists.newCopyOnWriteArrayList();

    public SoyService(
        SoyRenderer renderer,
        HeaderRegion headerRegion,
        MenuRegion menuRegion,
        HomeRegion homeRegion,
        String cluster,
        HostKey hostKey,
        UpenaStore upenaStore
    ) {
        this.renderer = renderer;
        this.headerRegion = headerRegion;
        this.menuRegion = menuRegion;
        this.homeRegion = homeRegion;
        this.cluster = cluster;
        this.hostKey = hostKey;
        this.upenaStore = upenaStore;

    }

    public String render(String user) throws Exception {

        return chrome("soy.chrome.chromeRegion", homeRegion).render(user, new HomeInput());
    }

    public void registerPlugin(PluginHandle plugin) {
        plugins.add(plugin);
    }

    private <I extends PluginInput, R extends PageRegion<I>> ChromeRegion<I, R> chrome(String template, R region) {
        return new ChromeRegion<>(template,
            renderer,
            headerRegion,
            menuRegion,
            plugins,
            region,
            cluster,
            hostKey,
            upenaStore);
    }

    public String renderOverview(String user) throws Exception {
        return homeRegion.renderOverview(user);
    }


    public <I extends PluginInput> String renderNoChromePlugin(String user, PageRegion<I> pluginRegion, I input) throws Exception {
        return chrome("soy.chrome.noChromeRegion", pluginRegion).render(user, input);
    }

    public <I extends PluginInput> String renderPlugin(String user, PageRegion<I> pluginRegion, I input) throws Exception {
        return chrome("soy.chrome.chromeRegion",pluginRegion).render(user, input);
    }

    public <I extends PluginInput> String wrapWithChrome(String path, String user, String name, String title, String htmlRegion) {
        return chrome("soy.chrome.chromeRegion", null).render(path, user, name, title, htmlRegion);
    }

}
