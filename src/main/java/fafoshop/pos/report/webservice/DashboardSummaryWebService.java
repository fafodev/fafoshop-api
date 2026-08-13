package fafoshop.pos.report.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.report.dto.DashboardSummaryRequest;
import fafoshop.pos.report.dto.DashboardSummaryResponse;
import fafoshop.pos.report.process.DashboardSummaryProcess;

@Path("pos/report")
public class DashboardSummaryWebService extends AbstractWebService {

	@POST
	@Path("/dashboardSummary")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public DashboardSummaryResponse dashboardSummary(DashboardSummaryRequest request) {
		return (DashboardSummaryResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new DashboardSummaryProcess(this);
	}
}
