package com.jivesoftware.os.upena.deployable.endpoints.ui;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.AuthPluginRegion;
import com.jivesoftware.os.upena.deployable.region.AuthPluginRegion.AuthInput;
import com.jivesoftware.os.upena.deployable.region.UnauthorizedPluginRegion;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

/**
 *
 */
@Singleton
@Path("/ui/auth")
public class AuthPluginEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SoyService soyService;
    private final AuthPluginRegion pluginRegion;
    private final UnauthorizedPluginRegion unauthorizedPluginRegion;

    public AuthPluginEndpoints(@Context SoyService soyService,
        @Context AuthPluginRegion pluginRegion,
        @Context UnauthorizedPluginRegion unauthorizedPluginRegion) {
        this.soyService = soyService;
        this.pluginRegion = pluginRegion;
        this.unauthorizedPluginRegion = unauthorizedPluginRegion;
    }

    @Path("/login")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getLogin(@Context HttpServletRequest httpRequest) {
        try {
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion, new AuthInput("", false));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("auth GET", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Path("/login")
    @POST
    @Produces(MediaType.TEXT_HTML)
    public Response postLogin(@FormParam("username") @DefaultValue("") String username, @Context HttpServletRequest httpRequest) {
        try {
            Object got = httpRequest.getAttribute("shiroLoginFailure");
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion, new AuthInput(username, (got == null ? false : true)));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("auth GET", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Path("/unauthorized")
    @POST
    @Produces(MediaType.TEXT_HTML)
    public Response postUnauthorized(@Context HttpServletRequest httpRequest) {
        try {
            LOG.info("httpRequest:"+httpRequest);
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), unauthorizedPluginRegion, new UnauthorizedPluginRegion.UnauthorizedInput());
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("auth GET", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Path("/unauthorized")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getUnauthorized(@Context HttpServletRequest httpRequest) {
        try {
            LOG.info("httpRequest:"+httpRequest);
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), unauthorizedPluginRegion, new UnauthorizedPluginRegion.UnauthorizedInput());
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("auth GET", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Path("/logout")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response logout(@Context HttpServletRequest httpRequest) {
        try {
            Subject subject = SecurityUtils.getSubject();
            subject.logout();
            String rendered = soyService.renderPlugin(httpRequest.getRemoteUser(), pluginRegion, new AuthInput("", false));
            return Response.ok(rendered).build();
        } catch (Exception e) {
            LOG.error("auth GET", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
