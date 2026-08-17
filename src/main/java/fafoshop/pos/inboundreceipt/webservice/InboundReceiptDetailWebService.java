package fafoshop.pos.inboundreceipt.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptDetailRequest;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptDetailResponse;
import fafoshop.pos.inboundreceipt.process.InboundReceiptDetailProcess;

@Path("pos/inboundreceipt")
public class InboundReceiptDetailWebService extends AbstractWebService {

	@POST
	@Path("/detail")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public InboundReceiptDetailResponse detail(InboundReceiptDetailRequest request) {
		return (InboundReceiptDetailResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new InboundReceiptDetailProcess(this);
	}
}
