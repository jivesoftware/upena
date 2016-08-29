package com.jivesoftware.os.upena.deployable;

import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.AddTagsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DeregisterTargetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTagsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTagsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupAttributesRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupAttributesResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthResult;
import com.amazonaws.services.elasticloadbalancingv2.model.Matcher;
import com.amazonaws.services.elasticloadbalancingv2.model.ModifyTargetGroupAttributesRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.ModifyTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.ProtocolEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.RegisterTargetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.Tag;
import com.amazonaws.services.elasticloadbalancingv2.model.TagDescription;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetDescription;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroupAttribute;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealth;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealthDescription;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.aws.AWSClientFactory;
import com.jivesoftware.os.upena.service.JenkinsHash;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.ReleaseGroupPropertyKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;

/**
 *
 * @author jonathan.colt
 */
public class UpenaAWSLoadBalancerNanny {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String vpcId;
    private final UpenaStore upenaStore;
    private final HostKey selfHostKey;
    private final AWSClientFactory awsClientFactory;

    public UpenaAWSLoadBalancerNanny(String vpcId, UpenaStore upenaStore, HostKey selfHostKey, AWSClientFactory awsClientFactory) {
        this.vpcId = vpcId;
        this.upenaStore = upenaStore;
        this.selfHostKey = selfHostKey;
        this.awsClientFactory = awsClientFactory;
    }

    public void ensureSelf() throws Exception {

        if (vpcId == null) {
            LOG.info("No vpcId specified.");
            return;
        }

        if (!awsClientFactory.enabled()) {
            return;
        }

        ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(
            new InstanceFilter(null, selfHostKey, null, null, null, 0, 100_000));

        Set<String> missingTargetGroups = new HashSet<>();
        ListMultimap<String, Instance> targetGroupInstances = ArrayListMultimap.create();
        Map<String, ReleaseGroupKey> targetGroupRelease = Maps.newHashMap();
        Map<String, TargetGroup> targetGroups = Maps.newHashMap();
        Map<String, String> targetGroupIdToName = Maps.newHashMap();
        AmazonElasticLoadBalancingClient elbc = awsClientFactory.getELBC("lbNanny");

        figureOutTargetGroups(found, missingTargetGroups, targetGroupInstances, targetGroupRelease, targetGroupIdToName);
        ensureTargetGroups(elbc, targetGroups, missingTargetGroups, targetGroupRelease, targetGroupIdToName);
        ensureTargetsRegistered(targetGroupInstances, targetGroups, elbc);

    }

