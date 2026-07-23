package fafoshop.pos.auth.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.auth.NoAuth;
import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.auth.dto.AuthLoginRequest;
import fafoshop.pos.auth.dto.AuthLoginResponse;
import fafoshop.pos.auth.process.AuthLoginProcess;

@Path("pos/auth")
public class AuthWebService extends AbstractWebService {

	@POST
	@Path("/login")
	@NoAuth
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public AuthLoginResponse login(AuthLoginRequest request) {
		return (AuthLoginResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new AuthLoginProcess(this);
	}
}
