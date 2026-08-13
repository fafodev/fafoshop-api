package fafoshop.common.auth;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import fafoshop.common.dto.ErrorDto;
import fafoshop.common.utility.IdTokenUtility;
import fafoshop.common.utility.MessageUtility;

/**
 * Kiểm tra token xác thực cho MỌI request (trừ method có @NoAuth, ví dụ đăng
 * nhập).
 *
 * Đặt ở tầng JAX-RS ContainerRequestFilter (áp dụng tự động cho MỌI
 * endpoint) thay vì gọi tay trong từng Process — tránh trường hợp process
 * mới quên gọi kiểm tra token.
 *
 * Token đọc từ cookie {@link SessionCookieUtility#SESSION_COOKIE_NAME}
 * (HttpOnly, do trình duyệt tự gửi kèm request), KHÔNG còn đọc từ header
 * Authorization/Bearer như trước — tránh việc client (Angular) phải tự cầm
 * giá trị token bằng JavaScript rồi lưu localStorage (lỗ hổng: JS độc hại từ
 * XSS đọc được token). Xem SessionCookieUtility để biết chi tiết thuộc tính
 * cookie.
 *
 * Cũng implement {@link ContainerResponseFilter} để GIA HẠN cookie phiên
 * phía trình duyệt (Max-Age) song song với gia hạn {@code expire_datetime}
 * phía DB ở {@link IdTokenUtility#verify(String)} (sliding session) — thiếu
 * bước này thì dù DB còn hạn, trình duyệt vẫn tự xoá cookie đúng Max-Age cũ
 * (tính từ lúc đăng nhập) và ngừng gửi kèm, khiến request vẫn nhận 401.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthTokenFilter implements ContainerRequestFilter, ContainerResponseFilter {

	public static final String AUTHENTICATED_USER_CODE_PROPERTY = "fafoshop.authenticatedUserCode";

	@Context
	private ResourceInfo resourceInfo;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {

		if (resourceInfo.getResourceMethod() != null
				&& resourceInfo.getResourceMethod().isAnnotationPresent(NoAuth.class)) {
			return;
		}

		Cookie sessionCookie = requestContext.getCookies().get(SessionCookieUtility.SESSION_COOKIE_NAME);
		if (sessionCookie == null || sessionCookie.getValue() == null || sessionCookie.getValue().isEmpty()) {
			abortUnauthorized(requestContext, "MC000002");
			return;
		}

		String token = sessionCookie.getValue();
		String userCode;
		try {
			userCode = IdTokenUtility.verify(token);
		} catch (Exception e) {
			abortUnauthorized(requestContext, "MC000002");
			return;
		}

		if (userCode == null) {
			abortUnauthorized(requestContext, "MC000002");
			return;
		}

		requestContext.setProperty(AUTHENTICATED_USER_CODE_PROPERTY, userCode);
	}

	/**
	 * Gia hạn Max-Age cookie phiên trên MỌI response của request đã xác thực
	 * thành công (bỏ qua request @NoAuth — không set
	 * {@link #AUTHENTICATED_USER_CODE_PROPERTY} nên tự loại ở đây) — cùng giá
	 * trị token cũ, chỉ làm mới thời hạn để khớp với DB vừa gia hạn ở
	 * {@code IdTokenUtility.verify()}.
	 */
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {

		if (requestContext.getProperty(AUTHENTICATED_USER_CODE_PROPERTY) == null) {
			return;
		}

		Cookie sessionCookie = requestContext.getCookies().get(SessionCookieUtility.SESSION_COOKIE_NAME);
		if (sessionCookie == null || sessionCookie.getValue() == null || sessionCookie.getValue().isEmpty()) {
			return;
		}

		int maxAgeSeconds = IdTokenUtility.getSessionMinutes() * 60;
		responseContext.getHeaders().add("Set-Cookie",
				SessionCookieUtility.buildSessionCookie(sessionCookie.getValue(), maxAgeSeconds));
	}

	private void abortUnauthorized(ContainerRequestContext requestContext, String errId) {
		ErrorDto error = new ErrorDto();
		error.errId = errId;
		error.errMsg = MessageUtility.getSystemErrMsg(errId);
		requestContext.abortWith(
				Response.status(Response.Status.UNAUTHORIZED)
						.type(MediaType.APPLICATION_JSON)
						.entity(error)
						.build());
	}
}
