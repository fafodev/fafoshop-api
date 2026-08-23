package fafoshop.pos.auth.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.auth.dto.AuthMenuRequest;
import fafoshop.pos.auth.dto.AuthMenuResponse;
import fafoshop.pos.auth.process.AuthMenuProcess;

@Path("pos/auth")
public class AuthMenuWebService extends AbstractWebService {

	@POST
	@Path("/menu")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public AuthMenuResponse menu(AuthMenuRequest request) {
		return (AuthMenuResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new AuthMenuProcess(this);
	}
}
