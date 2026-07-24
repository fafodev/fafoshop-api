package fafoshop.pos.supplier.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.supplier.dto.SupplierSearchRequest;
import fafoshop.pos.supplier.dto.SupplierSearchResponse;
import fafoshop.pos.supplier.process.SupplierSearchProcess;

@Path("pos/supplier")
public class SupplierSearchWebService extends AbstractWebService {

	@POST
	@Path("/search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public SupplierSearchResponse search(SupplierSearchRequest request) {
		return (SupplierSearchResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new SupplierSearchProcess(this);
	}
}
