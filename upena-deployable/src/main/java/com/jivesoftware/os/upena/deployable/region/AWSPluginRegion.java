package com.jivesoftware.os.upena.deployable.region;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthResult;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetDescription;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealth;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealthDescription;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.aws.AWSClientFactory;
import com.jivesoftware.os.upena.deployable.region.AWSPluginRegion.AWSPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
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
    private final AWSClientFactory awsClientFactory;

    public AWSPluginRegion(String template,
        SoyRenderer renderer,
        AWSClientFactory awsClientFactory
    ) {
        this.template = template;
        this.renderer = renderer;
        this.awsClientFactory = awsClientFactory;
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

                AmazonElasticLoadBalancingClient elbc = awsClientFactory.getELBC("aws-plugin");
                DescribeLoadBalancersRequest describeLoadBalancersRequest = new DescribeLoadBalancersRequest();
                DescribeLoadBalancersResult loadBalancersResult = elbc.describeLoadBalancers(describeLoadBalancersRequest);
                List<Map<String, Object>> list = Lists.newArrayList();
                if (loadBalancersResult != null && loadBalancersResult.getLoadBalancers() != null) {
                    for (LoadBalancer loadBalancerDescription : loadBalancersResult.getLoadBalancers()) {
                        Map<String, Object> item = Maps.newHashMap();
                        item.put("toString", prettyString(loadBalancerDescription.toString()));
                        list.add(item);
                    }
                }

                DescribeTargetGroupsRequest describeTargetGroupsRequest = new DescribeTargetGroupsRequest();
                DescribeTargetGroupsResult describeTargetGroups = elbc.describeTargetGroups(describeTargetGroupsRequest);
                if (describeTargetGroups != null && describeTargetGroups.getTargetGroups() != null) {
                    for (TargetGroup targetGroup : describeTargetGroups.getTargetGroups()) {
                        Map<String, Object> item = Maps.newHashMap();
                        item.put("toString", prettyString(targetGroup.toString()));
                        list.add(item);

                        DescribeTargetHealthRequest describeTargetHealthRequest = new DescribeTargetHealthRequest();
                        describeTargetHealthRequest.setTargetGroupArn(targetGroup.getTargetGroupArn());
                        DescribeTargetHealthResult describeTargetHealth = elbc.describeTargetHealth(describeTargetHealthRequest);
                        if (describeTargetHealth != null && describeTargetHealth.getTargetHealthDescriptions() != null) {
                            for (TargetHealthDescription targetHealthDescription : describeTargetHealth.getTargetHealthDescriptions()) {
                                //String healthCheckPort = targetHealthDescription.getHealthCheckPort();
                                TargetDescription targetDescription = targetHealthDescription.getTarget();
                                item = Maps.newHashMap();
                                item.put("toString", prettyString(targetDescription.toString()));
                                list.add(item);

                                TargetHealth targetHealth = targetHealthDescription.getTargetHealth();

                                item = Maps.newHashMap();
                                item.put("toString", prettyString(targetHealth.toString()));
                                list.add(item);
                            }
                        }

                    }
                }

                data.put("loadBalancers", list);

            } else if (input.action.equals("instances")) {
                AmazonEC2Client ec2 = awsClientFactory.getEC2("aws-plugin");

                DescribeInstanceStatusResult describeInstanceStatus = ec2.describeInstanceStatus();
                List<Map<String, Object>> list = Lists.newArrayList();
                for (InstanceStatus instanceStatus : describeInstanceStatus.getInstanceStatuses()) {
                    Map<String, Object> item = Maps.newHashMap();
                    item.put("toString", prettyString(instanceStatus.toString()));
                    list.add(item);
                }
                data.put("instances", list);
            } else if (input.action.equals("volumes")) {
                AmazonEC2Client ec2 = awsClientFactory.getEC2("aws-plugin");
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
}
