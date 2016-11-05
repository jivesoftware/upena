package com.jivesoftware.os.upena.deployable.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;

/**
 *
 * @author jonathan.colt
 */
public class AWSClientFactory {

    private final String region;
    private final String roleArn;

    public AWSClientFactory(String region, String roleArn) {
        this.region = region;
        this.roleArn = roleArn;
    }

    public boolean enabled() {
        return region != null || roleArn != null;
    }

    private AWSCredentials getFederatedSession(String sessionName) {
        AWSSecurityTokenServiceClient stsClient = new AWSSecurityTokenServiceClient();

        AssumeRoleRequest assumeRequest = new AssumeRoleRequest()
            .withRoleArn(roleArn)
            .withDurationSeconds(3_600)
            .withRoleSessionName(sessionName);

        AssumeRoleResult assumeResult = stsClient.assumeRole(assumeRequest);

        return new BasicSessionCredentials(
            assumeResult.getCredentials()
            .getAccessKeyId(), assumeResult.getCredentials().getSecretAccessKey(),
            assumeResult.getCredentials().getSessionToken());
    }

    public AmazonEC2Client getEC2(String session) {

        AWSCredentials cred = (roleArn != null) ? getFederatedSession(session) : null;
        AmazonEC2Client amazonEC2Client = (cred) == null ? new AmazonEC2Client() : new AmazonEC2Client(cred);
        amazonEC2Client.setRegion(com.amazonaws.regions.Region.getRegion(Regions.fromName(region)));
        return amazonEC2Client;
    }

    public AmazonElasticLoadBalancingClient getELBC(String session) {
        AWSCredentials cred = (roleArn != null) ? getFederatedSession(session) : null;
        AmazonElasticLoadBalancingClient elbc = (cred) == null ? new AmazonElasticLoadBalancingClient() : new AmazonElasticLoadBalancingClient(cred);
        elbc.setRegion(com.amazonaws.regions.Region.getRegion(Regions.fromName(region)));
        return elbc;
    }

}
