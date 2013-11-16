package com.jivesoftware.os.upena.routing.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ConnectionDescriptorsRequestTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testConstruction() throws IOException {

        ConnectionDescriptorsRequest a = new ConnectionDescriptorsRequest("tenant", "instance", "to", "port");
        Assert.assertEquals(a.getTenantId(), "tenant");
        Assert.assertEquals(a.getInstanceId(), "instance");
        Assert.assertEquals(a.getConnectToServiceNamed(), "to");
        Assert.assertEquals(a.getPortName(), "port");

        String asString = mapper.writeValueAsString(a);
        ConnectionDescriptorsRequest b = mapper.readValue(asString, ConnectionDescriptorsRequest.class);

        Assert.assertEquals(a.getTenantId(), b.getTenantId());
        Assert.assertEquals(a.getInstanceId(), b.getInstanceId());
        Assert.assertEquals(a.getConnectToServiceNamed(), b.getConnectToServiceNamed());
        Assert.assertEquals(a.getPortName(), b.getPortName());

        Assert.assertEquals(a, b);
        Assert.assertEquals(a.hashCode(), b.hashCode());
        Assert.assertEquals(a.toString(), b.toString());

    }
}