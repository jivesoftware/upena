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
package com.jivesoftware.os.upena.uba.service.endpoints;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import com.jivesoftware.os.uba.shared.DeployableUpload;
import com.jivesoftware.os.upena.uba.service.UbaService;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/uba")
public class UbaServiceRestEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final UbaService ubaService;

    public UbaServiceRestEndpoints(@Context UbaService ubaService) {
        this.ubaService = ubaService;
    }

    @POST
    @Path("/upload")
    @Consumes("application/json")
    public Response upload(@Context SecurityContext sc, DeployableUpload deploy) {
        try {
            LOG.info("Uploading instanceIds:" + deploy.instanceIds + " version:" + deploy.version);
            //ubaService.upload(deploy.instanceIds, deploy.version, deploy.deployableFileBytes, deploy.extension);
            LOG.info("Uploaded version:" + deploy.version + " for instanceids:" + deploy.instanceIds);
            return ResponseHelper.INSTANCE.jsonResponse("Successfuly uploaded version:" + deploy.version);
        } catch (Exception x) {
            LOG.error("Failure while uploading instanceids:" + deploy.instanceIds + " version:" + deploy.version);
            return ResponseHelper.INSTANCE.errorResponse("Failed to upload artifact for instances:"
                    + deploy.instanceIds + " to version: " + deploy.version, x);
        }
    }

    /**
     * Get a report of what state all the service are in.
     *
     * @return
     * @throws Exception
     */
    @GET
    @Path("/report")
    public Response report(@Context SecurityContext sc) throws Exception {
        try {
            LOG.info("Getting report");
            return ResponseHelper.INSTANCE.jsonResponse(ubaService.report());
        } catch (Exception x) {
            LOG.error("Failure while getting report.", x);
            return ResponseHelper.INSTANCE.errorResponse("Failure while getting report.", x);
        }
    }
}
