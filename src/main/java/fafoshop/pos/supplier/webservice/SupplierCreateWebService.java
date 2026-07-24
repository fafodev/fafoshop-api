package fafoshop.pos.supplier.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.supplier.dto.SupplierCreateRequest;
import fafoshop.pos.supplier.dto.SupplierCreateResponse;
import fafoshop.pos.supplier.process.SupplierCreateProcess;

@Path("pos/supplier")
public class SupplierCreateWebService extends AbstractWebService {

	@POST
	@Path("/create")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public SupplierCreateResponse create(SupplierCreateRequest request) {
		return (SupplierCreateResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new SupplierCreateProcess(this);
	}
}
