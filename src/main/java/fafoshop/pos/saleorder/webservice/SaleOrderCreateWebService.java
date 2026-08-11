package fafoshop.pos.saleorder.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.saleorder.dto.SaleOrderCreateRequest;
import fafoshop.pos.saleorder.dto.SaleOrderCreateResponse;
import fafoshop.pos.saleorder.process.SaleOrderCreateProcess;

@Path("pos/saleorder")
public class SaleOrderCreateWebService extends AbstractWebService {

	@POST
	@Path("/create")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public SaleOrderCreateResponse create(SaleOrderCreateRequest request) {
		return (SaleOrderCreateResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new SaleOrderCreateProcess(this);
	}
}
