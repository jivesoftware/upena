package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.HeaderRegion.HeaderInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
// soy.chrome.chromeRegion
public class ChromeRegion<I extends PluginInput, R extends PageRegion<I>> implements Region<I> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final HeaderRegion headerRegion;
    private final MenuRegion menuRegion;
    private final List<ManagePlugin> plugins;
    private final R region;
    private final String cluster;
    private final HostKey hostKey;
    private final UpenaStore upenaStore;

    public ChromeRegion(String template,
        SoyRenderer renderer,
        HeaderRegion headerRegion,
        MenuRegion menuRegion,
        List<ManagePlugin> plugins,
        R region,
        String cluster,
        HostKey hostKey,
        UpenaStore upenaStore) {

        this.template = template;
        this.renderer = renderer;
        this.headerRegion = headerRegion;
        this.menuRegion = menuRegion;
        this.plugins = plugins;
        this.region = region;
        this.cluster = cluster;
        this.hostKey = hostKey;
        this.upenaStore = upenaStore;
    }

    @Override
    public String render(String user, I input) throws Exception {
        return render(region.getRootPath(), user, input.name(), region.getTitle(), region.render(user, input));

    }

    public String render(String path, String user, String name, String title, String htmlRegion) {
        List<Map<String, String>> p = Lists.transform(plugins, (input1) -> {
            Map<String, String> map = new HashMap<>();
            if (name.equals(input1.name)) {
                map.put("active", String.valueOf(name.equals(input1.name)));
            }
            if (input1.seperator != null) {
                map.put("seperator", input1.seperator);
            }
            map.put("name", input1.name);
            map.put("path", input1.path);
            map.put("glyphicon", input1.glyphicon);
            map.put("icon", input1.icon);
            return map;
        });
        HeaderInput headerData = new HeaderInput();
        headerData.put("plugins", p);
        headerData.put("user", user);

        List<Map<String, String>> instances = new ArrayList<>();
        int[] instance = new int[1];
        try {
            int[] i = new int[1];
            upenaStore.hosts.scan((HostKey key, Host value) -> {
                instances.add(ImmutableMap.of("host", value.name,
                    "name", String.valueOf("upena-" + i[0]),
                    "port", String.valueOf(value.port),
                    "path", path));
                if (key.equals(hostKey)) {
                    instance[0] = i[0];
                }
                i[0]++;
                return true;
            });
        } catch (Exception x) {
            LOG.error("Failure.", x);
        }
        headerData.put("cluster", cluster);
        headerData.put("instance", String.valueOf(instance[0]));
        headerData.put("total", String.valueOf(instances.size()));
        headerData.put("instances", instances);

        Map<String, Object> data = Maps.newHashMap();
        data.put("header", headerRegion.render(user, headerData));
        data.put("menu", menuRegion.render(user, headerData));
        data.put("region", htmlRegion);
        data.put("title", title);
        data.put("plugins", p);
        data.put("user", user);
        return renderer.render(template, data);

    }

}
