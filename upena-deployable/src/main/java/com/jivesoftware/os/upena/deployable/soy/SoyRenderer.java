package com.jivesoftware.os.upena.deployable.soy;

import com.google.common.base.Preconditions;
import com.google.template.soy.tofu.SoyTofu;

import java.util.Map;

/**
 *
 */
public class SoyRenderer {

    private final SoyTofu tofu;
    private final SoyDataUtils soyDataUtils;

    public SoyRenderer(SoyTofu tofu, SoyDataUtils soyDataUtils) {
        this.tofu = tofu;
        this.soyDataUtils = soyDataUtils;
    }

    public String render(String template, Map<String, ?> data) {
        Preconditions.checkArgument(template != null && !template.isEmpty(), "argument is null or empty [template]");
        Preconditions.checkNotNull(data, "argument is null [data]");

        SoyTofu.Renderer renderer = tofu.newRenderer(template);
        if (renderer == null) {
            throw new IllegalArgumentException("No renderer found for template:" + template);
        }

        renderer.setData(soyDataUtils.toSoyCompatibleMap(data));

        return renderer.render();
    }

}
