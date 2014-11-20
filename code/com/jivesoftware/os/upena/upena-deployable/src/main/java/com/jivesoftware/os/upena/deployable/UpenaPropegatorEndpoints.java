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
package com.jivesoftware.os.upena.deployable;

import com.jivesoftware.os.jive.utils.jaxrs.util.ResponseHelper;
import com.jivesoftware.os.jive.utils.logger.MetricLogger;
import com.jivesoftware.os.jive.utils.logger.MetricLoggerFactory;
import java.io.File;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author jonathan.colt
 */
@Singleton
@Path("/upena/propegator")
public class UpenaPropegatorEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    @GET
    @Consumes("application/json")
    @Path("/deploy")
    public Response getInstanceConfig(@QueryParam("clusterName") String clusterName,
        @QueryParam("scpUser") String scpUser,
        @QueryParam("scpHost") String scpHost,
        @QueryParam("scpHome") String scpHome) {
        try {
            File pathToUpenaJar = new File(System.getProperty("user.dir"), "upena.jar");
            UpenaPropegator propegator = new UpenaPropegator(pathToUpenaJar.getAbsolutePath(),
                clusterName,
                "~/.ssh/id_rsa",
                scpUser,
                scpHost,
                scpHome
            );
            propegator.propegate();
            return Response.ok("Propegated upena to: " + scpHost + ":" + scpHome, MediaType.TEXT_PLAIN).build();
        } catch (Exception x) {
            LOG.warn("Failed to deploy upena to: " + scpHost + ":" + scpHome, x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to deploy upena to:" + scpHost + ":" + scpHome, x);
        }
    }
}
