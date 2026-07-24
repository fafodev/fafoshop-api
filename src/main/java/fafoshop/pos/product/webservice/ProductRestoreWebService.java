package fafoshop.pos.product.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.product.dto.ProductRestoreRequest;
import fafoshop.pos.product.dto.ProductRestoreResponse;
import fafoshop.pos.product.process.ProductRestoreProcess;

@Path("pos/product")
public class ProductRestoreWebService extends AbstractWebService {

	@POST
	@Path("/restore")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public ProductRestoreResponse restore(ProductRestoreRequest request) {
		return (ProductRestoreResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new ProductRestoreProcess(this);
	}
}
