package fafoshop.pos.inboundreceipt.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptCreateRequest;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptCreateResponse;
import fafoshop.pos.inboundreceipt.process.InboundReceiptCreateProcess;

@Path("pos/inboundreceipt")
public class InboundReceiptCreateWebService extends AbstractWebService {

	@POST
	@Path("/create")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public InboundReceiptCreateResponse create(InboundReceiptCreateRequest request) {
		return (InboundReceiptCreateResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new InboundReceiptCreateProcess(this);
	}
}
