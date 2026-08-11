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

	public List<SaleOrderItemDto> items;
}
