package fafoshop.pos.saleorder.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.saleorder.dto.SaleOrderDetailRequest;
import fafoshop.pos.saleorder.dto.SaleOrderDetailResponse;
import fafoshop.pos.saleorder.process.SaleOrderDetailProcess;

@Path("pos/saleorder")
public class SaleOrderDetailWebService extends AbstractWebService {

	@POST
	@Path("/detail")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public SaleOrderDetailResponse detail(SaleOrderDetailRequest request) {
		return (SaleOrderDetailResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new SaleOrderDetailProcess(this);
	}
}
