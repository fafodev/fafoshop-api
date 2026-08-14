package fafoshop.pos.saleorder.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.AbstractDto;

public class SaleOrderRowDto extends AbstractDto {

	public String saleOrderNo;
	public String branchCode;
	public String customerName;

	/** Định dạng "yyyy-MM-dd HH:mm:ss" — dùng String để tránh phải thêm module Jackson JSR-310 chỉ cho 1 field. */
	public String saleDatetime;

	public String paymentMethod;
	public BigDecimal paidAmount;
	public BigDecimal changeAmount;

	/** Tổng tiền hàng của đơn (SUM line_amount) — tính qua subquery, KHÔNG phải cột trực tiếp trên sale_order. */
	public BigDecimal totalAmount;

	/** Số dòng hàng trong đơn (COUNT sale_order_item) — không phải tổng số lượng. */
	public int itemCount;

	public String cashierUserCode;
	public String cashierName;

	/** Cờ đơn bị huỷ: "1"=đã huỷ, "0"=còn hiệu lực (sale_order.void_flg). */
	public String voidFlg;
}
