package fafoshop.pos.inboundreceipt.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptUpdateRequest;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptUpdateResponse;
import fafoshop.pos.inboundreceipt.process.InboundReceiptUpdateProcess;

@Path("pos/inboundreceipt")
public class InboundReceiptUpdateWebService extends AbstractWebService {

	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public InboundReceiptUpdateResponse update(InboundReceiptUpdateRequest request) {
		return (InboundReceiptUpdateResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new InboundReceiptUpdateProcess(this);
	}
}
