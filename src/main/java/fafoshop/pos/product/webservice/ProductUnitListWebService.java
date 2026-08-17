package fafoshop.pos.product.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.product.dto.ProductUnitListRequest;
import fafoshop.pos.product.dto.ProductUnitListResponse;
import fafoshop.pos.product.process.ProductUnitListProcess;

@Path("pos/product")
public class ProductUnitListWebService extends AbstractWebService {

	@POST
	@Path("/unit/list")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public ProductUnitListResponse list(ProductUnitListRequest request) {
		return (ProductUnitListResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new ProductUnitListProcess(this);
	}
}
