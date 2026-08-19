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
	 * Giá bán theo ĐÚNG đơn vị của dòng này — đơn vị LẺ nếu `unitName` null
	 * (Process ghi đè THẲNG vào `product.price` KHÔNG cần xác nhận, hành vi
	 * CŨ giữ nguyên), hoặc giá bán theo đơn vị đóng gói (vd giá bán/1 Vỉ) nếu
	 * `unitName` khác null (Process KHÔNG đụng `product.price` nữa — BUG ĐÃ
	 * SỬA: trước đây field này LUÔN hiểu là per-lẻ dù dòng đang ở đơn vị nào,
	 * khiến chọn "Vỉ" gõ giá bán/Vỉ vào đây ghi đè SAI `product.price` thành
	 * giá của cả Vỉ — xem docs/pos-dong-bo-gia.md mục "Nhập hàng ghi sai
	 * product.price". Đồng bộ vào `product_unit.unit_price` (nếu có xác
	 * nhận) thay vào đó — xem {@link #updateMasterPrice}/{@link #masterUnitPrice}).
	 * LUÔN lưu lại nguyên giá trị này vào `inbound_receipt_item.price` để xem
	 * lại đúng ở Chi tiết phiếu nhập, bất kể có ghi vào Master hay không.
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

	/**
	 * true = người dùng ĐÃ XÁC NHẬN (qua hộp thoại xác nhận phía FE) ghi đè
	 * giá vốn cấu hình trong Product Master bằng đúng {@link #masterUnitCost}
	 * — xem docs/pos-dong-bo-gia.md. null/false = KHÔNG đổi gì trên Master,
	 * `unitCost` trên dòng này CHỈ là bản ghi lịch sử của phiếu.
	 */
	public Boolean updateMasterCost;

	/**
	 * Giá vốn theo ĐÚNG đơn vị của dòng này — đơn vị LẺ nếu `unitName` null
	 * (bằng đúng `unitCost`), hoặc giá vốn theo đơn vị đóng gói (vd giá vốn/1
	 * Vỉ) nếu `unitName` khác null (KHÁC `unitCost` vốn luôn quy đổi về lẻ).
	 * CHỈ dùng khi `updateMasterCost = true`, ghi thẳng vào
	 * `product.cost`/`product_unit.unit_cost` tương ứng — null nếu
	 * `updateMasterCost` không true.
	 */
	public BigDecimal masterUnitCost;

	/**
	 * true = người dùng ĐÃ XÁC NHẬN ghi đè giá bán cấu hình trong
	 * `product_unit.unit_price` bằng đúng {@link #masterUnitPrice} — CHỈ có ý
	 * nghĩa khi `unitName` khác null (dòng đơn vị lẻ luôn ghi thẳng
	 * `product.price`, không cần xác nhận, xem Javadoc {@link #price}). Xem
	 * docs/pos-dong-bo-gia.md.
	 */
	public Boolean updateMasterPrice;

	/**
	 * Giá bán theo đơn vị đóng gói của dòng này (bằng đúng {@link #price} khi
	 * `unitName` khác null) — CHỈ dùng khi `updateMasterPrice = true`, ghi
	 * thẳng vào `product_unit.unit_price` — null nếu `updateMasterPrice`
	 * không true hoặc dòng ở đơn vị lẻ.
	 */
	public BigDecimal masterUnitPrice;
}
