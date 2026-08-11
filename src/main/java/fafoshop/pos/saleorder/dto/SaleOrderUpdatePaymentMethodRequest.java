package fafoshop.pos.saleorder.dto;

import fafoshop.common.dto.request.AbstractRequest;

/**
 * Sửa phương thức thanh toán của đơn bán VỪA TẠO — dùng cho tình huống khách
 * chọn chuyển khoản (đã in bill kèm QR) nhưng đổi ý trả tiền mặt tại quầy
 * (hoặc ngược lại). Xem điều kiện được phép sửa trong
 * SaleOrderUpdatePaymentMethodProcess và docs/pos-in-hoa-don.md (gốc
 * workspace, mục "Phương án A").
 */
public class SaleOrderUpdatePaymentMethodRequest extends AbstractRequest {

	public String saleOrderNo;

	/** Phương thức thanh toán MỚI: CASH = tiền mặt, TRANSFER = chuyển khoản. */
	public String paymentMethod;
}
