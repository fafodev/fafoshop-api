package fafoshop.pos.category.dto;

import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

public class CategoryListResponse extends AbstractResponse {

	public List<CategoryRowDto> rows = new ArrayList<>();
}
