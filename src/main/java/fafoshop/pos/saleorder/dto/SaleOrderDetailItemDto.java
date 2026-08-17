package fafoshop.pos.saleorder.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.AbstractDto;

public class SaleOrderDetailItemDto extends AbstractDto {

	public int lineNo;
	public String productCode;
	public String productName;
	public String barcode;
	public BigDecimal unitPrice;
	public int quantity;
	public BigDecimal lineAmount;

	/** Giá vốn bình quân gia quyền TẠI THỜI ĐIỂM bán — NULL nếu sản phẩm chưa từng có phiếu nhập tính đến lúc bán (không phải giá vốn = 0). */
	public BigDecimal unitCost;

	/** Lãi của dòng = lineAmount - unitCost*quantity — NULL nếu unitCost NULL. */
	public BigDecimal lineProfit;

	/**
	 * Tên đơn vị đóng gói đã chọn lúc bán (vd "Lốc") — NULL = đơn vị lẻ, xem
	 * Javadoc `SaleOrderItemDto.unitName`. NULLABLE (kế thừa `AbstractDto`,
	 * `@JsonInclude(NON_NULL)`) — frontend PHẢI check cả `null` lẫn
	 * `undefined`, không chỉ `=== null` (xem coding-rules.md mục "Field
	 * nullable từ API backend").
	 */
	public String unitName;

	/** Số lượng theo ĐÚNG đơn vị đã chọn — NULL khi `unitName` NULL. NULLABLE, xem Javadoc `unitName`. */
	public Integer unitQty;
}
