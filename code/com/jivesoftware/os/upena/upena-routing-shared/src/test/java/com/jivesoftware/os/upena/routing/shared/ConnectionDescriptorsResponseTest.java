package com.jivesoftware.os.upena.routing.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ConnectionDescriptorsResponseTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testConstruction() throws IOException {
        Map<String, String> properties = new HashMap<>();
        properties.put("a", "b");
        List<ConnectionDescriptor> connections = new ArrayList<>();
        connections.add(new ConnectionDescriptor("host", 1, properties));
        ConnectionDescriptorsResponse a = new ConnectionDescriptorsResponse(1, Arrays.asList("message"), "user",
                connections, new HashSet(Arrays.asList("user1")));

        Assert.assertEquals(a.getConnections(), connections);
        Assert.assertEquals(a.getMessages(), Arrays.asList("message"));
        Assert.assertEquals(a.getReturnCode(), 1);
        Assert.assertEquals(a.getUserId(), "user");
        Assert.assertEquals(a.getValidUserIds(), new HashSet(Arrays.asList("user1")));

        String asString = mapper.writeValueAsString(a);
        ConnectionDescriptorsResponse b = mapper.readValue(asString, ConnectionDescriptorsResponse.class);

        Assert.assertEquals(a.getConnections(), b.getConnections());
        Assert.assertEquals(a.getMessages(), b.getMessages());
        Assert.assertEquals(a.getReturnCode(), b.getReturnCode());
        Assert.assertEquals(a.getUserId(), b.getUserId());
        Assert.assertEquals(a.getValidUserIds(), b.getValidUserIds());

        Assert.assertEquals(a, b);
        Assert.assertEquals(a.hashCode(), b.hashCode());
        Assert.assertEquals(a.toString(), b.toString());

    }
}