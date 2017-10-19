package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.testng.annotations.Test;

import java.util.Map;

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

    @Test
    public void testInstanceJson() throws JsonProcessingException {
        ClusterKey ck = new ClusterKey("foock");
        HostKey hk = new HostKey("foohk");
        ServiceKey sk = new ServiceKey("foosk");
        ReleaseGroupKey rgk = new ReleaseGroupKey("foorgk");
        int instanceId = 123;
        boolean enabled = false;
        boolean locked = false;
        String publicKey = "pubkey";
        long restartTimestampGMTMillis = 12345;
        Map<String, Instance.Port> ports = null;

        Instance k = new Instance(ck,
            hk,
            sk,
            rgk,
            instanceId,
            enabled,
            locked,
            publicKey,
            restartTimestampGMTMillis,
            ports);
        String json = mapper.writeValueAsString(k);

        System.out.println(json);
        // {"objectType":"instance","clusterKey":{"objectType":"branchKey","key":"foock"},"hostKey":{"objectType":"hostKey","key":"foohk"},"serviceKey":{"objectType":"serviceKey","key":"foosk"},"releaseGroupKey":{"objectType":"userKey","key":"foorgk"},"instanceId":123,"enabled":false,"locked":false,"publicKey":"pubkey","restartTimestampGMTMillis":12345,"ports":{}}
    }

}
