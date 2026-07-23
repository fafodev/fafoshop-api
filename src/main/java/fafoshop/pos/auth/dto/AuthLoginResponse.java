package fafoshop.pos.auth.dto;

import fafoshop.common.dto.response.AbstractResponse;

public class AuthLoginResponse extends AbstractResponse {

	/**
	 * Token phiên đăng nhập — CHỈ dùng nội bộ để AuthWebService lấy giá trị
	 * dựng cookie Set-Cookie (HttpOnly, xem SessionCookieUtility).
	 * AuthWebService.login() PHẢI gán field này về null trước khi trả response
	 * cho client — token thật KHÔNG bao giờ xuất hiện trong JSON body, chỉ gửi
	 * qua cookie HttpOnly để tránh bị JavaScript độc hại (nếu trang dính XSS)
	 * đọc trộm, khác với cách cũ trả token qua JSON rồi Angular tự lưu
	 * localStorage.
	 *
	 * Client xác định đăng nhập thành công bằng
	 * {@code getFatalError().isEmpty()}, KHÔNG dựa vào field này (luôn null
	 * trong response thật trả về client).
	 */
	public String token;

	/** Tên hiển thị người dùng */
	public String userName;

	/** Chi nhánh mặc định */
	public String mainBranchCode;
}
