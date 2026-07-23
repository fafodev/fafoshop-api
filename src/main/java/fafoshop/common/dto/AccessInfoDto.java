package fafoshop.common.dto;

/**
 * Thông tin truy cập đính kèm mỗi request. Không đa tenant (1 cửa hàng,
 * không cần mã khách thuê), không đa ngôn ngữ (chỉ tiếng Việt). Tên field
 * dùng camelCase thống nhất với schema (app_user.user_code,
 * branch.branch_code...).
 *
 * userCode được AuthTokenFilter gán từ token đã xác thực (KHÔNG lấy trực
 * tiếp từ dữ liệu client gửi lên, trừ lúc đăng nhập).
 */
public class AccessInfoDto extends AbstractDto {

	/** Mã người dùng (lấy từ token đã xác thực, không phải client tự khai) */
	public String userCode;

	/** Tên người dùng */
	public String userName;

	/** Mã chi nhánh/cửa hàng đang thao tác */
	public String branchCode;

	/** Tên process đang chạy (dùng để checkAuth() và ghi log) */
	public String processId;

	/** Token xác thực (Bearer token) */
	public String token;
}
