package fafoshop.pos.supplier.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.supplier.dto.SupplierRestoreRequest;
import fafoshop.pos.supplier.dto.SupplierRestoreResponse;
import fafoshop.pos.supplier.process.SupplierRestoreProcess;

@Path("pos/supplier")
public class SupplierRestoreWebService extends AbstractWebService {

	@POST
	@Path("/restore")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public SupplierRestoreResponse restore(SupplierRestoreRequest request) {
		return (SupplierRestoreResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new SupplierRestoreProcess(this);
	}
}
