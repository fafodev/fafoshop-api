package fafoshop.pos.supplier.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.supplier.dto.SupplierDeleteRequest;
import fafoshop.pos.supplier.dto.SupplierDeleteResponse;
import fafoshop.pos.supplier.process.SupplierDeleteProcess;

@Path("pos/supplier")
public class SupplierDeleteWebService extends AbstractWebService {

	@POST
	@Path("/delete")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public SupplierDeleteResponse delete(SupplierDeleteRequest request) {
		return (SupplierDeleteResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new SupplierDeleteProcess(this);
	}
}
