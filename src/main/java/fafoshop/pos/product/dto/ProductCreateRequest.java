package fafoshop.pos.product.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.request.AbstractRequest;

public class ProductCreateRequest extends AbstractRequest {

	public String name;
	public String barcode;
	public String categoryCode;
	public String unitName;
	public BigDecimal price;
}
