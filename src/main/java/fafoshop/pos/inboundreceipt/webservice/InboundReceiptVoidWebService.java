package fafoshop.pos.inboundreceipt.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptVoidRequest;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptVoidResponse;
import fafoshop.pos.inboundreceipt.process.InboundReceiptVoidProcess;

@Path("pos/inboundreceipt")
public class InboundReceiptVoidWebService extends AbstractWebService {

	@POST
	@Path("/void")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public InboundReceiptVoidResponse voidReceipt(InboundReceiptVoidRequest request) {
		return (InboundReceiptVoidResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new InboundReceiptVoidProcess(this);
	}
}
