package fafoshop.pos.product.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.product.dto.ProductCreateRequest;
import fafoshop.pos.product.dto.ProductCreateResponse;
import fafoshop.pos.product.process.ProductCreateProcess;

@Path("pos/product")
public class ProductCreateWebService extends AbstractWebService {

	@POST
	@Path("/create")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public ProductCreateResponse create(ProductCreateRequest request) {
		return (ProductCreateResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new ProductCreateProcess(this);
	}
}
