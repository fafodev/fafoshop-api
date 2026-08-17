package fafoshop.pos.inboundreceipt.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptSearchRequest;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptSearchResponse;
import fafoshop.pos.inboundreceipt.process.InboundReceiptSearchProcess;

@Path("pos/inboundreceipt")
public class InboundReceiptSearchWebService extends AbstractWebService {

	@POST
	@Path("/search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public InboundReceiptSearchResponse search(InboundReceiptSearchRequest request) {
		return (InboundReceiptSearchResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new InboundReceiptSearchProcess(this);
	}
}
