package com.jivesoftware.os.upena.deployable.endpoints;

import com.jivesoftware.os.upena.deployable.profiler.visualize.VStrategies;
import com.jivesoftware.os.upena.deployable.region.ProfilerPluginRegion;
import com.jivesoftware.os.upena.deployable.region.ProfilerPluginRegion.ProfilerPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Singleton
@Path("/ui/profiler")
public class ProfilerPluginEndpoints {

    private final SoyService soyService;
    private final ProfilerPluginRegion pluginRegion;

    public ProfilerPluginEndpoints(@Context SoyService soyService,
        @Context ProfilerPluginRegion pluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response ring(@Context HttpServletRequest httpRequest) {
        String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
            new ProfilerPluginRegionInput("",
                800,
                VStrategies.ValueStrat.constant.name(),
                VStrategies.StackStrat.constant.name(),
                VStrategies.BarStrat.calledBy.name(),
                VStrategies.Colorings.heat.name(),
                VStrategies.StackOrder.ascending.name(),
                0,
                0));
        return Response.ok(rendered).build();
    }

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response action(@Context HttpServletRequest httpRequest,
        @FormParam("serviceName") @DefaultValue("") String serviceName,
        @FormParam("height") @DefaultValue("800") int height,
        @FormParam("valueStrategy") @DefaultValue("constant") String valueStrategy,
        @FormParam("stackStrategy") @DefaultValue("constant") String stackStrategy,
        @FormParam("barStrategy") @DefaultValue("calledBy") String barStrategy,
        @FormParam("coloring") @DefaultValue("heat") String coloring,
        @FormParam("stackOrder") @DefaultValue("ascending") String stackOrder,
        @FormParam("mouseX") @DefaultValue("0") int mouseX,
        @FormParam("mouseY") @DefaultValue("0") int mouseY) {
        String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion,
            new ProfilerPluginRegionInput(serviceName,
                height,
                valueStrategy,
                stackStrategy,
                barStrategy,
                coloring,
                stackOrder,
                mouseX,
                mouseY));
        return Response.ok(rendered).build();
    }
}
