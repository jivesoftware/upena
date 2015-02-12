package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import java.util.List;
import java.util.Map;

/**
 *
 */
// soy.chrome.chromeRegion
public class ChromeRegion<I, R extends PageRegion<I>> implements Region<I> {

    private final String template;
    private final SoyRenderer renderer;
    private final HeaderRegion headerRegion;
    private final List<ManagePlugin> plugins;
    private final R region;

    public ChromeRegion(String template, SoyRenderer renderer, HeaderRegion headerRegion, List<ManagePlugin> plugins, R region) {
        this.template = template;
        this.renderer = renderer;
        this.headerRegion = headerRegion;
        this.plugins = plugins;
        this.region = region;
    }

    @Override
    public String render(I input) {
        Map<String, Object> data = Maps.newHashMap();
        data.put("header", headerRegion.render(null));
        data.put("region", region.render(input));
        data.put("title", region.getTitle());
        data.put("plugins", Lists.transform(plugins, new Function<ManagePlugin, Map<String, String>>() {
            @Override
            public Map<String, String> apply(ManagePlugin input) {
                return ImmutableMap.of("name", input.name, "path", input.path);
            }
        }));
        return renderer.render(template, data);

        /*
        // inject js page region module data
        List<String> jsmodulesVal = Arrays.asList(JSProcessor.classToAMDPath(region.getClass()));

        context.put("jsmodules").value(jsmodulesVal);
        */
    }

}
