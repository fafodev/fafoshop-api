package fafoshop.pos.product.dto;

import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

public class ProductSupplierListResponse extends AbstractResponse {

	public List<ProductSupplierRowDto> rows = new ArrayList<>();
}
