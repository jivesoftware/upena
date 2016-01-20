/*
 * Copyright 2013 jonathan.colt.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package colt.nicity.performance.latent.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

public class ApacheHttpClient {

    public static final String CONTENT_TYPE_HEADER_NAME = "Content-Type";
    public static final String APPLICATION_JSON_CONTENT_TYPE = "application/json";
    private final org.apache.commons.httpclient.HttpClient client;
    private final Map<String, String> headersForEveryRequest;
    private static final int JSON_POST_LOG_LENGTH_LIMIT = 2048;

    public ApacheHttpClient(org.apache.commons.httpclient.HttpClient client,
        Map<String, String> headersForEveryRequest) {
        this.client = client;
        this.headersForEveryRequest = headersForEveryRequest;
    }

    public HttpResponse get(String path) {
        GetMethod get = new GetMethod(path);
        try {
            return execute(get);
        } catch (Exception e) {
            throw new RuntimeException("Error executing GET request to: " + client.getHostConfiguration().getHostURL()
                + " path: " + path, e);
        }
    }

    public HttpResponse postJson(String path, String postJsonBody) {
        try {
            PostMethod post = new PostMethod(path);
            post.setRequestEntity(new StringRequestEntity(postJsonBody, APPLICATION_JSON_CONTENT_TYPE, "UTF-8"));
            post.setRequestHeader(CONTENT_TYPE_HEADER_NAME, APPLICATION_JSON_CONTENT_TYPE);
            return execute(post);
        } catch (Exception e) {
            String trimmedPostBody = (postJsonBody.length() > JSON_POST_LOG_LENGTH_LIMIT)
                ? postJsonBody.substring(0, JSON_POST_LOG_LENGTH_LIMIT)
                : postJsonBody;
            throw new RuntimeException("Error executing POST request to: "
                + client.getHostConfiguration().getHostURL() + " path: " + path + " JSON body: " + trimmedPostBody, e);
        }
    }

    // package scoped so OauthHttpClient can use
    HttpResponse execute(HttpMethod method) throws IOException {

        applyHeadersCommonToAllRequests(method);

        byte[] responseBody;
        StatusLine statusLine;
        try {
            client.executeMethod(method);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream responseBodyAsStream = method.getResponseBodyAsStream();
            if (responseBodyAsStream != null) {
                copyLarge(responseBodyAsStream, outputStream, new byte[1024 * 4]);
            }

            responseBody = outputStream.toByteArray();
            statusLine = method.getStatusLine();

            // Catch exception and log error here? or silently fail?
        } finally {
            method.releaseConnection();
        }

        return new HttpResponse(statusLine.getStatusCode(), statusLine.getReasonPhrase(), responseBody);
    }

    public static long copyLarge(InputStream input, OutputStream output, byte[] buffer)
        throws IOException {
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    private void applyHeadersCommonToAllRequests(HttpMethod method) {
        for (Map.Entry<String, String> headerEntry : headersForEveryRequest.entrySet()) {
            method.addRequestHeader(headerEntry.getKey(), headerEntry.getValue());
        }
    }

    void setState(HttpState state) {
        client.setState(state);
    }

    void setHostConfiguration(HostConfiguration hostConfiguration) {
        client.setHostConfiguration(hostConfiguration);
    }
}
