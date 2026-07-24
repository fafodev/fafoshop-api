package fafoshop.pos.category.dto;

import fafoshop.common.dto.request.AbstractRequest;

public class CategoryListRequest extends AbstractRequest {

	/**
	 * Loại danh mục cần lấy — bảng category dùng CHUNG nhiều nghiệp vụ nên
	 * caller bắt buộc phải khai rõ (vd "PRODUCT" cho danh mục sản phẩm).
	 */
	public String categoryType;
}
