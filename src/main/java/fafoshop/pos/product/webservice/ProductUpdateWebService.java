package fafoshop.pos.product.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.product.dto.ProductUpdateRequest;
import fafoshop.pos.product.dto.ProductUpdateResponse;
import fafoshop.pos.product.process.ProductUpdateProcess;

@Path("pos/product")
public class ProductUpdateWebService extends AbstractWebService {

	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public ProductUpdateResponse update(ProductUpdateRequest request) {
		return (ProductUpdateResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new ProductUpdateProcess(this);
	}
}
