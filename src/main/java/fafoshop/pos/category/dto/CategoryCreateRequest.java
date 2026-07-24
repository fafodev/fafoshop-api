package fafoshop.pos.category.dto;

import fafoshop.common.dto.request.AbstractRequest;

public class CategoryCreateRequest extends AbstractRequest {

	public String name;
	/** Loại danh mục (category_type) — rỗng thì CategoryCreateProcess mặc định "PRODUCT" */
	public String categoryType;
	public Integer displayOrder;
}
