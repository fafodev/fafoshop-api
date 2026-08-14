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
}
