package fafoshop.pos.category.dto;

import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

/** Danh sách category_type đang có dữ liệu — phục vụ gợi ý autocomplete ở form Category Master. */
public class CategoryTypeListResponse extends AbstractResponse {

	public List<String> types = new ArrayList<>();
}
