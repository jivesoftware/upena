/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.upena.deployable.profiler.server.endpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.upena.deployable.profiler.sample.LatentSample;
import java.io.IOException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/profile")
public class PerfServiceEndpoint {

    static ObjectMapper mapper = new ObjectMapper();

    private final PerfService perfService;

    public PerfServiceEndpoint(@Context PerfService perfService) {
        this.perfService = perfService;
    }

    @POST
    @Path("/latents")
    public Response ingressLatents(String latents) throws IOException {
        boolean enabled = perfService.getCallDepthStack().call(mapper.readValue(latents, LatentSample.class));
        return Response.ok(String.valueOf(enabled), MediaType.TEXT_PLAIN_TYPE).build();
    }

    @Path("/render")
    @POST
    @Produces("image/png")
    public Response render() throws IOException {
        /*
         IImage i = UV.toImage(new VPan(new VString("hello"), 300, 300));

         BufferedImage image = (BufferedImage) i.data(0);

         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ImageIO.write(image, "png", baos);
         byte[] imageData = baos.toByteArray();

         // uncomment line below to send non-streamed
         // return Response.ok(imageData).build();
         //uncomment line below to send streamed
         return Response.ok(new ByteArrayInputStream(imageData)).build();
         */
        return null;
    }
}