    private void figureOutTargetGroups(ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> found,
        Set<String> desiredTargetGroups,
        ListMultimap<String, Instance> targetGroupInstances,
        Map<String, ReleaseGroupKey> targetGroupRelease,
        Map<String, String> targetGroupIdToName) throws Exception {
        JenkinsHash jenkinsHash = new JenkinsHash();

        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entry : found.entrySet()) {
            if (entry.getValue().getTombstoned()) {
                continue;
            }
            Instance instance = entry.getValue().getValue();

            Cluster cluster = upenaStore.clusters.get(instance.clusterKey);
            Service service = upenaStore.services.get(instance.serviceKey);
            ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(instance.releaseGroupKey);

            String targetGroupId = instance.clusterKey.getKey() + "|" + instance.serviceKey.getKey() + "|" + instance.releaseGroupKey.getKey();
            String targetGroupKey = "tg-" + String.valueOf(Math.abs(jenkinsHash.hash(targetGroupId.getBytes(StandardCharsets.UTF_8), 0)));

            String targetGroupName = cluster.name + "_" + service.name + "_" + releaseGroup.name;
            targetGroupIdToName.put(targetGroupKey, targetGroupName);
            desiredTargetGroups.add(targetGroupKey);

            targetGroupInstances.put(targetGroupKey, instance);
            targetGroupRelease.put(targetGroupKey, instance.releaseGroupKey);

        }
    }

    private void ensureTargetGroups(AmazonElasticLoadBalancingClient elbc,
        Map<String, TargetGroup> targetGroups,
        Set<String> missingTargetGroups,
        Map<String, ReleaseGroupKey> targetGroupRelease,
        Map<String, String> targetGroupIdToName) throws Exception {

        DescribeTargetGroupsRequest describeTargetGroupsRequest = new DescribeTargetGroupsRequest();
        DescribeTargetGroupsResult describeTargetGroups = elbc.describeTargetGroups(describeTargetGroupsRequest);
        if (describeTargetGroups != null && describeTargetGroups.getTargetGroups() != null) {
            for (TargetGroup targetGroup : describeTargetGroups.getTargetGroups()) {
                targetGroups.put(targetGroup.getTargetGroupName(), targetGroup);
                String targetGroupName = targetGroup.getTargetGroupName();
                missingTargetGroups.remove(targetGroupName);

                ReleaseGroupKey releaseGroupKey = targetGroupRelease.get(targetGroupName);
                if (releaseGroupKey == null) {
                    // Ignore unknown
                    continue;
                }
                ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(releaseGroupKey);

                boolean[] modified = {false};

                ModifyTargetGroupRequest modifyTargetGroupRequest = new ModifyTargetGroupRequest();
                updateIfChanged(
                    targetGroup.getMatcher() != null ? targetGroup.getMatcher().getHttpCode() : ReleaseGroupPropertyKey.matcher.getDefaultValue(),
                    releaseGroup.properties,
                    ReleaseGroupPropertyKey.matcher,
                    modified,
                    (newValue) -> {
                        modifyTargetGroupRequest.setMatcher(new Matcher().withHttpCode(newValue));
                    }
                );

                updateIfChanged(
                    targetGroup.getHealthCheckIntervalSeconds() == null ? null : targetGroup.getHealthCheckIntervalSeconds().toString(),
                    releaseGroup.properties,
                    ReleaseGroupPropertyKey.healthCheckIntervalSeconds,
                    modified,
                    (newValue) -> {
                        modifyTargetGroupRequest.setHealthCheckIntervalSeconds(Integer.parseInt(newValue));
                    }
                );

                updateIfChanged(
                    targetGroup.getHealthCheckPath(),
                    releaseGroup.properties,
                    ReleaseGroupPropertyKey.healthCheckPath,
                    modified,
                    (newValue) -> {
                        modifyTargetGroupRequest.setHealthCheckPath(newValue);
                    }
                );

                updateIfChanged(
                    targetGroup.getHealthCheckProtocol(),
                    releaseGroup.properties,
                    ReleaseGroupPropertyKey.healthCheckProtocol,
                    modified,
                    (newValue) -> {
                        modifyTargetGroupRequest.setHealthCheckProtocol(ProtocolEnum.valueOf(newValue));
                    }
                );

                updateIfChanged(
                    targetGroup.getHealthCheckTimeoutSeconds() == null ? null : targetGroup.getHealthCheckTimeoutSeconds().toString(),
                    releaseGroup.properties,
                    ReleaseGroupPropertyKey.healthCheckTimeoutSeconds,
                    modified,
                    (newValue) -> {
                        modifyTargetGroupRequest.setHealthCheckTimeoutSeconds(Integer.parseInt(newValue));
                    }
                );

                updateIfChanged(
                    targetGroup.getHealthyThresholdCount() == null ? null : targetGroup.getHealthyThresholdCount().toString(),
                    releaseGroup.properties,
                    ReleaseGroupPropertyKey.healthyThresholdCount,
                    modified,
                    (newValue) -> {
                        modifyTargetGroupRequest.setHealthyThresholdCount(Integer.parseInt(newValue));
                    }
                );

                updateIfChanged(
                    targetGroup.getUnhealthyThresholdCount() == null ? null : targetGroup.getUnhealthyThresholdCount().toString(),
                    releaseGroup.properties,
                    ReleaseGroupPropertyKey.unhealthyThresholdCount,
                    modified,
                    (newValue) -> {
                        modifyTargetGroupRequest.setUnhealthyThresholdCount(Integer.parseInt(newValue));
                    }
                );

                if (modified[0]) {
                    LOG.info("Modifying {}", modifyTargetGroupRequest);
                    elbc.modifyTargetGroup(modifyTargetGroupRequest);
                }

                DescribeTargetGroupAttributesRequest describeTargetGroupAttributesRequest = new DescribeTargetGroupAttributesRequest();
                describeTargetGroupAttributesRequest.setTargetGroupArn(targetGroup.getTargetGroupArn());

                List<TargetGroupAttribute> updatedAttributes = Lists.newArrayList();
                DescribeTargetGroupAttributesResult describeTargetGroupAttributes = elbc.describeTargetGroupAttributes(describeTargetGroupAttributesRequest);
                if (describeTargetGroupAttributes != null && describeTargetGroupAttributes.getAttributes() != null) {
                    for (TargetGroupAttribute attribute : describeTargetGroupAttributes.getAttributes()) {
                        ReleaseGroupPropertyKey releaseGroupPropertyKey = ReleaseGroupPropertyKey.forKey(attribute.getKey());
                        if (releaseGroupPropertyKey != null) {
                            String value = releaseGroup.properties.getOrDefault(releaseGroupPropertyKey.key(), releaseGroupPropertyKey.getDefaultValue());
                            if (!value.equals(attribute.getValue())) {
                                updatedAttributes.add(new TargetGroupAttribute().withKey(releaseGroupPropertyKey.key()).withValue(value));
                            }
                        }
                    }
                }

                if (!updatedAttributes.isEmpty()) {
                    ModifyTargetGroupAttributesRequest modifyTargetGroupAttributesRequest = new ModifyTargetGroupAttributesRequest();
                    modifyTargetGroupAttributesRequest.setTargetGroupArn(targetGroup.getTargetGroupArn());
                    modifyTargetGroupAttributesRequest.setAttributes(updatedAttributes);
                    LOG.info("Modifying {}", modifyTargetGroupAttributesRequest);
                    elbc.modifyTargetGroupAttributes(modifyTargetGroupAttributesRequest);
                }

                boolean foundUpenaName = false;
                DescribeTagsResult describeTagsResult = elbc.describeTags(new DescribeTagsRequest().withResourceArns(targetGroup.getTargetGroupArn()));
                if (describeTagsResult != null && describeTagsResult.getTagDescriptions() != null) {

                    for (TagDescription tagDescription : describeTagsResult.getTagDescriptions()) {
                        if (tagDescription.getTags() != null) {
                            for (Tag tag : tagDescription.getTags()) {
                                if (tag.getKey().equals("upena_name")) {
                                    if (tag.getValue().equals(targetGroupIdToName.get(targetGroup.getTargetGroupName()))) {
                                        foundUpenaName = true;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }

                if (!foundUpenaName) {
                    AddTagsRequest addTag = new AddTagsRequest()
                        .withResourceArns(targetGroup.getTargetGroupArn())
                        .withTags(new Tag().withKey("upena_name").withValue(targetGroupIdToName.get(targetGroup.getTargetGroupName())));
                    LOG.info("Modifying {}", addTag);
                    elbc.addTags(addTag);
                }
            }
        }

        if (!missingTargetGroups.isEmpty()) {

            for (String missingTargetGroup : missingTargetGroups) {

                ReleaseGroupKey releaseGroupKey = targetGroupRelease.get(missingTargetGroup);
                ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(releaseGroupKey);

                boolean[] modified = {false};

                CreateTargetGroupRequest createTargetGroupRequest = new CreateTargetGroupRequest();
                createTargetGroupRequest.setName(missingTargetGroup);
                createTargetGroupRequest.setVpcId(vpcId);
                createTargetGroupRequest.setPort(1);

                createTargetGroupRequest.setMatcher(
                    new Matcher().withHttpCode(getOrDefault(releaseGroup.properties, ReleaseGroupPropertyKey.matcher)));
                createTargetGroupRequest.setProtocol(
                    ProtocolEnum.valueOf(getOrDefault(releaseGroup.properties, ReleaseGroupPropertyKey.protocol).toUpperCase()));
                createTargetGroupRequest.setHealthCheckPath(
                    getOrDefault(releaseGroup.properties, ReleaseGroupPropertyKey.healthCheckPath));
                //createTargetGroupRequest.setHealthCheckPort(Integer.MIN_VALUE);
                createTargetGroupRequest.setHealthCheckProtocol(
                    ProtocolEnum.valueOf(getOrDefault(releaseGroup.properties, ReleaseGroupPropertyKey.healthCheckProtocol).toUpperCase()));
                createTargetGroupRequest.setHealthCheckTimeoutSeconds(
                    Integer.parseInt(getOrDefault(releaseGroup.properties, ReleaseGroupPropertyKey.healthCheckTimeoutSeconds)));
                createTargetGroupRequest.setHealthyThresholdCount(
                    Integer.parseInt(getOrDefault(releaseGroup.properties, ReleaseGroupPropertyKey.healthyThresholdCount)));
                createTargetGroupRequest.setUnhealthyThresholdCount(
                    Integer.parseInt(getOrDefault(releaseGroup.properties, ReleaseGroupPropertyKey.unhealthyThresholdCount)));
                createTargetGroupRequest.setHealthCheckIntervalSeconds(
                    Integer.parseInt(getOrDefault(releaseGroup.properties, ReleaseGroupPropertyKey.healthCheckIntervalSeconds)));

                if (modified[0]) {
                    upenaStore.releaseGroups.update(releaseGroupKey, releaseGroup);
                }

                LOG.info("Modifying {}", createTargetGroupRequest);
                CreateTargetGroupResult createTargetGroupResult = elbc.createTargetGroup(createTargetGroupRequest);

                if (createTargetGroupResult != null && createTargetGroupResult.getTargetGroups() != null) {
                    for (TargetGroup targetGroup : createTargetGroupResult.getTargetGroups()) {

                        targetGroups.put(targetGroup.getTargetGroupName(), targetGroup);

                        releaseGroupKey = targetGroupRelease.get(targetGroup.getTargetGroupName());
                        releaseGroup = upenaStore.releaseGroups.get(releaseGroupKey);

                        ModifyTargetGroupAttributesRequest modifyTargetGroupAttributesRequest = new ModifyTargetGroupAttributesRequest();
                        modifyTargetGroupAttributesRequest.setTargetGroupArn(targetGroup.getTargetGroupArn());

                        List<TargetGroupAttribute> attributes = Lists.newArrayList();
                        attributes.add(new TargetGroupAttribute()
                            .withKey("deregistration_delay.timeout_seconds")
                            .withValue(getOrDefault(releaseGroup.properties, ReleaseGroupPropertyKey.deregistrationDelayTimeoutSeconds)));

                        attributes.add(new TargetGroupAttribute()
                            .withKey("stickiness.enabled")
                            .withValue(getOrDefault(releaseGroup.properties, ReleaseGroupPropertyKey.stickinessEnabled)));

                        attributes.add(new TargetGroupAttribute()
                            .withKey("stickiness.type")
                            .withValue(getOrDefault(releaseGroup.properties, ReleaseGroupPropertyKey.stickinessType)));

                        attributes.add(new TargetGroupAttribute()
                            .withKey("stickiness.lb_cookie.duration_seconds")
                            .withValue(getOrDefault(releaseGroup.properties, ReleaseGroupPropertyKey.stickinessLBCookieDurationSeconds)));

                        modifyTargetGroupAttributesRequest.setAttributes(attributes);

                        LOG.info("Modifying {}", modifyTargetGroupAttributesRequest);
                        elbc.modifyTargetGroupAttributes(modifyTargetGroupAttributesRequest);

                        AddTagsRequest addTag = new AddTagsRequest()
                            .withResourceArns(targetGroup.getTargetGroupArn())
                            .withTags(new Tag().withKey("upena_name").withValue(targetGroupIdToName.get(targetGroup.getTargetGroupName())));
                        LOG.info("Modifying {}", addTag);
                        elbc.addTags(addTag);

                    }
                }
            }
        }
    }

    private void ensureTargetsRegistered(ListMultimap<String, Instance> targetGroupInstances, Map<String, TargetGroup> targetGroups,
        AmazonElasticLoadBalancingClient elbc) throws Exception {
        // ensure instance is registered
        Host self = upenaStore.hosts.get(selfHostKey);
        for (Map.Entry<String, Collection<Instance>> entry : targetGroupInstances.asMap().entrySet()) {
            String targetGroupArn = targetGroups.get(entry.getKey()).getTargetGroupArn();

            Map<TargetDescription, TargetHealth> targetDescriptionsHealth = Maps.newHashMap();

            DescribeTargetHealthRequest describeTargetHealthRequest = new DescribeTargetHealthRequest();
            describeTargetHealthRequest.setTargetGroupArn(targetGroupArn);
            DescribeTargetHealthResult describeTargetHealth = elbc.describeTargetHealth(describeTargetHealthRequest);
            if (describeTargetHealth != null && describeTargetHealth.getTargetHealthDescriptions() != null) {
                for (TargetHealthDescription targetHealthDescription : describeTargetHealth.getTargetHealthDescriptions()) {
                    //String healthCheckPort = targetHealthDescription.getHealthCheckPort();
                    TargetDescription targetDescription = targetHealthDescription.getTarget();
                    TargetHealth targetHealth = targetHealthDescription.getTargetHealth();

                    targetDescriptionsHealth.put(targetDescription, targetHealth);
                }
            }

            List<TargetDescription> addTargetDescriptions = Lists.newArrayList();
            for (Instance instance : entry.getValue()) {
                TargetDescription targetDescription = new TargetDescription();
                targetDescription.setId(self.instanceId);

                Instance.Port port = instance.ports.get("main"); // Hmmm
                targetDescription.setPort(port.port);

                if (targetDescriptionsHealth.remove(targetDescription) == null) {
                    addTargetDescriptions.add(targetDescription);
                }
            }

            if (!addTargetDescriptions.isEmpty()) {
                RegisterTargetsRequest registerTargetsRequest = new RegisterTargetsRequest();
                registerTargetsRequest.setTargetGroupArn(targetGroupArn);
                registerTargetsRequest.setTargets(addTargetDescriptions);
                LOG.info("Registering {}", registerTargetsRequest);
                elbc.registerTargets(registerTargetsRequest);
            }

            if (!targetDescriptionsHealth.isEmpty()) {
                List<TargetDescription> remove = Lists.newArrayList();
                for (TargetDescription targetDescription : targetDescriptionsHealth.keySet()) {
                    if (targetDescription.getId().equals(self.instanceId)) {
                        remove.add(targetDescription);
                    }
                }
                if (!remove.isEmpty()) {
                    DeregisterTargetsRequest deregisterTargetsRequest = new DeregisterTargetsRequest();
                    deregisterTargetsRequest.setTargetGroupArn(targetGroupArn);
                    deregisterTargetsRequest.setTargets(targetDescriptionsHealth.keySet());
                    LOG.info("Deregistering {}", deregisterTargetsRequest);
                    elbc.deregisterTargets(deregisterTargetsRequest);
                }
            }
        }
    }

    public void cleanupAll() throws Exception {

    }

    private void updateIfChanged(String currentValue,
        Map<String, String> properties,
        ReleaseGroupPropertyKey releaseGroupPropertyKey,
        boolean[] updated,
        Update update) {

        String value = properties.getOrDefault(releaseGroupPropertyKey.key(), releaseGroupPropertyKey.getDefaultValue());
        if (!value.equals(currentValue)) {
            update.set(value);
            updated[0] = true;
        }

    }

    static interface Update {

        void set(String input);
    }

    private String getOrDefault(Map<String, String> properties, ReleaseGroupPropertyKey releaseGroupPropertyKey) {
        return properties.getOrDefault(releaseGroupPropertyKey.key(), releaseGroupPropertyKey.getDefaultValue());
    }
}
