package fafoshop.pos.saleorder.dto;

import java.math.BigDecimal;

/** 1 dòng hàng trong đơn bán — khớp bảng sale_order_item. */
public class SaleOrderItemDto {

	public String productCode;

	/** Đơn giá bán tại thời điểm giao dịch (có thể khác giá gốc của sản phẩm nếu thu ngân sửa tay). */
	public BigDecimal unitPrice;

	public Integer quantity;

	/**
	 * Thành tiền CHÍNH XÁC của dòng — KHÔNG BẮT BUỘC, chỉ gửi khi dòng hàng
	 * đến từ quy đổi đơn vị đóng gói (Lốc/Thùng, xem
	 * UnitChooserDialogComponent/docs/pos-da-don-vi-tinh.md) mà tổng tiền cấu
	 * hình cho đơn vị đó KHÔNG CHIA HẾT cho số lượng lẻ quy đổi ra (vd 1 Thùng
	 * 500.000đ = 24 cái → giá lẻ làm tròn 20.833đ × 24 = 499.992đ, LỆCH 8đ so
	 * với giá đã cấu hình). Khi có giá trị, server dùng THẲNG làm line_amount
	 * thay vì tính lại `unitPrice × quantity` (tránh làm tròn 2 lần khiến tiền
	 * thu SAI lệch so với giá đã cấu hình) — server vẫn validate lệch so với
	 * `unitPrice × quantity` không vượt quá `quantity` đồng (đủ rộng cho sai
	 * số làm tròn tối đa &lt;1đ/đơn vị, đủ hẹp để chặn gian lận gửi thành tiền
	 * tuỳ ý). null/không gửi → server tự tính `unitPrice × quantity` như cũ
	 * (đa số dòng hàng quét/gõ tay bình thường).
	 */
	public BigDecimal lineAmount;

	/**
	 * Tên đơn vị đóng gói đã chọn lúc bán (vd "Lốc") — KHÔNG BẮT BUỘC, chỉ gửi
	 * khi dòng hàng đến từ dialog "Chọn đơn vị" (xem
	 * UnitChooserDialogComponent/docs/pos-da-don-vi-tinh.md). null/không gửi =
	 * đơn vị lẻ (mặc định). CHỈ lưu lại để hiển thị đúng lựa chọn ban đầu của
	 * người dùng (khắc phục khiếu nại "chọn Vỉ nhưng lưới chỉ hiện số đã quy
	 * đổi ra lẻ") — KHÔNG dùng để tính toán, `quantity` vẫn LUÔN là số lượng
	 * lẻ thật dùng cho tồn kho.
	 */
	public String unitName;

	/**
	 * Số lượng theo ĐÚNG đơn vị đã chọn (vd 3, nếu unitName="Lốc") — chỉ có ý
	 * nghĩa khi `unitName` khác null, xem Javadoc `unitName`.
	 */
	public Integer unitQty;
}
