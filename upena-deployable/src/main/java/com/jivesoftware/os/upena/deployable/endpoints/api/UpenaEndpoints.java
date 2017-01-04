/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.upena.deployable.endpoints.api;

import com.jivesoftware.os.upena.deployable.UpenaHealth;
import com.jivesoftware.os.upena.deployable.soy.SoyService;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 *
 * @author jonathan.colt
 */
@Singleton
@Path("/")
public class UpenaEndpoints {

    private final SoyService soyService;
    private final UpenaHealth upenaHealth;

    public UpenaEndpoints(@Context SoyService soyService,
        @Context UpenaHealth upenaHealth) {
        this.soyService = soyService;
        this.upenaHealth = upenaHealth;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getUIRedirect(@Context HttpServletRequest httpRequest,
        @Context UriInfo uriInfo) throws Exception {
        return Response.temporaryRedirect(URI.create("/ui")).build();
    }

    @Path("/ui")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response get(@Context HttpServletRequest httpRequest,
        @Context UriInfo uriInfo) throws Exception {
        String rendered = soyService.render(httpRequest.getRemoteUser(), null);
        return Response.ok(rendered).build();
    }

    @Path("/ui/overview")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response overview(@Context HttpServletRequest httpRequest,
        @Context UriInfo uriInfo) throws Exception {
        String rendered = soyService.renderOverview(httpRequest.getRemoteUser());
        return Response.ok(rendered).build();
    }

    @Path("/ui/healthGradient")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response healthGradient(@Context HttpServletRequest httpRequest,
        @Context UriInfo uriInfo) throws Exception {
        return Response.ok(upenaHealth.healthGradient()).build();
    }

}
