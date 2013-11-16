package com.jivesoftware.os.upena.routing.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ConnectionDescriptorTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testConstruction() throws IOException {
        Map<String, String> properties = new HashMap<>();
        properties.put("a", "b");

        ConnectionDescriptor a = new ConnectionDescriptor("host", 1, properties);
        Assert.assertEquals(a.getHost(), "host");
        Assert.assertEquals(a.getPort(), 1);
        Assert.assertEquals(a.getProperties(), properties);

        String asString = mapper.writeValueAsString(a);
        ConnectionDescriptor b = mapper.readValue(asString, ConnectionDescriptor.class);

        Assert.assertEquals(a.getHost(), b.getHost());
        Assert.assertEquals(a.getPort(), b.getPort());
        Assert.assertEquals(a.getProperties(), b.getProperties());

        Assert.assertEquals(a, b);
        Assert.assertEquals(a.hashCode(), b.hashCode());
        Assert.assertEquals(a.toString(), b.toString());

    }
}