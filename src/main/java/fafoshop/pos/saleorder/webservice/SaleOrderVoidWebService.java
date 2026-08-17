package fafoshop.pos.saleorder.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.saleorder.dto.SaleOrderVoidRequest;
import fafoshop.pos.saleorder.dto.SaleOrderVoidResponse;
import fafoshop.pos.saleorder.process.SaleOrderVoidProcess;

@Path("pos/saleorder")
public class SaleOrderVoidWebService extends AbstractWebService {

	@POST
	@Path("/void")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public SaleOrderVoidResponse voidOrder(SaleOrderVoidRequest request) {
		return (SaleOrderVoidResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new SaleOrderVoidProcess(this);
	}
}
