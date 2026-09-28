package fafoshop.pos.saleorder.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.response.AbstractResponse;

public class SaleOrderCreateResponse extends AbstractResponse {

	/** Số đơn bán vừa tạo (sale_order.sale_order_no). */
	public String saleOrderNo;

	/** Tổng tiền hàng — server tính lại từ các dòng hàng, không tin số client gửi. */
	public BigDecimal subtotal;

	/** Tiền thối lại = paidAmount - subtotal — server tính. */
	public BigDecimal changeAmount;

	/**
	 * true = request này trùng idempotencyKey của 1 đơn ĐÃ tạo trước đó —
	 * saleOrderNo/subtotal/changeAmount là của đơn cũ, KHÔNG tạo đơn mới.
	 * false = vừa tạo đơn mới. Luôn có giá trị (không null).
	 */
	public boolean duplicate;
}
