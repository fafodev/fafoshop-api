package fafoshop.pos.saleorder.dto;

import java.math.BigDecimal;
import java.util.List;

import fafoshop.common.dto.request.AbstractRequest;

/**
 * Tạo đơn bán tại quầy (checkout POS thật). branchCode/cashierUserCode
 * KHÔNG nhận từ client — branchCode tra theo main_branch_code của
 * accessInfo.userCode, cashierUserCode = chính accessInfo.userCode. Thành
 * tiền từng dòng và tổng tiền hàng do server tính lại từ unitPrice*quantity,
 * không tin số subtotal/changeAmount phía client tự tính.
 */
public class SaleOrderCreateRequest extends AbstractRequest {

	/**
	 * Tên khách hàng ghi tự do lúc bán — KHÔNG phải customer_code thật (chưa có
	 * màn hình quản lý khách hàng, xem retail-domain.md). Có thể để trống.
	 */
	public String customerName;

	public BigDecimal paidAmount;

	/** Phương thức thanh toán: CASH = tiền mặt, TRANSFER = chuyển khoản. */
	public String paymentMethod;

	public List<SaleOrderItemDto> items;

	/**
	 * Khoá chống ghi trùng (idempotency key) do POS sinh ngẫu nhiên (UUID) cho
	 * mỗi lần thanh toán 1 giỏ hàng — gửi lại ĐÚNG khoá này (vd bấm 2 lần lúc
	 * máy lag, hoặc bấm lại sau khi mất kết nối) thì server trả lại đơn đã tạo
	 * lần trước thay vì tạo đơn mới. Không bắt buộc (client cũ không gửi vẫn
	 * chạy như trước, chỉ không được chống trùng). Chỉ nhận chữ/số/gạch nối,
	 * 8–64 ký tự.
	 */
	public String idempotencyKey;
}
