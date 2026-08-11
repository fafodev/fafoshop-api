package fafoshop.pos.bankaccount.dto;

import fafoshop.common.dto.response.AbstractResponse;

public class BankAccountGetResponse extends AbstractResponse {

	/**
	 * true = chi nhánh đã cấu hình tài khoản NH nhận tiền, các field bên dưới
	 * mới có giá trị. false = CHƯA cấu hình (chưa có màn quản lý, phải chờ
	 * seed/sửa trực tiếp DB) — không phải lỗi, frontend tự ẩn/khoá lựa chọn
	 * thanh toán chuyển khoản khi gặp trường hợp này.
	 */
	public boolean configured;

	public String bankBin;

	public String bankName;

	public String accountNo;

	public String accountName;
}
