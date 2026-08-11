package fafoshop.pos.saleorder.dto;

import java.math.BigDecimal;

/** 1 dòng hàng trong đơn bán — khớp bảng sale_order_item. */
public class SaleOrderItemDto {

	public String productCode;

	/** Đơn giá bán tại thời điểm giao dịch (có thể khác giá gốc của sản phẩm nếu thu ngân sửa tay). */
	public BigDecimal unitPrice;

	public Integer quantity;
}
