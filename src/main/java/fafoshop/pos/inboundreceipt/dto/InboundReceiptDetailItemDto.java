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
}
