package fafoshop.pos.product.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.product.dto.ProductSupplierListRequest;
import fafoshop.pos.product.dto.ProductSupplierListResponse;
import fafoshop.pos.product.process.ProductSupplierListProcess;

@Path("pos/product")
public class ProductSupplierListWebService extends AbstractWebService {

	@POST
	@Path("/supplier/list")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public ProductSupplierListResponse list(ProductSupplierListRequest request) {
		return (ProductSupplierListResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new ProductSupplierListProcess(this);
	}
}
