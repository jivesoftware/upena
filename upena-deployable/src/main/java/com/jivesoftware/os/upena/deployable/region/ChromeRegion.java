package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.HeaderRegion.HeaderInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.HostKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

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
    private final List<PluginHandle> plugins;
    private final R region;
    private final String cluster;
    

    public ChromeRegion(String template,
        SoyRenderer renderer,
        HeaderRegion headerRegion,
        MenuRegion menuRegion,
        List<PluginHandle> plugins,
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
        
    }

    @Override
    public String render(String user, I input) throws Exception {
        return render(region.getRootPath(), user, input.name(), region.getTitle(), region.render(user, input));

    }

    public String render(String path, String user, String name, String title, String htmlRegion) {
        HeaderInput headerData = new HeaderInput();
       
        Subject s;
        try {
            s = SecurityUtils.getSubject();
        } catch (Exception x) {
            s = null;
            LOG.error("Failure.", x);
        }
        Subject subject = s;
        List<Map<String, String>> p = plugins.stream().filter((PluginHandle t) -> {
            if (t.permissions == null || t.permissions.length == 0) {
                return true;
            }
            if (t.name.equals("login")) {
                if (subject != null && subject.isAuthenticated()) {
                    return false;
                }
            }
            if (subject != null && subject.isPermittedAll(t.permissions)) {
                return true;
            }
            return false;
        }).map((input1) -> {
            Map<String, String> map = new HashMap<>();
            if (name.equals(input1.name)) {
                map.put("active", String.valueOf(name.equals(input1.name)));
            }
            if (input1.separator != null) {
                map.put("separator", input1.separator);
            }
            map.put("name", input1.name);
            map.put("path", input1.path);
            map.put("glyphicon", input1.glyphicon);
            map.put("icon", input1.icon);
            return map;
        }).collect(Collectors.toList());

        headerData.put("plugins", p);
        headerData.put("user", user);
        headerData.put("cluster", cluster);
       

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
