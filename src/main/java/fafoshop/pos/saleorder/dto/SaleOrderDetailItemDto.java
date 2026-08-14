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
}
