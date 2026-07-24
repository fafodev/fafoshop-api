package fafoshop.pos.supplier.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.supplier.dto.SupplierUpdateRequest;
import fafoshop.pos.supplier.dto.SupplierUpdateResponse;
import fafoshop.pos.supplier.process.SupplierUpdateProcess;

@Path("pos/supplier")
public class SupplierUpdateWebService extends AbstractWebService {

	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public SupplierUpdateResponse update(SupplierUpdateRequest request) {
		return (SupplierUpdateResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new SupplierUpdateProcess(this);
	}
}
