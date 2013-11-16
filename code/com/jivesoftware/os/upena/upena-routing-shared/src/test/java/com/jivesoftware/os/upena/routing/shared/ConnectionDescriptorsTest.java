package com.jivesoftware.os.upena.routing.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ConnectionDescriptorsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testConstruction() throws IOException {
        Map<String, String> properties = new HashMap<>();
        properties.put("a", "b");

        ConnectionDescriptor cd1 = new ConnectionDescriptor("host1", 1, properties);
        ConnectionDescriptor cd2 = new ConnectionDescriptor("host2", 2, properties);

        ConnectionDescriptors a = new ConnectionDescriptors(3, Arrays.asList(cd1, cd2));

        Assert.assertEquals(a.getTimestamp(), 3);
        Assert.assertEquals(a.getConnectionDescriptors(), Arrays.asList(cd1, cd2));

        String asString = mapper.writeValueAsString(a);
        ConnectionDescriptors b = mapper.readValue(asString, ConnectionDescriptors.class);

        Assert.assertEquals(a.getTimestamp(), b.getTimestamp());
        Assert.assertEquals(a.getConnectionDescriptors(), b.getConnectionDescriptors());


        Assert.assertEquals(a, b);
        Assert.assertEquals(a.hashCode(), b.hashCode());
        Assert.assertEquals(a.toString(), b.toString());
    }
}