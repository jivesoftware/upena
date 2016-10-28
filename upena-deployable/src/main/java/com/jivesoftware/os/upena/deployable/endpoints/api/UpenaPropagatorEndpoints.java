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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

/**
 *
 * @author jonathan.colt
 */
@Singleton
@Path("/propagator")
public class UpenaPropagatorEndpoints {

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/download")
    public StreamingOutput pull() throws Exception {
        return (OutputStream os) -> {
            try {
                File f = new File(System.getProperty("user.dir"), "upena.jar");

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
    }

}
