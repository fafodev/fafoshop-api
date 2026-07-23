package fafoshop.pos.auth.webservice;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import fafoshop.common.auth.NoAuth;
import fafoshop.common.auth.SessionCookieUtility;
import fafoshop.common.process.AbstractProcess;
import fafoshop.common.utility.IdTokenUtility;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.auth.dto.AuthLoginRequest;
import fafoshop.pos.auth.dto.AuthLoginResponse;
import fafoshop.pos.auth.process.AuthLoginProcess;

/**
 * Đăng nhập — token phiên gửi cho client qua cookie HttpOnly (xem
 * SessionCookieUtility), KHÔNG trả trong JSON body, để JavaScript phía
 * client (kể cả khi trang dính XSS) không thể đọc được token.
 */
@Path("pos/auth")
public class AuthWebService extends AbstractWebService {

	@Context
	private HttpServletResponse httpResponse;

	@POST
	@Path("/login")
	@NoAuth
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public AuthLoginResponse login(AuthLoginRequest request) throws IOException {

		AuthLoginResponse response = (AuthLoginResponse) super.executeProcess(request);

		if (response != null && response.token != null) {
			int maxAgeSeconds = IdTokenUtility.getSessionMinutes() * 60;
			httpResponse.addHeader("Set-Cookie",
					SessionCookieUtility.buildSessionCookie(response.token, maxAgeSeconds));
			response.token = null;
		}

		return response;
	}

	@Override
	protected AbstractProcess getProcess() {
		return new AuthLoginProcess(this);
	}
}
