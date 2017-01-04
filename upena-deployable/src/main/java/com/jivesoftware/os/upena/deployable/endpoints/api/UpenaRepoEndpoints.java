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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.HeaderDecoration;
import com.jivesoftware.os.upena.deployable.UpenaAutoRelease;
import com.jivesoftware.os.upena.shared.PathToRepo;
import io.swagger.annotations.Api;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author jonathan.colt
 */
@Api(value = "Upena Maven Repo")@Singleton
@Path("/repo")
public class UpenaRepoEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final AmzaClusterName amzaClusterName;

    public static class AmzaClusterName {

        private final String name;

        public AmzaClusterName(String name) {
            this.name = name;
        }

    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final PathToRepo localPathToRepo;
    private final UpenaAutoRelease autoRelease;
    
    public UpenaRepoEndpoints(@Context AmzaClusterName amzaClusterName,
        @Context PathToRepo localPathToRepo,
        @Context UpenaAutoRelease autoRelease) {

        this.amzaClusterName = amzaClusterName;
        this.localPathToRepo = localPathToRepo;
        this.autoRelease = autoRelease;
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @GET
    @Path("/{subResources:.*}")
    public Object pull(@PathParam("subResources") String subResources) {
        File f = new File(localPathToRepo.get(), subResources);
        try {
            if (!f.exists()) {
                return HeaderDecoration.decorate(Response.status(Response.Status.NOT_FOUND)).build();
            }

            return (StreamingOutput) (OutputStream os) -> {
                try {

                    try {
                        byte[] buf = new byte[8192];
                        InputStream is = new FileInputStream(f);
                        int c = 0;
                        while ((c = is.read(buf, 0, buf.length)) > 0) {
                            os.write(buf, 0, c);
                            os.flush();
                        }
                        os.close();
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    throw new WebApplicationException(e);
                }
            };
        } catch (Exception x) {
            LOG.error("Failed to read " + f, x);
            return Response.serverError().build();
        }
    }

    @PUT
    @Path("/{subResources:.*}")
    public Response putFileInRepo(
        InputStream fileInputStream,
        @PathParam("subResources") String subResources) {

        File f = new File(localPathToRepo.get(), subResources);
        try {
            FileUtils.forceMkdir(f.getParentFile());
            saveFile(fileInputStream, f);
        } catch (Exception x) {
            LOG.error("Failed to write " + f, x);
            FileUtils.deleteQuietly(f);
            return Response.serverError().build();
        }

        if (f.getName().endsWith(".pom")) {
            autoRelease.uploaded(f);
        }

        return HeaderDecoration.decorate(Response.ok("Success")).build();
    }

    // save uploaded file to a defined location on the server
    private void saveFile(InputStream uploadedInputStream, File file) {
        try (OutputStream outpuStream = new FileOutputStream(file)) {
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                outpuStream.write(bytes, 0, read);
            }
            outpuStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
