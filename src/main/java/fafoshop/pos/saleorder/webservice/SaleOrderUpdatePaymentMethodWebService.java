package fafoshop.pos.saleorder.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.saleorder.dto.SaleOrderUpdatePaymentMethodRequest;
import fafoshop.pos.saleorder.dto.SaleOrderUpdatePaymentMethodResponse;
import fafoshop.pos.saleorder.process.SaleOrderUpdatePaymentMethodProcess;

@Path("pos/saleorder")
public class SaleOrderUpdatePaymentMethodWebService extends AbstractWebService {

	@POST
	@Path("/updatepaymentmethod")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public SaleOrderUpdatePaymentMethodResponse updatePaymentMethod(SaleOrderUpdatePaymentMethodRequest request) {
		return (SaleOrderUpdatePaymentMethodResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new SaleOrderUpdatePaymentMethodProcess(this);
	}
}
