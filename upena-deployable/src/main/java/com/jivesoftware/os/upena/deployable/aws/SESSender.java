package com.jivesoftware.os.upena.deployable.aws;

import com.amazonaws.regions.Region;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.service.SimpleEmailSender;
import java.util.Collections;

/**
 *
 * @author jonathan.colt
 */
public class SESSender implements SimpleEmailSender {

    private final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String from;
    private final Region region;

    public SESSender(String from, Region region) {
        this.from = from;
        this.region = region;
    }

    @Override
    public void send(String to, String subject, String body) throws Exception {
        try {
            AmazonSimpleEmailServiceClient client = new AmazonSimpleEmailServiceClient();
            client.setRegion(region);
            client.sendEmail(new SendEmailRequest(from,
                new Destination(Collections.singletonList(to)),
                new Message(
                    new Content(subject),
                    new Body(new Content(body))
                )
            ));
        } catch (Exception x) {
            LOG.warn("Failed to send email to:{}", new Object[]{to}, x);
        }
    }

}
