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

	/** Số lượng thực nhận — LUÔN theo đơn vị LẺ (đã quy đổi sẵn từ đơn vị đóng gói nếu có, xem `unitName`/`unitQty`). */
	public Integer quantity;

	/** Đơn giá vốn nhập hàng (giá gốc) — LUÔN theo đơn vị LẺ, không đổi ý nghĩa theo `unitName` (xem docs/pos-da-don-vi-tinh.md). */
	public BigDecimal unitCost;

	/**
	 * Giá bán hiện hành của sản phẩm - hiển thị/sửa ngay trên lưới màn Nhập
	 * hàng (thay vì phải qua màn Sản phẩm sửa riêng). Process ghi đè thẳng vào
	 * product.price bất kể có thay đổi hay không (ghi lại giá trị đang hiển thị
	 * trên lưới là an toàn - không đổi thì ghi lại đúng giá cũ), ĐỒNG THỜI lưu
	 * lại nguyên giá trị này vào inbound_receipt_item.price để xem lại đúng ở
	 * Chi tiết phiếu nhập (trước đây chỉ là side-effect thoáng qua, không lưu
	 * lại — khắc phục khiếu nại "phiếu nhập không hiện giá bán").
	 */
	public BigDecimal price;

	/** Hạn sử dụng, định dạng "yyyy-MM-dd" - có thể để trống nếu hàng không có hạn dùng. */
	public String expiryDate;

	/**
	 * Tên đơn vị đóng gói đã chọn lúc nhập (vd "Lốc") — KHÔNG BẮT BUỘC, chỉ gửi
	 * khi dòng hàng đến từ dialog "Chọn đơn vị" (xem
	 * UnitChooserDialogComponent/docs/pos-da-don-vi-tinh.md). null/không gửi =
	 * đơn vị lẻ (mặc định). CHỈ lưu lại để hiển thị đúng lựa chọn ban đầu của
	 * người dùng — KHÔNG dùng để tính toán, `quantity` vẫn LUÔN là số lượng lẻ
	 * thật dùng cho tồn kho/giá vốn.
	 *
	 * Khi khác null, backend BẮT BUỘC `price` phải &gt; 0 (không chỉ &gt;= 0
	 * như dòng đơn vị lẻ) — dòng thêm qua đơn vị đóng gói KHÔNG được phép tự
	 * điền ngầm giá bán cũ, người dùng phải tự xác nhận (xem
	 * InboundReceiptCreateProcess.validateItems, khắc phục khiếu nại giá bán
	 * hiển thị sai khi chọn Vỉ).
	 */
	public String unitName;

	/** Số lượng theo ĐÚNG đơn vị đã chọn (vd 3, nếu unitName="Lốc") — chỉ có ý nghĩa khi `unitName` khác null. */
	public Integer unitQty;

	/**
	 * Thành tiền CHÍNH XÁC của dòng — KHÔNG BẮT BUỘC, mirror
	 * SaleOrderItemDto.lineAmount (xem Javadoc ở đó cho lý do đầy đủ: làm tròn
	 * 2 lần khi đơn vị đóng gói không chia hết cho số lượng lẻ quy đổi ra).
	 * Khi có giá trị, server dùng THẲNG làm line_amount thay vì tính lại
	 * `unitCost × quantity` — vẫn validate lệch không vượt quá `quantity`
	 * đồng. null/không gửi → server tự tính `unitCost × quantity` như cũ.
	 */
	public BigDecimal lineAmount;
}
