package fafoshop.pos.auth.webservice;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.CookieParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import fafoshop.common.auth.NoAuth;
import fafoshop.common.auth.SessionCookieUtility;
import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.auth.dto.AuthLogoutRequest;
import fafoshop.pos.auth.dto.AuthLogoutResponse;
import fafoshop.pos.auth.process.AuthLogoutProcess;

/**
 * Đăng xuất — xoá session_token (nếu cookie còn hợp lệ) và LUÔN trả cookie
 * Set-Cookie hết hạn ngay (Max-Age=0) để trình duyệt xoá cookie phiên phía
 * client. @NoAuth vì phải cho phép gọi kể cả khi cookie đã hết hạn/không hợp
 * lệ — mục đích chính là đảm bảo trình duyệt xoá sạch cookie, không phải
 * kiểm tra quyền.
 *
 * Tách WebService riêng (không gộp vào AuthWebService) theo đúng quy ước dự
 * án: mỗi hành động nghiệp vụ có 1 Process/WebService riêng (xem
 * ProductSearchWebService/ProductCreateWebService) — getProcess() chỉ trả
 * về 1 process cố định cho toàn class nên không gộp 2 action khác process
 * vào chung 1 WebService được.
 */
@Path("pos/auth")
public class AuthLogoutWebService extends AbstractWebService {

	@Context
	private HttpServletResponse httpResponse;

	@POST
	@Path("/logout")
	@NoAuth
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public AuthLogoutResponse logout(
			@CookieParam(SessionCookieUtility.SESSION_COOKIE_NAME) String sessionToken) {

		AuthLogoutRequest request = new AuthLogoutRequest();
		request.token = sessionToken;

		AuthLogoutResponse response = (AuthLogoutResponse) super.executeProcess(request);

		httpResponse.addHeader("Set-Cookie", SessionCookieUtility.buildExpiredSessionCookie());

		return response;
	}

	@Override
	protected AbstractProcess getProcess() {
		return new AuthLogoutProcess(this);
	}
}
