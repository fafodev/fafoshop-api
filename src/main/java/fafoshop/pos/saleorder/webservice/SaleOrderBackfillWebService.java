package fafoshop.pos.saleorder.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.saleorder.dto.SaleOrderBackfillRequest;
import fafoshop.pos.saleorder.dto.SaleOrderBackfillResponse;
import fafoshop.pos.saleorder.process.SaleOrderBackfillProcess;

/** Bổ sung đơn bán cho 1 ngày trước đó — xem {@link SaleOrderBackfillProcess}. */
@Path("pos/saleorder")
public class SaleOrderBackfillWebService extends AbstractWebService {

	@POST
	@Path("/backfill")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public SaleOrderBackfillResponse backfill(SaleOrderBackfillRequest request) {
		return (SaleOrderBackfillResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new SaleOrderBackfillProcess(this);
	}
}
