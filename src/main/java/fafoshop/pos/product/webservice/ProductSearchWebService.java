package fafoshop.pos.product.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.product.dto.ProductSearchRequest;
import fafoshop.pos.product.dto.ProductSearchResponse;
import fafoshop.pos.product.process.ProductSearchProcess;

@Path("pos/product")
public class ProductSearchWebService extends AbstractWebService {

	@POST
	@Path("/search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public ProductSearchResponse search(ProductSearchRequest request) {
		return (ProductSearchResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new ProductSearchProcess(this);
	}
}
