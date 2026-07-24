package fafoshop.pos.category.dto;

import fafoshop.common.dto.response.AbstractResponse;

public class CategoryCreateResponse extends AbstractResponse {

	/** Mã danh mục vừa được tạo (echo lại categoryCode nhập tay ở request) */
	public String categoryCode;
}
