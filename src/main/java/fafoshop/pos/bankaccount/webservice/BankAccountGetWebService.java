package fafoshop.pos.bankaccount.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.bankaccount.dto.BankAccountGetRequest;
import fafoshop.pos.bankaccount.dto.BankAccountGetResponse;
import fafoshop.pos.bankaccount.process.BankAccountGetProcess;

@Path("pos/bankaccount")
public class BankAccountGetWebService extends AbstractWebService {

	@POST
	@Path("/get")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public BankAccountGetResponse get(BankAccountGetRequest request) {
		return (BankAccountGetResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new BankAccountGetProcess(this);
	}
}
