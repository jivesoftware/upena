package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.testng.annotations.Test;

public class ApiKeyTests {

    ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    public void testClusterKeyJson() throws JsonProcessingException {
        ClusterKey k = new ClusterKey("foo");
        String json = mapper.writeValueAsString(k);

        System.out.println(json);
        // {"objectType":"branchKey","key":"foo"}
    }

    @Test
    public void testHostKeyJson() throws JsonProcessingException {
        HostKey k = new HostKey("foo");
        String json = mapper.writeValueAsString(k);

        System.out.println(json);
        // {"objectType":"hostKey","key":"foo"}
    }

    @Test
    public void testInstanceKeyJson() throws JsonProcessingException {
        InstanceKey k = new InstanceKey("foo");
        String json = mapper.writeValueAsString(k);

        System.out.println(json);
        // {"objectType":"instanceKey","key":"foo"}
    }

    @Test
    public void testReleaseGroupKeyJson() throws JsonProcessingException {
        ReleaseGroupKey k = new ReleaseGroupKey("foo");
        String json = mapper.writeValueAsString(k);

        System.out.println(json);
        // {"objectType":"userKey","key":"foo"}
    }

    @Test
    public void testServiceKeyJson() throws JsonProcessingException {
        ServiceKey k = new ServiceKey("foo");
        String json = mapper.writeValueAsString(k);

        System.out.println(json);
        // {"objectType":"serviceKey","key":"foo"}
    }

    @Test
    public void testTenantKeyJson() throws JsonProcessingException {
        TenantKey k = new TenantKey("foo");
        String json = mapper.writeValueAsString(k);

        System.out.println(json);
        // {"objectType":"tenantKey","key":"foo"}
    }

}
