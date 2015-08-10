package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jivesoftware.os.upena.deployable.region.HeaderRegion.HeaderInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
// soy.chrome.chromeRegion
public class ChromeRegion<I extends PluginInput, R extends PageRegion<I>> implements Region<I> {

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
    public String render(String user, I input) {
        List<Map<String, String>> p = Lists.transform(plugins, (input1) -> {
            Map<String, String> map = new HashMap<>();
            map.put("name", input1.name);
            if (input.name().equals(input1.name)) {
                map.put("active", String.valueOf(input.name().equals(input1.name)));
            }
            map.put("path", input1.path);
            map.put("glyphicon", input1.glyphicon);
            map.put("icon", input1.icon);
            return map;
        });
        HeaderInput headerData = new HeaderInput();
        headerData.put("plugins", p);
        headerData.put("user", user);

        Map<String, Object> data = Maps.newHashMap();
        data.put("header", headerRegion.render(user, headerData));
        data.put("region", region.render(user, input));
        data.put("title", region.getTitle());
        data.put("plugins", p);
        data.put("user", user);
        return renderer.render(template, data);

    }

}
