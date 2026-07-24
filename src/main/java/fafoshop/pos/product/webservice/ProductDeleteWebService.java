package fafoshop.pos.product.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.product.dto.ProductDeleteRequest;
import fafoshop.pos.product.dto.ProductDeleteResponse;
import fafoshop.pos.product.process.ProductDeleteProcess;

@Path("pos/product")
public class ProductDeleteWebService extends AbstractWebService {

	@POST
	@Path("/delete")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public ProductDeleteResponse delete(ProductDeleteRequest request) {
		return (ProductDeleteResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new ProductDeleteProcess(this);
	}
}
