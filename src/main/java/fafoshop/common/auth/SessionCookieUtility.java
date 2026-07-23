package fafoshop.common.auth;

/**
 * Dựng chuỗi header {@code Set-Cookie} cho cookie phiên đăng nhập — gom 1
 * chỗ DUY NHẤT để AuthTokenFilter (đọc), AuthWebService (set lúc login) và
 * AuthLogoutWebService (xoá lúc logout) dùng chung, KHÔNG lặp lại chuỗi
 * thuộc tính cookie (HttpOnly/Secure/SameSite) ở nhiều nơi.
 *
 * Cookie đặt {@code HttpOnly} để JavaScript phía client (kể cả khi trang
 * dính lỗi XSS) không đọc được giá trị token — khác với cách cũ trả token
 * trong JSON body rồi Angular tự lưu vào localStorage (localStorage luôn đọc
 * được bằng JS, là lỗ hổng nếu có XSS). {@code SameSite=Strict} để trình
 * duyệt không gửi kèm cookie này cho request cross-site (chống CSRF) — toàn
 * bộ API chỉ được gọi qua fetch/XHR của chính SPA Angular, không có luồng
 * nào cần cookie gửi cross-site. {@code Secure} yêu cầu HTTPS (Chrome/
 * Firefox coi {@code localhost} là secure context nên vẫn hoạt động ở dev
 * qua http://localhost).
 *
 * LƯU Ý: frontend và backend PHẢI cùng site (cùng eTLD+1, vd
 * app.fafoshop.com/api.fafoshop.com) để cookie SameSite=Strict còn hoạt
 * động khi triển khai thật — domain production cụ thể vẫn là UNKNOWN.
 */
public final class SessionCookieUtility {

	public static final String SESSION_COOKIE_NAME = "fafoshop_session";

	private SessionCookieUtility() {
	}

	/** Dựng header Set-Cookie mang token thật, hết hạn sau {@code maxAgeSeconds}. */
	public static String buildSessionCookie(String tokenValue, int maxAgeSeconds) {
		return SESSION_COOKIE_NAME + "=" + tokenValue
				+ "; Path=/; Max-Age=" + maxAgeSeconds
				+ "; HttpOnly; Secure; SameSite=Strict";
	}

	/** Dựng header Set-Cookie xoá cookie phiên ngay lập tức (dùng khi logout). */
	public static String buildExpiredSessionCookie() {
		return SESSION_COOKIE_NAME + "=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict";
	}
}
