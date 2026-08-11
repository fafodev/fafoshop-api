package fafoshop.pos.inboundreceipt.dto;

import java.math.BigDecimal;

/**
 * 1 dòng hàng trong phiếu nhập — khớp bảng inbound_receipt_item. Chưa hỗ trợ
 * chọn quality_code (luôn '01' - hàng thường) hay ghi chú riêng từng dòng,
 * đúng phạm vi màn Nhập hàng lần này (chỉ ghi nhận thực nhận, không có luồng
 * lập kế hoạch nhập hàng - xem retail-domain.md).
 */
public class InboundReceiptItemDto {

	public String productCode;

	/** Số lượng thực nhận. */
	public Integer quantity;

	/** Đơn giá vốn nhập hàng (giá gốc). */
	public BigDecimal unitCost;

	/**
	 * Giá bán hiện hành của sản phẩm - hiển thị/sửa ngay trên lưới màn Nhập
	 * hàng (thay vì phải qua màn Sản phẩm sửa riêng). Process ghi đè thẳng vào
	 * product.price bất kể có thay đổi hay không (ghi lại giá trị đang hiển thị
	 * trên lưới là an toàn - không đổi thì ghi lại đúng giá cũ).
	 */
	public BigDecimal price;

	/** Hạn sử dụng, định dạng "yyyy-MM-dd" - có thể để trống nếu hàng không có hạn dùng. */
	public String expiryDate;
}
