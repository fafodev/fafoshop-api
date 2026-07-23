package fafoshop.pos.product.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.AbstractDto;

public class ProductRowDto extends AbstractDto {

	public String productCode;
	public String name;
	public String barcode;
	public String categoryCode;
	public String unitName;
	public BigDecimal price;
}
