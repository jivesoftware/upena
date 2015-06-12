package com.jivesoftware.os.upena.status;

import com.jivesoftware.os.routing.bird.deployable.reporter.shared.StatusReport;
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/cluster")
public class StatusReportRestEndpoints {

    @Context
    ActiveStatusReports statusReports;

    @POST
    @Consumes("application/json")
    @Path("/status/add")
    @Produces("application/json")
    public Response add(StatusReport statusReport) {
        statusReports.add(statusReport);
        return Response.ok().build();
    }

    @GET
    @Consumes("application/json")
    @Path("/status/list")
    public Response listAnnouncements(@QueryParam("callback") @DefaultValue("") String callback) {
        LinkedList<StatusReport> linkedList = new LinkedList<>();
        for (StatusReport entry : statusReports.active()) {
            linkedList.add(entry);
        }

        if (callback.length() > 0) {
            return ResponseHelper.INSTANCE.jsonpResponse(callback, linkedList);
        } else {
            return ResponseHelper.INSTANCE.jsonResponse(linkedList);
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/alerts/list")
    public Response listAlerts(@QueryParam("callback") @DefaultValue("") String callback) {
        Alerts alerts = new Alerts((int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()), statusReports.listAlerts());
        if (callback.length() > 0) {
            return ResponseHelper.INSTANCE.jsonpResponse(callback, alerts);
        } else {
            return ResponseHelper.INSTANCE.jsonResponse(alerts);
        }
    }
}
