package fafoshop.pos.product.dto;

import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

public class ProductSearchResponse extends AbstractResponse {

	public List<ProductRowDto> rows = new ArrayList<>();
}
