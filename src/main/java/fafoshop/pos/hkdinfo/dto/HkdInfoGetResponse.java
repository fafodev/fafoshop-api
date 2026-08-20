package fafoshop.pos.hkdinfo.dto;

import fafoshop.common.dto.response.AbstractResponse;

public class HkdInfoGetResponse extends AbstractResponse {

	/**
	 * true = chi nhánh đã cấu hình thông tin hộ kinh doanh, các field bên
	 * dưới mới có giá trị. false = CHƯA cấu hình (chưa có màn quản lý, phải
	 * chờ seed/sửa trực tiếp DB) — không phải lỗi, giống hệt cách
	 * BankAccountGetResponse.configured đang xử lý.
	 */
	public boolean configured;

	public String hkdName;

	public String address;

	public String taxCode;

	public String phone;

	public String email;
}
