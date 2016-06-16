package com.jivesoftware.os.upena.deployable.region;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.AWSPluginRegion.AWSPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 */
// soy.page.upenaRingPluginRegion
public class AWSPluginRegion implements PageRegion<AWSPluginRegionInput> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;
    private final String region;
    private final String roleArn;
    private final String acccessKey;
    private final String secretKey;

    public AWSPluginRegion(String template,
        SoyRenderer renderer,
        UpenaStore upenaStore,
        String region,
        String roleArn,
        String acccessKey,
        String secretKey
    ) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
        this.region = region;
        this.roleArn = roleArn;
        this.acccessKey = acccessKey;
        this.secretKey = secretKey;
    }

    @Override
    public String getRootPath() {
        return "/ui/aws";
    }

    public static class AWSPluginRegionInput implements PluginInput {

        final String action;

        public AWSPluginRegionInput(String action) {
            this.action = action;
        }

        @Override
        public String name() {
            return "AWS";
        }

    }

    @Override
    public String render(String user, AWSPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();
        data.put("action", input.action);
        try {

            if (input.action.equals("loadBalancers")) {
                AWSCredentials credentials = (roleArn != null) ? getFederatedSession(roleArn, acccessKey, secretKey) : getBasicSession(acccessKey, secretKey);
                AmazonElasticLoadBalancingClient elbc = new AmazonElasticLoadBalancingClient(credentials);
                DescribeLoadBalancersResult loadBalancersResult = elbc.describeLoadBalancers();

                List<Map<String, Object>> list = Lists.newArrayList();
                for (LoadBalancerDescription loadBalancerDescription : loadBalancersResult.getLoadBalancerDescriptions()) {
                    Map<String, Object> item = Maps.newHashMap();
                    item.put("toString", prettyString(loadBalancerDescription.toString()));
                    list.add(item);
                }
                data.put("loadBalancers", list);
            } else if (input.action.equals("instances")) {
                AWSCredentials credentials = (roleArn != null) ? getFederatedSession(roleArn, acccessKey, secretKey) : getBasicSession(acccessKey, secretKey);
                AmazonEC2Client ec2 = getClientForAccount(credentials, Regions.fromName(this.region));

                DescribeInstanceStatusResult describeInstanceStatus = ec2.describeInstanceStatus();
                List<Map<String, Object>> list = Lists.newArrayList();
                for (InstanceStatus instanceStatus : describeInstanceStatus.getInstanceStatuses()) {
                    Map<String, Object> item = Maps.newHashMap();
                    item.put("toString", prettyString(instanceStatus.toString()));
                    list.add(item);
                }
                data.put("instances", list);
            } else if (input.action.equals("volumes")) {
                AWSCredentials credentials = (roleArn != null) ? getFederatedSession(roleArn, acccessKey, secretKey) : getBasicSession(acccessKey, secretKey);
                AmazonEC2Client ec2 = getClientForAccount(credentials, Regions.fromName(this.region));
                DescribeVolumesResult describeVolumes = ec2.describeVolumes();
                List<Map<String, Object>> list = Lists.newArrayList();
                for (Volume volume : describeVolumes.getVolumes()) {
                    Map<String, Object> item = Maps.newHashMap();
                    item.put("toString", prettyString(volume.toString()));
                    list.add(item);
                }
                data.put("volumes", list);
            }

        } catch (Exception x) {
            log.error("Unable to retrieve data", x);
            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
            data.put("message", "Error while trying to " + input.action + "\n" + trace);

        }
        return renderer.render(template, data);
    }

    private final String prettyString(String string) throws IOException {
        string = string.replaceAll(",", ",\n");
        return string;
    }

    @Override
    public String getTitle() {
        return "AWS";
    }

    private static AWSCredentials getBasicSession(String acccessKey,
        String secretKey) {
        return new BasicAWSCredentials(acccessKey, secretKey);
    }

    private static AWSCredentials getFederatedSession(String roleArn, String acccessKey, String secretKey) {
        AWSSecurityTokenServiceClient stsClient = new AWSSecurityTokenServiceClient(new BasicAWSCredentials(acccessKey, secretKey));

        AssumeRoleRequest assumeRequest = new AssumeRoleRequest()
            .withRoleArn(roleArn)
            .withDurationSeconds(3600)
            .withRoleSessionName("upena");  // TODO expose ?

        AssumeRoleResult assumeResult = stsClient.assumeRole(assumeRequest);

        BasicSessionCredentials temporaryCredentials = new BasicSessionCredentials(
            assumeResult.getCredentials()
            .getAccessKeyId(), assumeResult.getCredentials().getSecretAccessKey(),
            assumeResult.getCredentials().getSessionToken());
        return temporaryCredentials;
    }

    private static AmazonEC2Client getClientForAccount(AWSCredentials credentials, Regions regions) {

        AmazonEC2Client amazonEC2Client = new AmazonEC2Client(credentials);
        amazonEC2Client.setRegion(com.amazonaws.regions.Region.getRegion(regions));

        return amazonEC2Client;
    }

}
