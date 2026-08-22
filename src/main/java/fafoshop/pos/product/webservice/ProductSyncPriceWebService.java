package fafoshop.pos.product.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.product.dto.ProductSyncPriceRequest;
import fafoshop.pos.product.dto.ProductSyncPriceResponse;
import fafoshop.pos.product.process.ProductSyncPriceProcess;

@Path("pos/product")
public class ProductSyncPriceWebService extends AbstractWebService {

	@POST
	@Path("/syncprice")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public ProductSyncPriceResponse syncPrice(ProductSyncPriceRequest request) {
		return (ProductSyncPriceResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new ProductSyncPriceProcess(this);
	}
}
