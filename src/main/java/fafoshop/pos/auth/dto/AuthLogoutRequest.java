package fafoshop.pos.auth.dto;

import fafoshop.common.dto.request.AbstractRequest;

public class AuthLogoutRequest extends AbstractRequest {

	/**
	 * Token phiên (giá trị cookie HttpOnly) — AuthLogoutWebService gán từ
	 * cookie request TRƯỚC khi gọi process, KHÔNG lấy từ JSON body client gửi
	 * lên (client không cầm được giá trị cookie HttpOnly để tự gửi lại).
	 */
	public String token;
}
