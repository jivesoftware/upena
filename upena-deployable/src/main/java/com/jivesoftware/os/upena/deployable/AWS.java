package com.jivesoftware.os.upena.deployable;

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

        AmazonEC2Client ec2 = getClientForAccount(
            "",
            ""
        );

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
    }

    private static AmazonEC2Client getClientForAccount(String acccessKey,
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

        AmazonEC2Client amazonEC2Client = new AmazonEC2Client(temporaryCredentials);
        amazonEC2Client.setRegion(Region.getRegion(Regions.US_WEST_2));

        return amazonEC2Client;
    }

}
