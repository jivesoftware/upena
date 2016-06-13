package com.jivesoftware.os.upena.deployable;

import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.VolumeType;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import java.util.ArrayList;

/**
 *
 * @author jonathan.colt
 */
public class AWS {

    public static void main(String[] args) {

        AWSSessionCredentials credentials = getSession("", "");

        AmazonEC2Client ec2 = getClientForAccount(credentials);
        

        System.out.println("hostsResult:" + ec2.describeHosts());

        System.out.println("account:" + ec2.describeAccountAttributes());
        System.out.println("zones:" + ec2.describeAvailabilityZones());
        System.out.println("account:" + ec2.describeAddresses());
        System.out.println("status:" + ec2.describeInstanceStatus());
        System.out.println("instance:" + ec2.describeInstances());
        System.out.println("volumes:" + ec2.describeVolumes());

        CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest(500, "us-west-2a").withVolumeType(VolumeType.St1);

        CreateVolumeResult volumeResult = ec2.createVolume(createVolumeRequest);

        ArrayList<Tag> instanceTags = new ArrayList<>();
        instanceTags.add(new Tag("Name", "Upena-Test"));
        CreateTagsRequest createTagsRequest = new CreateTagsRequest().withTags(instanceTags).withResources(volumeResult.getVolume().getVolumeId());
        ec2.createTags(createTagsRequest);

//        RunInstancesRequest runInstancesRequest =
//            new RunInstancesRequest();
//
//        runInstancesRequest.withImageId("ami-4b814f22")
//            .withInstanceType("m1.small")
//            .withMinCount(1)
//            .withMaxCount(1)
//            .withKeyName("reco-pipeline")
//            .withSecurityGroups("infra-pipeline");
//
//        client.runInstances(runInstancesRequest);
        AmazonElasticLoadBalancingClient elbc = new AmazonElasticLoadBalancingClient(credentials);
        DescribeLoadBalancersResult loadBalancersResult = elbc.describeLoadBalancers();

        /*
        //create load balancer
        CreateLoadBalancerRequest lbRequest = new CreateLoadBalancerRequest();
        lbRequest.setLoadBalancerName("loader");
        List<Listener> listeners = new ArrayList<Listener>(1);
        listeners.add(new Listener("HTTP", 80, 80));
        lbRequest.withAvailabilityZones(availabilityZone1,availabilityZone2);
        lbRequest.setListeners(listeners);

        CreateLoadBalancerResult lbResult=elb.createLoadBalancer(lbRequest);
        System.out.println("created load balancer loader");
        */


        /*
        //get the running instances
        DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
        List<Reservation> reservations = describeInstancesRequest.getReservations();
        List<Instance> instances = new ArrayList<Instance>();

        for (Reservation reservation : reservations) {
            instances.addAll(reservation.getInstances());
        }


        //get instance id's
        String id;
        List instanceId=new ArrayList();
        List instanceIdString=new ArrayList();
        Iterator<Instance> iterator=instances.iterator();
        while (iterator.hasNext())
        {
            id=iterator.next().getInstanceId();
            instanceId.add(new com.amazonaws.services.elasticloadbalancing.model.Instance(id));
            instanceIdString.add(id);
        }


        //register the instances to the balancer
        RegisterInstancesWithLoadBalancerRequest register =new RegisterInstancesWithLoadBalancerRequest();
        register.setLoadBalancerName("loader");
        register.setInstances((Collection)instanceId);
        RegisterInstancesWithLoadBalancerResult registerWithLoadBalancerResult= elb.registerInstancesWithLoadBalancer(register);
        */
    }

    private static AWSSessionCredentials getSession(String acccessKey,
        String secretKey) {
        AWSSecurityTokenServiceClient stsClient = new AWSSecurityTokenServiceClient(new BasicAWSCredentials(acccessKey, secretKey));

        AssumeRoleRequest assumeRequest = new AssumeRoleRequest()
            .withRoleArn("arn:aws:iam::642745549043:role/administrator")
            .withDurationSeconds(3600)
            .withRoleSessionName("upena");

        AssumeRoleResult assumeResult = stsClient.assumeRole(assumeRequest);

        BasicSessionCredentials temporaryCredentials = new BasicSessionCredentials(
            assumeResult.getCredentials()
            .getAccessKeyId(), assumeResult.getCredentials().getSecretAccessKey(),
            assumeResult.getCredentials().getSessionToken());
        return temporaryCredentials;
    }

    private static AmazonEC2Client getClientForAccount(AWSSessionCredentials credentials) {

        AmazonEC2Client amazonEC2Client = new AmazonEC2Client(credentials);
        amazonEC2Client.setRegion(Region.getRegion(Regions.US_WEST_2));

        return amazonEC2Client;
    }

}
