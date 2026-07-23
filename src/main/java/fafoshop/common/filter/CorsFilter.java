package fafoshop.common.filter;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Cấu hình CORS (Cross-Origin Resource Sharing) cho toàn bộ API — cần thiết
 * vì frontend Angular (`fafoshop`, chạy `ng serve` mặc định cổng 4200) gọi
 * API này từ origin khác (khác cổng tính là khác origin theo chuẩn CORS),
 * nếu thiếu header Access-Control-* trình duyệt sẽ chặn response.
 *
 * `@PreMatching` để chặn request OPTIONS (preflight) TRƯỚC khi Jersey định
 * tuyến: nếu không, OPTIONS tới 1 `@Path` chỉ khai GET/POST sẽ bị trả 404
 * trước khi tới được filter; đồng thời request này chạy trước
 * AuthTokenFilter (chạy sau bước định tuyến) — preflight không gửi cookie
 * phiên nên sẽ luôn bị AuthTokenFilter trả 401 nếu không chặn sớm.
 *
 * CÓ bật Access-Control-Allow-Credentials: true vì xác thực dùng cookie
 * phiên HttpOnly (SessionCookieUtility/AuthTokenFilter) — trình duyệt CHỈ
 * gửi/nhận cookie cho request cross-origin khi cả 2 điều kiện: request có
 * `withCredentials: true` (phía Angular) VÀ response có header này. Khi bật
 * credentials, Access-Control-Allow-Origin BẮT BUỘC phải là 1 origin cụ thể
 * (không được `*`) — ALLOWED_ORIGIN bên dưới đã là origin cụ thể sẵn.
 */
@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

	/**
	 * Origin của frontend được phép gọi API. Hiện chỉ có môi trường dev
	 * (Angular `ng serve` mặc định cổng 4200). Domain production CHƯA xác
	 * định (UNKNOWN) — bổ sung origin thật vào đây khi triển khai.
	 */
	private static final String ALLOWED_ORIGIN = "http://localhost:4200";

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
			requestContext.abortWith(Response.ok().build());
		}
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
		responseContext.getHeaders().putSingle("Access-Control-Allow-Headers",
				"Content-Type, " + HttpHeaders.AUTHORIZATION);
		responseContext.getHeaders().putSingle("Access-Control-Allow-Methods",
				"GET, POST, PUT, DELETE, OPTIONS, HEAD");
		responseContext.getHeaders().putSingle("Access-Control-Max-Age", "3600");
		responseContext.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
	}
}
