package fafoshop.pos.saleorder.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.saleorder.dto.SaleOrderSearchRequest;
import fafoshop.pos.saleorder.dto.SaleOrderSearchResponse;
import fafoshop.pos.saleorder.process.SaleOrderSearchProcess;

@Path("pos/saleorder")
public class SaleOrderSearchWebService extends AbstractWebService {

	@POST
	@Path("/search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public SaleOrderSearchResponse search(SaleOrderSearchRequest request) {
		return (SaleOrderSearchResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new SaleOrderSearchProcess(this);
	}
}
