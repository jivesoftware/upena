package com.jivesoftware.os.upena.deployable.region;

import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.AvailabilityZone;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.Tag;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.aws.AWSClientFactory;
import com.jivesoftware.os.upena.deployable.region.LoadBalancersPluginRegion.LoadBalancersPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.JenkinsHash;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.LB;
import com.jivesoftware.os.upena.shared.LBKey;
import com.jivesoftware.os.upena.shared.LoadBalancerFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.ReleaseGroupPropertyKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;

/**
 *
 */
// soy.page.loadBalancersPluginRegion
public class LoadBalancersPluginRegion implements PageRegion<LoadBalancersPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;
    private final AWSClientFactory awsClientFactory;

    public LoadBalancersPluginRegion(String template,
        SoyRenderer renderer,
        UpenaStore upenaStore,
        AWSClientFactory awsClientFactory
    ) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
        this.awsClientFactory = awsClientFactory;
    }

    @Override
    public String getRootPath() {
        return "/ui/loadbalancers";
    }

    public static class LoadBalancersPluginRegionInput implements PluginInput {

        final String key;
        final String name;
        final String description;
        final String scheme;
        final int loadBalancerPort;
        final int instancePort;
        final List<String> availabilityZones;
        final String protocol;
        final String certificate;
        final String serviceProtocol;

        final List<String> securityGroups;
        final List<String> subnets;
        final Map<String, String> tags;

        final String clusterKey;
        final String cluster;
        final String serviceKey;
        final String service;
        final String releaseGroupKey;
        final String releaseGroup;
        final String action;

        public LoadBalancersPluginRegionInput(String key, String name, String description, String scheme, int loadBalancerPort, int instancePort,
            List<String> availabilityZones, String protocol, String certificate, String serviceProtocol, List<String> securityGroups, List<String> subnets,
            Map<String, String> tags, String clusterKey, String cluster, String serviceKey, String service, String releaseGroupKey, String releaseGroup,
            String action) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.scheme = scheme;
            this.loadBalancerPort = loadBalancerPort;
            this.instancePort = instancePort;
            this.availabilityZones = availabilityZones;
            this.protocol = protocol;
            this.certificate = certificate;
            this.serviceProtocol = serviceProtocol;
            this.securityGroups = securityGroups;
            this.subnets = subnets;
            this.tags = tags;
            this.clusterKey = clusterKey;
            this.cluster = cluster;
            this.serviceKey = serviceKey;
            this.service = service;
            this.releaseGroupKey = releaseGroupKey;
            this.releaseGroup = releaseGroup;
            this.action = action;
        }

        @Override
        public String name() {
            return "Load Balancers";
        }

    }

    @Override
    public String render(String user, LoadBalancersPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();
        if (SecurityUtils.getSubject().isPermitted("write")) {
            data.put("readWrite", true);
        }
        try {

            Map<String, Object> filters = new HashMap<>();
            filters.put("name", input.name);
            filters.put("clusterKey", input.clusterKey);
            filters.put("cluster", input.cluster);
            filters.put("serviceKey", input.serviceKey);
            filters.put("service", input.service);
            filters.put("releaseKey", input.releaseGroupKey);
            filters.put("release", input.releaseGroup);
            data.put("filters", filters);

            LoadBalancerFilter filter = new LoadBalancerFilter(input.name, input.clusterKey, input.serviceKey, input.releaseGroupKey, 0, 100_000);
            if (input.action != null) {
                if (input.action.equals("remove-unattached")) {
                   SecurityUtils.getSubject().checkPermission("write");
                    try {

                        AmazonElasticLoadBalancingClient elbc = awsClientFactory.getELBC("upena-lb");
                        DeleteLoadBalancerRequest deleteLoadBalancerRequest = new DeleteLoadBalancerRequest();
                        deleteLoadBalancerRequest.setLoadBalancerArn(input.name);
                        elbc.deleteLoadBalancer(deleteLoadBalancerRequest);
                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to remove LoadBalancer:" + input.name + "\n" + trace);
                    }
                }

                if (input.action.equals("add")) {
                    SecurityUtils.getSubject().checkPermission("write");
//filters.clear();
                    try {
                        LB newLoadBalancer = new LB(input.name,
                            Objects.firstNonNull(input.description, ""),
                            Objects.firstNonNull(input.scheme, "internal"),
                            input.loadBalancerPort != -1 ? input.loadBalancerPort : 0,
                            input.instancePort != -1 ? input.instancePort : 0,
                            Objects.firstNonNull(input.availabilityZones, Collections.<String>emptyList()),
                            Objects.firstNonNull(input.protocol, "http"),
                            Objects.firstNonNull(input.certificate, ""),
                            Objects.firstNonNull(input.serviceProtocol, "http"),
                            Objects.firstNonNull(input.securityGroups, Collections.<String>emptyList()),
                            Objects.firstNonNull(input.subnets, Collections.<String>emptyList()),
                            Collections.emptyMap(),
                            new ClusterKey(input.clusterKey),
                            new ServiceKey(input.serviceKey),
                            new ReleaseGroupKey(input.releaseGroupKey));
                        upenaStore.loadBalancers.update(null, newLoadBalancer);
                        upenaStore.recordChange(user, "added", System.currentTimeMillis(), "", "loadBalancers-ui", newLoadBalancer.toString());

                        data.put("message", "Created LoadBalancer:" + input.name);
                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to add LoadBalancer:" + input.name + "\n" + trace);
                    }
                } else if (input.action.equals("update")) {
                    SecurityUtils.getSubject().checkPermission("write");
//filters.clear();
                    try {
                        LB loadBalancer = upenaStore.loadBalancers.get(new LBKey(input.key));
                        if (loadBalancer == null) {
                            data.put("message", "Couldn't update no existent loadBalancer. Someone else likely just removed it since your last refresh.");
                        } else {
                            LB updatedLoadBalancer = new LB(
                                loadBalancer.name,
                                Objects.firstNonNull(input.description, loadBalancer.description),
                                Objects.firstNonNull(input.scheme, loadBalancer.scheme),
                                input.loadBalancerPort != -1 ? input.loadBalancerPort : loadBalancer.loadBalancerPort,
                                input.instancePort != -1 ? input.instancePort : loadBalancer.instancePort,
                                Objects.firstNonNull(input.availabilityZones, loadBalancer.availabilityZones),
                                Objects.firstNonNull(input.protocol, loadBalancer.protocol),
                                Objects.firstNonNull(input.certificate, loadBalancer.certificate),
                                Objects.firstNonNull(input.serviceProtocol, loadBalancer.serviceProtocol),
                                Objects.firstNonNull(input.securityGroups, loadBalancer.securityGroups),
                                Objects.firstNonNull(input.subnets, loadBalancer.subnets),
                                loadBalancer.tags,
                                new ClusterKey(Objects.firstNonNull(input.clusterKey, loadBalancer.clusterKey.getKey())),
                                new ServiceKey(Objects.firstNonNull(input.serviceKey, loadBalancer.serviceKey.getKey())),
                                new ReleaseGroupKey(Objects.firstNonNull(input.releaseGroupKey, loadBalancer.releaseGroupKey.getKey())));

                            upenaStore.loadBalancers.update(new LBKey(input.key), updatedLoadBalancer);
                            data.put("message", "Updated LoadBalancer:" + input.name);
                            upenaStore.recordChange(user, "updated", System.currentTimeMillis(), "", "loadBalancers-ui", loadBalancer.toString());
                        }
                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to add LoadBalancer:" + input.name + "\n" + trace);
                    }
                } else if (input.action.equals("remove")) {
                    SecurityUtils.getSubject().checkPermission("write");
                    if (input.key.isEmpty()) {
                        data.put("message", "Failed to remove LoadBalancer:" + input.name);
                    } else {
                        try {
                            LBKey loadBalancerKey = new LBKey(input.key);
                            LB removing = upenaStore.loadBalancers.get(loadBalancerKey);
                            if (removing != null) {
                                upenaStore.loadBalancers.remove(loadBalancerKey);
                                upenaStore.recordChange(user, "removed", System.currentTimeMillis(), "", "loadBalancers-ui", removing.toString());
                            }
                        } catch (Exception x) {
                            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                            data.put("message", "Error while trying to remove LoadBalancer:" + input.name + "\n" + trace);
                        }
                    }
                }
            }

            AmazonElasticLoadBalancingClient elbc = awsClientFactory.getELBC("upena-lb");
            DescribeLoadBalancersRequest describeLoadBalancersRequest = new DescribeLoadBalancersRequest();
            //describeLoadBalancersRequest.setLoadBalancerArns(Collections.emptyList());
            //describeLoadBalancersRequest.setMarker("markerFromPreviousCall"); // paging
            //describeLoadBalancersRequest.setPageSize(1000);
            //describeLoadBalancersRequest.setNames(Collections.emptyList());
            DescribeLoadBalancersResult loadBalancersResult = elbc.describeLoadBalancers(describeLoadBalancersRequest);

            Map<String, LoadBalancer> awsLoadBalancers = new ConcurrentHashMap<>();
            List<String> loadBalancerNames = Lists.newArrayList();
            if (loadBalancersResult != null && loadBalancersResult.getLoadBalancers() != null) {
                for (LoadBalancer loadBalancerDescription : loadBalancersResult.getLoadBalancers()) {
                    awsLoadBalancers.put(loadBalancerDescription.getLoadBalancerName(), loadBalancerDescription);
                    loadBalancerNames.add(loadBalancerDescription.getLoadBalancerName());
                }
            }

            Map<String, List<Tag>> awsLoadBalancersTags = new ConcurrentHashMap<>();
//            if (!loadBalancerNames.isEmpty()) {
//                DescribeTagsRequest describeTagsRequest = new DescribeTagsRequest();
//                describeTagsRequest.setLoadBalancerNames(loadBalancerNames);
//                DescribeTagsResult describeTags = elbc.describeTags(describeTagsRequest);
//                for (TagDescription tagDescription : describeTags.getTagDescriptions()) {
//                    awsLoadBalancersTags.put(tagDescription.getLoadBalancerName(), tagDescription.getTags());
//                }
//            }

            Set<String> attachedLB = new HashSet<>();

            List<Map<String, Object>> rows = new ArrayList<>();
            Map<LBKey, TimestampedValue<LB>> found = upenaStore.loadBalancers.find(false, filter);
            for (Map.Entry<LBKey, TimestampedValue<LB>> entrySet : found.entrySet()) {
                LBKey key = entrySet.getKey();
                TimestampedValue<LB> timestampedValue = entrySet.getValue();
                LB lb = timestampedValue.getValue();

                Status status = new Status();

                Cluster cluster = upenaStore.clusters.get(lb.clusterKey);
                Service service = upenaStore.services.get(lb.serviceKey);
                ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(lb.releaseGroupKey);

                LoadBalancer loadBalancer = awsLoadBalancers.get(lb.name);
                if (loadBalancer == null) {
                    status.message("MISSING");
                    if ("apply".equals(input.action)) {

                    }

                } else {
                    attachedLB.add(loadBalancer.getLoadBalancerName());
                    status.message("ONLINE");
                    populateStatus(status, loadBalancer);
                }

//                try {
//                    CreateTargetGroupRequest createTargetGroup = new CreateTargetGroupRequest();
//                    createTargetGroup.setHealthCheckIntervalSeconds(Integer.MIN_VALUE);
//                    createTargetGroup.setHealthCheckPath(template);
//                    createTargetGroup.setHealthCheckPort(template);
//                    createTargetGroup.setHealthCheckProtocol(template);
//                    createTargetGroup.setHealthCheckTimeoutSeconds(Integer.MIN_VALUE);
//                    createTargetGroup.setUnhealthyThresholdCount(Integer.BYTES);
//
//                    createTargetGroup.setMatcher(template);
//                    createTargetGroup.setName(template);
//                    createTargetGroup.setPort(template);
//                    createTargetGroup.setProtocol(template);
//                    createTargetGroup.setVpcId("");
//
//                    CreateTargetGroupResult createdTargetGroup = elbc.createTargetGroup(createTargetGroup);
//
//                    DescribeTargetGroupsRequest describeTargetGroupsRequest = new DescribeTargetGroupsRequest();
//                    elbc.describeTargetGroups(describeTargetGroupsRequest);
//
//                    DeregisterTargetsRequest deregisterTargetsRequest = new DeregisterTargetsRequest();
//                    deregisterTargetsRequest.setTargetGroupArn(region);
//
//                    TargetDescription targetDescription = new TargetDescription();
//                    targetDescription.setId(user);
//                    targetDescription.setPort(Integer.SIZE);
//
//                    deregisterTargetsRequest.setTargets(Collections.singletonList(targetDescription));
//                    elbc.deregisterTargets(deregisterTargetsRequest);
//
//                    RegisterTargetsRequest registerTargetsRequest = new RegisterTargetsRequest();
//                    registerTargetsRequest.setTargetGroupArn(region);
//
//                    targetDescription = new TargetDescription();
//                    targetDescription.setId(ec2id);
//                    targetDescription.setPort(Integer.SIZE);
//                    registerTargetsRequest.setTargets(Collections.singletonList(targetDescription));
//
//                    elbc.registerTargets(registerTargetsRequest);
//
//                } catch (Exception x) {
//                    status.message("ERROR " + x.getMessage());
//                    LOG.error("while creating a LB", x);
//                }
                if (loadBalancer != null) {
                    try {
                        DescribeListenersRequest describeListenersRequest = new DescribeListenersRequest();
                        describeListenersRequest.setLoadBalancerArn(loadBalancer.getLoadBalancerArn());
                        DescribeListenersResult describeListeners = elbc.describeListeners(describeListenersRequest);
                        if (describeListeners != null && describeListeners.getListeners() != null && !describeListeners.getListeners().isEmpty()) {
                            // TODO validate
                            int i = 1;
                            for (Listener listener : describeListeners.getListeners()) {
                                status.addProperty("listener-" + i, listener.toString());
                                i++;
                            }

                        } else {

//                            List<Certificate> certificates = new ArrayList<>();
//                            if (!StringUtils.isBlank(lb.certificate)) {
//                                Certificate certificate = new Certificate();
//                                certificate.setCertificateArn(lb.certificate);
//                                certificates.add(certificate);
//                            }
//
//                            CreateListenerRequest request = new CreateListenerRequest();
//                            Action action = new Action();
//                            action.setTargetGroupArn(region);
//                            action.setType(ActionTypeEnum.Forward);
//
//                            request.setDefaultActions(Collections.singletonList(action));
//                            request.setLoadBalancerArn(loadBalancer.getLoadBalancerArn());
//                            if (!certificates.isEmpty()) {
//                                request.setCertificates(certificates);
//                            }
//                            request.setPort(lb.instancePort);
//                            request.setProtocol(ProtocolEnum.valueOf(lb.serviceProtocol.toUpperCase()));
//
//                            CreateListenerResult result = elbc.createListener(request);
//                            for (Listener listener : result.getListeners()) {
//                                status.message("Created Listener:" + listener.toString());
//                            }
                        }
                    } catch (Exception x) {
                        status.message("ERROR " + x.getMessage());
                        LOG.error("while creating a LB", x);
                    }
                }

                Map<String, Object> row = new HashMap<>();
                row.put("status", status);

                row.put("clusterKey", lb.clusterKey.getKey());
                row.put("cluster", cluster == null ? "unknown" : cluster.name);

                row.put("serviceKey", lb.serviceKey.getKey());
                row.put("service", service == null ? "unknown" : service.name);

                row.put("releaseKey", lb.releaseGroupKey.getKey());
                row.put("release", releaseGroup == null ? "unknown" : releaseGroup.name);

                row.put("key", key.getKey());

                row.put("name", lb.name);

                row.put("description", lb.description);
                row.put("scheme", lb.scheme);
                row.put("loadBalancerPort", String.valueOf(lb.loadBalancerPort));
                row.put("instancePort", String.valueOf(lb.instancePort));
                row.put("protocol", lb.protocol);
                row.put("certificate", lb.certificate);
                row.put("serviceProtocol", lb.serviceProtocol);

                row.put("availabilityZones", lb.availabilityZones == null ? "" : Joiner.on(",").join(lb.availabilityZones));
                row.put("securityGroups", lb.securityGroups == null ? "" : Joiner.on(",").join(lb.securityGroups));
                row.put("subnets", lb.subnets == null ? "" : Joiner.on(",").join(lb.subnets));
                row.put("tags", lb.tags);

                // TODO add state i.e online, missing, out-of-date, etc...
                rows.add(row);
            }

            Collections.sort(rows, (Map<String, Object> o1, Map<String, Object> o2) -> {
                String loadBalancerName1 = (String) o1.get("name");
                String loadBalancerName2 = (String) o2.get("name");

                int c = loadBalancerName1.compareTo(loadBalancerName2);
                if (c != 0) {
                    return c;
                }
                return c;
            });

            data.put("loadBalancers", rows);

            rows = new ArrayList<>();
            if (loadBalancersResult != null && loadBalancersResult.getLoadBalancers() != null) {
                for (LoadBalancer loadBalancerDescription : loadBalancersResult.getLoadBalancers()) {
                    if (!attachedLB.contains(loadBalancerDescription.getLoadBalancerName())) {
                        Status status = new Status();
                        populateStatus(status, loadBalancerDescription);
                        Map<String, Object> row = new HashMap<>();
                        row.put("status", status);
                        row.put("key", loadBalancerDescription.getLoadBalancerName());
                        row.put("arn", loadBalancerDescription.getLoadBalancerArn());
                        rows.add(row);
                    }
                }
            }
            data.put("unattachedLoadBalancers", rows);

            rows = new ArrayList<>();
            JenkinsHash jenkinsHash = new JenkinsHash();
            ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> foundInstance = upenaStore.instances.find(false,
                new InstanceFilter(null, null, null, null, null, 0, 100_000));

            for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entry : foundInstance.entrySet()) {
                if (entry.getValue().getTombstoned()) {
                    continue;
                }
                Instance instance = entry.getValue().getValue();

                Cluster cluster = upenaStore.clusters.get(instance.clusterKey);
                Service service = upenaStore.services.get(instance.serviceKey);
                Host host = upenaStore.hosts.get(instance.hostKey);
                ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(instance.releaseGroupKey);
                if (releaseGroup.properties != null) {
                    String got = releaseGroup.properties.get(ReleaseGroupPropertyKey.loadBalanced.key());
                    if (got != null && Boolean.valueOf(got) == true) {
                        String targetGroupId = instance.clusterKey.getKey() + "|" + instance.serviceKey.getKey() + "|" + instance.releaseGroupKey.getKey();
                        String targetGroupKey = "tg-" + String.valueOf(Math.abs(jenkinsHash.hash(targetGroupId.getBytes(StandardCharsets.UTF_8), 0)));

                        String targetGroupName = cluster.name + "_" + service.name + "_" + releaseGroup.name;
                        rows.add(ImmutableMap.of("key", targetGroupKey,
                            "value", targetGroupName,
                            "instance", instance.instanceId,
                            "host", host.hostName,
                            "port", instance.ports.get("main").port // barf
                        ));
                    }
                }
            }

            Collections.sort(rows, (Map<String, Object> o1, Map<String, Object> o2) -> {
                String loadBalancerName1 = (String) o1.get("key");
                String loadBalancerName2 = (String) o2.get("key");

                int c = loadBalancerName1.compareTo(loadBalancerName2);
                if (c != 0) {
                    return c;
                }
                return c;
            });

            data.put("loadBalancerPools", rows);

        } catch(AuthorizationException x) {
            throw x;
        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    private void populateStatus(Status status, LoadBalancer loadBalancer) {
        status.addProperty("name", loadBalancer.getLoadBalancerName());
        status.addProperty("canonicalHostedZoneId", loadBalancer.getCanonicalHostedZoneId());
        status.addProperty("dnsName", loadBalancer.getDNSName());
        status.addProperty("arn", loadBalancer.getLoadBalancerArn());
        status.addProperty("scheme", loadBalancer.getScheme());
        status.addProperty("type", loadBalancer.getType());
        status.addProperty("vpcId", loadBalancer.getVpcId());

        int i = 1;
        for (AvailabilityZone availabilityZone : loadBalancer.getAvailabilityZones()) {
            status.addProperty("availabilityZone-" + i, availabilityZone.getZoneName());
            status.addProperty("availabilityZone-subnet-" + i, availabilityZone.getSubnetId());
            i++;
        }

        i = 1;
        for (String securityGroup : loadBalancer.getSecurityGroups()) {
            status.addProperty("securityGroup-" + i, securityGroup);
            i++;
        }
    }

    public static class Status extends HashMap<String, Object> {

        private final List<String> messages = new ArrayList<>();
        private final List<String> properties = new ArrayList<>();

        public Status() {
            put("messages", messages);
            put("properties", properties);
        }

        public void message(String message) {
            messages.add(message);
        }

        public void addProperty(String name, String value) {
            properties.add(name + "=" + value);
        }

    }

    @Override
    public String getTitle() {
        return "Load Balancers";
    }

    private List<String> sanitize(List<String> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        if (list.isEmpty()) {
            return list;
        }
        List<String> sanitized = Lists.newArrayList();
        for (String s : list) {
            s = s.trim();
            if (!StringUtils.isBlank(s)) {
                sanitized.add(s);
            }
        }
        return sanitized;
    }

//    Map<String, String> ensureTags(LoadBalancerKey loadBalancerKey, LoadBalancer loadBalancer, Map<String, String> tags) {
//        Map<String, String> ensuredTags = Maps.newHashMap(tags);
//        ensuredTags.put("loadBalancerKey", loadBalancerKey.getKey());
//        ensuredTags.put("clusterKey", loadBalancer.clusterKey.getKey());
//        ensuredTags.put("serviceKey", loadBalancer.serviceKey.getKey());
//        ensuredTags.put("releaseKey", loadBalancer.releaseGroupKey.getKey());
//        return ensuredTags;
//    }
    public void tag(String remoteUser, String key, String tagName, String tagValue, String action) throws Exception {
        LBKey loadBalancerKey = new LBKey(key);
        LB loadBalancer = upenaStore.loadBalancers.get(loadBalancerKey);
        if (loadBalancer != null) {

            Map<String, String> tags = Maps.newHashMap(loadBalancer.tags);
            if (action.equals("add")) {
                tags.put(tagName, tagValue);
            }
            if (action.equals("remove")) {
                tags.remove(tagName, tagValue);
            }

            LB updatedLoadBalancer = new LB(loadBalancer.name,
                loadBalancer.description,
                loadBalancer.scheme,
                loadBalancer.loadBalancerPort,
                loadBalancer.instancePort,
                loadBalancer.availabilityZones,
                loadBalancer.protocol,
                loadBalancer.certificate,
                loadBalancer.serviceProtocol,
                loadBalancer.securityGroups,
                loadBalancer.subnets,
                tags,
                loadBalancer.clusterKey,
                loadBalancer.serviceKey,
                loadBalancer.releaseGroupKey);

            upenaStore.loadBalancers.update(loadBalancerKey, updatedLoadBalancer);
            upenaStore.recordChange(remoteUser, "added tag", System.currentTimeMillis(), "", "loadBalancers-ui", loadBalancer.toString());
        }
    }

    private final String prettyString(String string) throws IOException {
        string = string.replaceAll(",", ",\n");
        return string;
    }

}
