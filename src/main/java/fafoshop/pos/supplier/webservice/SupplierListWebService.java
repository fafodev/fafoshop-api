package fafoshop.pos.supplier.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.supplier.dto.SupplierListRequest;
import fafoshop.pos.supplier.dto.SupplierListResponse;
import fafoshop.pos.supplier.process.SupplierListProcess;

@Path("pos/supplier")
public class SupplierListWebService extends AbstractWebService {

	@POST
	@Path("/list")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public SupplierListResponse list(SupplierListRequest request) {
		return (SupplierListResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new SupplierListProcess(this);
	}
}
