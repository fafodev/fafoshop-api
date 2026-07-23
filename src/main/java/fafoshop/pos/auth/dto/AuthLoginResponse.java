package fafoshop.pos.auth.dto;

import fafoshop.common.dto.response.AbstractResponse;

public class AuthLoginResponse extends AbstractResponse {

	/** Token dùng cho header Authorization: Bearer {token} của các request sau */
	public String token;

	/** Tên hiển thị người dùng */
	public String userName;

	/** Chi nhánh mặc định */
	public String mainBranchCode;
}
