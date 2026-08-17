package fafoshop.pos.saleorder.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.saleorder.dto.SaleOrderUpdateRequest;
import fafoshop.pos.saleorder.dto.SaleOrderUpdateResponse;
import fafoshop.pos.saleorder.process.SaleOrderUpdateProcess;

@Path("pos/saleorder")
public class SaleOrderUpdateWebService extends AbstractWebService {

	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public SaleOrderUpdateResponse update(SaleOrderUpdateRequest request) {
		return (SaleOrderUpdateResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new SaleOrderUpdateProcess(this);
	}
}
