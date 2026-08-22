package fafoshop.pos.product.dto;

import fafoshop.common.dto.response.AbstractResponse;

public class ProductSyncPriceResponse extends AbstractResponse {

	public String productCode;

	/** null/trống = đã ghi vào đơn vị lẻ. */
	public String unitName;
}
