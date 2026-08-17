package fafoshop.pos.inboundreceipt.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.AbstractDto;

public class InboundReceiptDetailItemDto extends AbstractDto {

	public int lineNo;
	public String productCode;
	public String productName;
	public String barcode;
	public int quantity;
	public BigDecimal unitCost;
	public BigDecimal lineAmount;
	/** "yyyy-MM-dd" — có thể trống nếu hàng không có hạn dùng. */
	public String expiryDate;

	/**
	 * Giá bán ĐÃ ÁP DỤNG lúc lập phiếu này — NULL nếu phiếu tạo TRƯỚC khi có
	 * cột này (không backfill), xem Javadoc InboundReceiptItemDto.price.
	 * NULLABLE (kế thừa AbstractDto, @JsonInclude(NON_NULL)) — frontend PHẢI
	 * check cả `null` lẫn `undefined`, không chỉ `=== null`.
	 */
	public BigDecimal price;

	/** Tên đơn vị đóng gói đã chọn lúc nhập (vd "Lốc") — NULL = đơn vị lẻ. NULLABLE, xem Javadoc `price`. */
	public String unitName;

	/** Số lượng theo ĐÚNG đơn vị đã chọn — NULL khi `unitName` NULL. NULLABLE, xem Javadoc `price`. */
	public Integer unitQty;
}
