package fafoshop.pos.auth.dto;

import fafoshop.common.dto.request.AbstractRequest;

public class AuthLoginRequest extends AbstractRequest {

	/** Mã người dùng nhập ở màn hình đăng nhập */
	public String userCode;

	/** Mật khẩu (plaintext, chỉ tồn tại trong request lúc gọi API, không lưu) */
	public String password;
}
