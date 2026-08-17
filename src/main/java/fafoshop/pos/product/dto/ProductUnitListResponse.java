package fafoshop.pos.product.dto;

import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

public class ProductUnitListResponse extends AbstractResponse {

	public List<ProductUnitDto> rows = new ArrayList<>();
}
